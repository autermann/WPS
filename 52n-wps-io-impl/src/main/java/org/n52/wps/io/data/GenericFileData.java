/**
 * Copyright (C) 2007 - 2014 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *       * Apache License, version 2.0
 *       * Apache Software License, version 1.0
 *       * GNU Lesser General Public License, version 3
 *       * Mozilla Public License, versions 1.0, 1.1 and 2.0
 *       * Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public
 * License version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 */
package org.n52.wps.io.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.DataStore;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.GeometryAttributeImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.type.GeometryDescriptorImpl;
import org.geotools.feature.type.GeometryTypeImpl;
import org.geotools.filter.identity.GmlObjectIdImpl;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.n52.wps.commons.Format;
import org.n52.wps.io.IOHandler;
import org.n52.wps.io.data.binding.complex.GTRasterDataBinding;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.io.datahandler.generator.GeotiffGenerator;
import org.n52.wps.io.datahandler.parser.GML2BasicParser;
import org.n52.wps.io.datahandler.parser.GML3BasicParser;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.IllegalAttributeException;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.PropertyType;
import org.opengis.filter.identity.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;

/**
 * 
 * @author Matthias Mueller, TU Dresden; Bastian Schaeffer, IFGI
 *
 */
public class GenericFileData {

	private static Logger LOGGER = LoggerFactory.getLogger(GenericFileData.class);

	protected final InputStream dataStream;
	protected String fileExtension;
	protected final String mimeType;
	protected File primaryFile;

	public GenericFileData(InputStream stream, String mimeType) {
		this.dataStream = stream;
		this.mimeType = mimeType;
		this.fileExtension = GenericFileDataConstants.mimeTypeFileTypeLUT()
				.get(mimeType);
		if(fileExtension == null){
			this.fileExtension = "dat";
		}
	}

	public GenericFileData(FeatureCollection<?, ?> featureCollection)
			throws IOException {
		this(getShpFile(featureCollection), IOHandler.MIME_TYPE_ZIPPED_SHP);
	}

	public GenericFileData(File primaryTempFile, String mimeType)
			throws IOException {
		this.primaryFile = primaryTempFile;
		this.mimeType = mimeType;
		this.fileExtension = GenericFileDataConstants.mimeTypeFileTypeLUT()
				.get(mimeType);

		InputStream is = null;

		if (GenericFileDataConstants.getIncludeFilesByMimeType(mimeType) != null) {

			String baseFile = primaryFile.getName();
			baseFile = baseFile.substring(0, baseFile.lastIndexOf('.'));
			File temp = new File(primaryFile.getAbsolutePath());
			File directory = new File(temp.getParent());
			String[] extensions = GenericFileDataConstants
					.getIncludeFilesByMimeType(mimeType);

			File[] allFiles = new File[extensions.length + 1];

			for (int i = 0; i < extensions.length; i++) {
                allFiles[i] = new File(directory, baseFile + "."
                                                  + extensions[i]);
            }

			allFiles[extensions.length] = primaryFile;
			
			// Handling the case if the files don't exist
			// (Can occur if ArcGIS backend has an error and returns no files,
			// but only filenames).
			int numberOfFiles = allFiles.length;
			int numberOfMissing = 0;
			for (int i = 0; i < numberOfFiles; i++){
				if (!allFiles[i].exists()){
					LOGGER.info("File " + (i+1) + " of " + numberOfFiles + " missing (" + allFiles[i].getName() + ").");
					numberOfMissing ++;
				}
			}
			if ((numberOfFiles - numberOfMissing) == 0){
				String message = "There is no files to generate data from!";
				LOGGER.error(message);
				throw new FileNotFoundException(message);
			} else if ((numberOfMissing > 0)){
				LOGGER.info("Not all files are available, but the available ones are zipped.");
			}

			is = new FileInputStream(org.n52.wps.io.IOUtils.zip(allFiles));
		} else {
			is = new FileInputStream(primaryFile);
		}

		this.dataStream = is;

	}
	
	
	public GenericFileData(GridCoverage2D payload, String mimeType) {
		this.dataStream = null;
		this.fileExtension = "tiff";
		this.mimeType = mimeType;

        try {
            GeotiffGenerator generator = new GeotiffGenerator();
            this.primaryFile = File.createTempFile("primary", ".tif");//changed to .tif
            try (InputStream is = generator.generateStream(new GTRasterDataBinding(payload), new Format(mimeType));
                    OutputStream os = new FileOutputStream(primaryFile)) {
                IOUtils.copy(is, os);
            }
        } catch (IOException e){
			LOGGER.error("Could not generate GeoTiff.", e);
		}
	}

