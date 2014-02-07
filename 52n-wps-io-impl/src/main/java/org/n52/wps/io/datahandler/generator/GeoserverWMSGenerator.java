/**
 * ﻿Copyright (C) 2007 - 2014 52°North Initiative for Geospatial Open Source
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
 *       • Apache License, version 2.0
 *       • Apache Software License, version 1.0
 *       • GNU Lesser General Public License, version 3
 *       • Mozilla Public License, versions 1.0, 1.1 and 2.0
 *       • Common Development and Distribution License (CDDL), version 1.0
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
package org.n52.wps.io.datahandler.generator;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.httpclient.HttpException;
import org.n52.wps.PropertyDocument.Property;
import org.n52.wps.commons.Format;
import org.n52.wps.commons.WPSConfig;
import org.n52.wps.commons.XMLUtil;
import org.n52.wps.io.IOUtils;
import org.n52.wps.io.data.GenericFileData;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GTRasterDataBinding;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.io.data.binding.complex.GeotiffBinding;
import org.n52.wps.io.data.binding.complex.ShapefileBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class GeoserverWMSGenerator extends AbstractGenerator {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(GeoserverWMSGenerator.class);
    private static final String GEOSERVER_PORT_PROPERTY = "Geoserver_port";
    private static final String GEOSERVER_HOST_PROPERTY = "Geoserver_host";
    private static final String GEOSERVER_PASSWORD_PROPERTY = "Geoserver_password";
    private static final String GEOSERVER_USERNAME_PROPERTY = "Geoserver_username";
	private String username;
	private String password;
	private String host;
	private String port;
	
	public GeoserverWMSGenerator() {
		
        super(GTRasterDataBinding.class,
              ShapefileBinding.class,
              GeotiffBinding.class,
              GTVectorDataBinding.class);

        Property[] properties = WPSConfig.getInstance()
                .getPropertiesForGeneratorClass(this.getClass().getName());
        for (Property property : properties) {
            if (property.getName().equalsIgnoreCase(GEOSERVER_USERNAME_PROPERTY)) {
                username = property.getStringValue();
            }
            if (property.getName().equalsIgnoreCase(GEOSERVER_PASSWORD_PROPERTY)) {
                password = property.getStringValue();
            }
            if (property.getName().equalsIgnoreCase(GEOSERVER_HOST_PROPERTY)) {
                host = property.getStringValue();
            }
            if (property.getName().equalsIgnoreCase(GEOSERVER_PORT_PROPERTY)) {
                port = property.getStringValue();
            }
        }
        if (port == null) {
            port = WPSConfig.getInstance().getWPSConfig().getServer()
                    .getHostport();
        }
    }

    @Override
    public InputStream generateStream(IData data, Format format) throws
            IOException {
        InputStream stream = null;
        try {
            Document doc = storeLayer(data);
            String xmlString = XMLUtil.nodeToString(doc);
            stream = new ByteArrayInputStream(xmlString.getBytes(format
                    .getEncoding().or(DEFAULT_ENCODING)));
        } catch (TransformerException | IOException |
                ParserConfigurationException e) {
            LOGGER.error("Error generating WMS output. Reason: ", e);
            throw new RuntimeException("Error generating WMS output. Reason: " +
                                       e);
        }
        return stream;
    }

    private Document storeLayer(IData coll) throws HttpException, IOException,
                                                   ParserConfigurationException {
        File file = null;
        String storeName;
        if (coll instanceof GTVectorDataBinding) {
            GTVectorDataBinding gtData = (GTVectorDataBinding) coll;

            try {
                GenericFileData fileData = new GenericFileData(gtData.getPayload());
                file = fileData.getBaseFile(true);
            } catch (IOException e1) {
                throw new RuntimeException("Error generating shp file for storage in WFS.", e1);
            }

            //zip shp file
            String path = file.getAbsolutePath();
            String baseName = path.substring(0, path.length() - ".shp".length());
            File zipped = IOUtils.zip(file,
                                      new File(baseName + ".shx"),
                                      new File(baseName + ".dbf"),
                                      new File(baseName + ".prj"));

            file = zipped;

        } else if (coll instanceof GTRasterDataBinding) {
            GTRasterDataBinding gtData = (GTRasterDataBinding) coll;
            GenericFileData fileData = new GenericFileData(gtData.getPayload(), null);
            file = fileData.getBaseFile(true);
        } else if (coll instanceof ShapefileBinding) {
            file = ((ShapefileBinding) coll).getZippedPayload();
        } else if (coll instanceof GeotiffBinding) {
            file = ((GeotiffBinding) coll).getPayload();
        } else {
            throw new RuntimeException("Unsupported BindingClass: " + coll);
        }
        storeName = file.getName();

		storeName = storeName +"_" + UUID.randomUUID();
		GeoServerUploader geoserverUploader = new GeoServerUploader(username, password, host, port);
		String result = geoserverUploader.createWorkspace();
		LOGGER.debug(result);
		if(coll instanceof GTVectorDataBinding){
			result = geoserverUploader.uploadShp(file, storeName);			
		}
		if(coll instanceof GTRasterDataBinding){
			result = geoserverUploader.uploadGeotiff(file, storeName);
		}
		
		LOGGER.debug(result);
				
		String capabilitiesLink = "http://"+host+":"+port+"/geoserver/wms?Service=WMS&Request=GetCapabilities&Version=1.1.1";
		//String directLink = geoserverBaseURL + "?Service=WMS&Request=GetMap&Version=1.1.0&Layers=N52:"+wmsLayerName+"&WIDTH=300&HEIGHT=300";;
		
		Document doc = createXML("N52:"+storeName, capabilitiesLink);
		return doc;
	
	}
	
	private Document createXML(String layerName, String getCapabilitiesLink) throws ParserConfigurationException{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		Document doc = factory.newDocumentBuilder().newDocument();
		
		Element root = doc.createElement("OWSResponse");
		root.setAttribute("type", "WMS");
		
		Element resourceIDElement = doc.createElement("ResourceID");
		resourceIDElement.appendChild(doc.createTextNode(layerName));
		root.appendChild(resourceIDElement);
		
		Element getCapabilitiesLinkElement = doc.createElement("GetCapabilitiesLink");
		getCapabilitiesLinkElement.appendChild(doc.createTextNode(getCapabilitiesLink));
		root.appendChild(getCapabilitiesLinkElement);
		/*
		Element directResourceLinkElement = doc.createElement("DirectResourceLink");
		directResourceLinkElement.appendChild(doc.createTextNode(getMapRequest));
		root.appendChild(directResourceLinkElement);
		*/
		doc.appendChild(root);
		
		return doc;
	}
	
}