	public static File getShpFile(FeatureCollection<?, ?> collection)
			throws IOException, IllegalAttributeException {
		SimpleFeatureType type = null;
		SimpleFeatureBuilder build = null;
		FeatureIterator<?> iterator = collection.features();
		FeatureCollection<SimpleFeatureType, SimpleFeature> modifiedFeatureCollection = null;
		Transaction transaction = new DefaultTransaction("create");
		FeatureStore<SimpleFeatureType, SimpleFeature> store = null;
		String uuid = UUID.randomUUID().toString();
		File shp = File.createTempFile("Shape_" + uuid, ".shp");
		shp.deleteOnExit();
		while (iterator.hasNext()) {
			SimpleFeature sf = (SimpleFeature) iterator.next();
			// create SimpleFeatureType
			if (type == null) {
				SimpleFeatureType inType = (SimpleFeatureType) collection
						.getSchema();
				SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
				builder.setName(inType.getName());
				builder.setNamespaceURI(inType.getName().getNamespaceURI());

				if (collection.getSchema().getCoordinateReferenceSystem() == null) {
					builder.setCRS(DefaultGeographicCRS.WGS84);
				} else {
					builder.setCRS(collection.getSchema()
							.getCoordinateReferenceSystem());
				}

				builder.setDefaultGeometry(sf.getDefaultGeometryProperty()
						.getName().getLocalPart());

				/*
				 * seems like the geometries must always be the first property..
				 * @see also ShapeFileDataStore.java getSchema() method 
				 */
				Property geomProperty = sf.getDefaultGeometryProperty();				

				if(geomProperty.getType().getBinding().getSimpleName().equals("Geometry")){
				Geometry g = (Geometry)geomProperty.getValue();
				if(g!=null){
					GeometryAttribute geo = null;
					if(g instanceof MultiPolygon){
					
					GeometryAttribute oldGeometryDescriptor = sf.getDefaultGeometryProperty();
					GeometryType type1 = new GeometryTypeImpl(geomProperty.getName(),MultiPolygon.class, oldGeometryDescriptor.getType().getCoordinateReferenceSystem(),oldGeometryDescriptor.getType().isIdentified(),oldGeometryDescriptor.getType().isAbstract(),oldGeometryDescriptor.getType().getRestrictions(),oldGeometryDescriptor.getType().getSuper(),oldGeometryDescriptor.getType().getDescription());
														
					GeometryDescriptor newGeometryDescriptor = new GeometryDescriptorImpl(type1,geomProperty.getName(),0,1,true,null);
					Identifier identifier = new GmlObjectIdImpl(sf.getID());
					geo = new GeometryAttributeImpl((Object)g,newGeometryDescriptor, identifier);
					sf.setDefaultGeometryProperty(geo);
					sf.setDefaultGeometry(g);
					}else{
						//TODO: implement other cases
					}
					if(geo != null){
					builder.add(geo.getName().getLocalPart(), geo
							.getType().getBinding());
					}
				}
				}else if (isSupportedShapefileType(geomProperty.getType())
						&& (geomProperty.getValue() != null)) {
					builder.add(geomProperty.getName().getLocalPart(), geomProperty
							.getType().getBinding());											
				}
				
				for (Property prop : sf.getProperties()) {
					
					if (prop.getType() instanceof GeometryType) {
						/*
						 * skip, was handled before
						 */
					}else if (isSupportedShapefileType(prop.getType())
							&& (prop.getValue() != null)) {
						builder.add(prop.getName().getLocalPart(), prop
								.getType().getBinding());
					}
				}

				type = builder.buildFeatureType();

				ShapefileDataStore dataStore = new ShapefileDataStore(shp
						.toURI().toURL());
				dataStore.createSchema(type);
				dataStore.forceSchemaCRS(type.getCoordinateReferenceSystem());

				String typeName = dataStore.getTypeNames()[0];
				store = (FeatureStore<SimpleFeatureType, SimpleFeature>) dataStore
						.getFeatureSource(typeName);

				store.setTransaction(transaction);
				
				build = new SimpleFeatureBuilder(type);
				modifiedFeatureCollection = new DefaultFeatureCollection("fc",
						type);
			}
			for (AttributeType attributeType : type.getTypes()) {
				build.add(sf.getProperty(attributeType.getName()).getValue());
			}

			SimpleFeature newSf = build.buildFeature(sf.getIdentifier().getID());
			
			modifiedFeatureCollection.add(newSf);
		}

		try {
			store.addFeatures(modifiedFeatureCollection);
			transaction.commit();
			return shp;
		} catch (Exception e1) {
			transaction.rollback();
			throw new IOException(e1.getMessage(),e1);
		} finally {
			transaction.close();
		}
	}

	private static boolean isSupportedShapefileType(PropertyType type) {
		String supported[] = { "String", "Integer", "Double", "Boolean",
				"Date", "LineString", "MultiLineString", "Polygon",
				"MultiPolygon", "Point", "MultiPoint", "Long"};
		for (String iter : supported) {
			if (type.getBinding().getSimpleName().equalsIgnoreCase(iter)) {
				return true;
			}
		}
		return false;
	}

	public String writeData(File workspaceDir) {

		String fileName = null;
		if (GenericFileDataConstants.getIncludeFilesByMimeType(mimeType) != null) {
			try {
				fileName = unzipData(dataStream, fileExtension, workspaceDir);
			} catch (IOException e) {
				LOGGER.error("Could not unzip the archive to " + workspaceDir, e);
			}
		} else {
			try {
				fileName = justWriteData(dataStream, fileExtension, workspaceDir);
			} catch (IOException e) {
				LOGGER.error("Could not write the input to " + workspaceDir, e);
			}
		}

		return fileName;
	}

	private String unzipData(InputStream is, String extension,
			File writeDirectory) throws IOException {
		String baseFileName = UUID.randomUUID().toString();
        String returnFile;
        try (ZipInputStream zipInputStream = new ZipInputStream(is)) {
            ZipEntry entry;
            returnFile = null;
            while ((entry = zipInputStream.getNextEntry()) != null) {

                String currentExtension = entry.getName();
                int beginIndex = currentExtension.lastIndexOf('.') + 1;
                currentExtension = currentExtension.substring(beginIndex);

                String fileName = baseFileName + "." + currentExtension;
                File currentFile = new File(writeDirectory, fileName);
                if (!writeDirectory.exists()){
                    writeDirectory.mkdir();
                }
                currentFile.createNewFile();
                try (FileOutputStream fos = new FileOutputStream(currentFile)) {
                    IOUtils.copy(zipInputStream, fos);

                    if (currentExtension.equalsIgnoreCase(extension)) {
                        returnFile = currentFile.getAbsolutePath();
                    }
                }
            }
        }
		return returnFile;
	}

	private String justWriteData(InputStream is, String extension, File writeDirectory) throws IOException {
		
		String fileName = null;
		String baseFileName = UUID.randomUUID().toString();

		fileName = baseFileName + "." + extension;
		File currentFile = new File(writeDirectory, fileName);
		if (!writeDirectory.exists()){
			writeDirectory.mkdir();
		}
		currentFile.createNewFile();

		// alter FileName for return
		fileName = currentFile.getAbsolutePath();
        try (FileOutputStream fos = new FileOutputStream(currentFile)) {
            IOUtils.copy(is, fos);
        }
		is.close();
		System.gc();

		return fileName;
	}

	public GTVectorDataBinding getAsGTVectorDataBinding() throws IOException {
		
		if(mimeType.equals(GenericFileDataConstants.MIME_TYPE_ZIPPED_SHP)){
			String tmpDirPath = System.getProperty("java.io.tmpdir");
			String dirName = tmpDirPath + File.separator + "tmp" + UUID.randomUUID();
			File tempDir = null;
	
			if (new File(dirName).mkdir()) {
				tempDir = new File(dirName);
			}
	
			LOGGER.info("Writing temp data to: " + tempDir);
			String fileName = writeData(tempDir);
			LOGGER.info("Temp file is: " + fileName);
			File shpFile = new File(fileName);
	
			try {
				DataStore store = new ShapefileDataStore(shpFile.toURI().toURL());
				FeatureCollection<?, ?> features = store.getFeatureSource(
						store.getTypeNames()[0]).getFeatures();
				System.gc();
				tempDir.delete();
				return new GTVectorDataBinding(features);
			} catch (MalformedURLException e) {
				LOGGER.error("Something went wrong while creating data store.");
				throw new RuntimeException("Something went wrong while creating data store.", e);
			} catch (IOException e) {
				LOGGER.error("Something went wrong while converting shapefile to FeatureCollection");
				throw new IOException("Something went wrong while converting shapefile to FeatureCollection", e);
            }
        }
        if (mimeType.equals(GenericFileDataConstants.MIME_TYPE_GML200) ||
            mimeType.equals(GenericFileDataConstants.MIME_TYPE_GML211) ||
            mimeType.equals(GenericFileDataConstants.MIME_TYPE_GML212) ||
            mimeType.equals(GenericFileDataConstants.MIME_TYPE_GML2121)) {
            GML2BasicParser parser = new GML2BasicParser();
            return parser.parse(getDataStream(), new Format(mimeType));
        }
        if (mimeType.equals(GenericFileDataConstants.MIME_TYPE_GML300) ||
            mimeType.equals(GenericFileDataConstants.MIME_TYPE_GML301) ||
            mimeType.equals(GenericFileDataConstants.MIME_TYPE_GML310) ||
            mimeType.equals(GenericFileDataConstants.MIME_TYPE_GML311) ||
            mimeType.equals(GenericFileDataConstants.MIME_TYPE_GML321)) {
            GML3BasicParser parser = new GML3BasicParser();
            return parser.parse(getDataStream(), new Format(mimeType));
        }
		throw new RuntimeException("Could not create GTVectorDataBinding for Input");
		
	}
	/*
	 * Returns the Shp file representation of the file if possible. The returning file is the shp file. All other files associated to that shp file have the same name and are in the same folder.
	 */
	public File getShpFile() throws IOException {
		return getAsGTVectorDataBinding().getPayloadAsShpFile();
	}

	public File getBaseFile(boolean unzipIfPossible) {
		String extension = fileExtension;	
		if(primaryFile==null && dataStream!=null){
			try{
			
			if(fileExtension.equals("shp")){
				extension = "zip";
			}
			primaryFile = File.createTempFile(UUID.randomUUID().toString(), "."+extension);
			OutputStream out = new FileOutputStream(primaryFile);
			byte buf[]=new byte[1024];
			int len;
			while((len=dataStream.read(buf))>0){
			  out.write(buf,0,len);
			}
			out.close();
			}catch(Exception e){
				LOGGER.error(e.getMessage(), e);
				throw new RuntimeException(
						"Something went wrong while writing the input stream to the file system",
						e);
			}
			
        }
        if (unzipIfPossible && extension.contains("zip")) {
            try {
                File tempFile1 = File.createTempFile(UUID.randomUUID()
                        .toString(), "");
                File dir = new File(tempFile1.getParentFile() + "/" + UUID
                        .randomUUID().toString());
                dir.mkdir();
                FileInputStream fis = new FileInputStream(primaryFile);
                try (ZipInputStream zis = new ZipInputStream(fis)) {
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        LOGGER.debug("Extracting: " + entry);
                        // write the files to the disk
                        FileOutputStream fos = new FileOutputStream(dir
                                .getAbsoluteFile() + "/" + entry.getName());

                        IOUtils.copy(zis, fos);

                    }
                }

                File[] files = dir.listFiles();
	         for(File file : files){
	        	 if(file.getName().contains(".shp") || file.getName().contains(".SHP")){
	        		 primaryFile = file;
	        	 }
	         }
			}catch(IOException e){
				LOGGER.error(e.getMessage(), e);
				throw new RuntimeException("Error while unzipping input data", e);
			}
		}
		return primaryFile;
	}

	protected void finalize(){
		try{
			primaryFile.delete();
		}catch(Exception e){
			LOGGER.error(e.getMessage(), e);
		}
	}
	
	public String getMimeType(){
		return mimeType;
	}
	
	public String getFileExtension(){
		return fileExtension;
	}

	public InputStream getDataStream() {
		return dataStream;
	}
}
