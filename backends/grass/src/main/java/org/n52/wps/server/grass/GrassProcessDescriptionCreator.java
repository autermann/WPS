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
package org.n52.wps.server.grass;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.opengis.wps.x100.ComplexDataDescriptionType;
import net.opengis.wps.x100.InputDescriptionType;
import net.opengis.wps.x100.OutputDescriptionType;
import net.opengis.wps.x100.ProcessDescriptionType;
import net.opengis.wps.x100.ProcessDescriptionsDocument;
import net.opengis.wps.x100.SupportedComplexDataInputType;
import net.opengis.wps.x100.SupportedComplexDataType;

import org.apache.xmlbeans.XmlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.n52.wps.commons.Format;
import org.n52.wps.commons.XmlConstants;
import org.n52.wps.io.IOHandler;
import org.n52.wps.io.data.GenericFileDataConstants;
import org.n52.wps.io.datahandler.parser.GenericFileParser;
import org.n52.wps.server.grass.io.GrassIOHandler;
import org.n52.wps.server.grass.util.JavaProcessStreamReader;

/**
 * @author Benjamin Pross (bpross-52n)
 *
 */
public class GrassProcessDescriptionCreator {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(GrassProcessDescriptionCreator.class);

	private final String fileSeparator = System.getProperty("file.separator");
	private final String lineSeparator = System.getProperty("line.separator");
	private String grassHome = "";
	private String pythonHome = "";	
	private String pythonPath = "";	
	private String addonPath = "";	

	private String[] envp = null;
	private final String wpsProcessDescCmd = " --wps-process-description";
	private final Runtime rt = Runtime.getRuntime();
	private final ExecutorService executor = Executors.newFixedThreadPool(10);
	private final String gisrcDir;

	public GrassProcessDescriptionCreator() {

		grassHome = GrassProcessRepository.grassHome;
		pythonHome = GrassProcessRepository.pythonHome;
		pythonPath = GrassProcessRepository.pythonPath;
		gisrcDir = GrassProcessRepository.gisrcDir;
		addonPath = GrassProcessRepository.addonPath;
	}

	public ProcessDescriptionType createDescribeProcessType(String identifier, boolean addon)
			throws IOException, XmlException {

		Process proc = null;

		//addons have their own directory and are python scripts
		if(addon){

			if (!GrassIOHandler.OS_NAME.startsWith("Windows")) {
				proc = rt.exec(addonPath + fileSeparator +
						identifier + wpsProcessDescCmd,
						getEnvp());
			} else {
				proc = rt.exec(pythonHome  + fileSeparator + "python.exe " + addonPath
						+ fileSeparator + identifier + ".py"
						+ wpsProcessDescCmd, getEnvp());
			}			
			
		} else {

			if (!GrassIOHandler.OS_NAME.startsWith("Windows")) {
				proc = rt.exec(grassHome + fileSeparator + "bin"
						+ fileSeparator + identifier + wpsProcessDescCmd,
						getEnvp());
			} else {
				proc = rt.exec(grassHome + fileSeparator + "bin"
						+ fileSeparator + identifier + ".exe"
						+ wpsProcessDescCmd, getEnvp());
			}
		}
		
		PipedOutputStream pipedOut = new PipedOutputStream();

		PipedInputStream pipedIn = new PipedInputStream(pipedOut);

		PipedOutputStream pipedOutError = new PipedOutputStream();

		PipedInputStream pipedInError = new PipedInputStream(pipedOutError);
		
		// attach error stream reader
		JavaProcessStreamReader errorGobbler = new JavaProcessStreamReader(proc.getErrorStream(), pipedOutError);

		// attach output stream reader
		JavaProcessStreamReader outputGobbler = new JavaProcessStreamReader(proc.getInputStream(), pipedOut);

		// start them
		executor.execute(errorGobbler);
		executor.execute(outputGobbler);

		BufferedReader xmlReader = new BufferedReader(new InputStreamReader(
				pipedIn));

		String line = xmlReader.readLine();

		String xml = "";

		while (line != null) {

			xml = xml.concat(line + lineSeparator);

			line = xmlReader.readLine();
		}

		pipedIn.close();
		pipedOut.close();
		xmlReader.close();

		BufferedReader errorReader = new BufferedReader(new InputStreamReader(
				pipedInError));

		String errorLine = errorReader.readLine();

		String errors = "";

		while (errorLine != null) {

			errors = errors.concat(errorLine + lineSeparator);

			errorLine = errorReader.readLine();
		}

		if (!"".equals(errors)) {
			LOGGER.error("Error while creating processdescription for process "
					+ identifier + ": " + errors);
		}
		
		pipedInError.close();
		pipedOutError.close();
		errorReader.close();
		
		try {
			proc.waitFor();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		proc.destroy();

		ProcessDescriptionsDocument pDoc = ProcessDescriptionsDocument.Factory
				.parse(xml);

		int i = pDoc.getProcessDescriptions().getProcessDescriptionArray().length;

		if (i == 1) {

			ProcessDescriptionType result = pDoc.getProcessDescriptions()
					.getProcessDescriptionArray()[0];

			InputDescriptionType[] inputs = result.getDataInputs().getInputArray();
			
			for (InputDescriptionType inputDescriptionType : inputs) {
				checkForBase64Encoding(inputDescriptionType.getComplexData());
				checkForKMLMimeType(inputDescriptionType);	
				addZippedSHPMimeType(inputDescriptionType);
			}			
			
			SupportedComplexDataType outputType = result.getProcessOutputs()
					.getOutputArray(0).getComplexOutput();

			if (outputType != null) {

				String schema = outputType.getDefault().getFormat().getSchema();

				if (schema != null) {

                    if (schema.contains(XmlConstants.SCHEMA_LOCATION_GML_2_0_0) ||
                        schema.contains(XmlConstants.SCHEMA_LOCATION_GML_2_1_1) ||
                        schema.contains(XmlConstants.SCHEMA_LOCATION_GML_2_1_2) ||
                        schema.contains(XmlConstants.SCHEMA_LOCATION_GML_2_1_2_1) ||
                        schema.contains(XmlConstants.SCHEMA_LOCATION_GML_3_0_0) ||
                        schema.contains(XmlConstants.SCHEMA_LOCATION_GML_3_0_1) ||
                        schema.contains(XmlConstants.SCHEMA_LOCATION_GML_3_1_1)) {

                        ComplexDataDescriptionType xZippedShapeType = outputType
                                .getSupported().addNewFormat();
										
						xZippedShapeType.setMimeType(IOHandler.MIME_TYPE_ZIPPED_SHP);
						xZippedShapeType.setEncoding(IOHandler.ENCODING_BASE64);

						ComplexDataDescriptionType xZippedShapeTypeUTF8 = outputType.getSupported().addNewFormat();
																		
						xZippedShapeTypeUTF8.setMimeType(IOHandler.MIME_TYPE_ZIPPED_SHP);

					}
				}
				
				checkForBase64Encoding(result.getProcessOutputs()
						.getOutputArray(0).getComplexOutput());
				checkForKMLMimeType(result.getProcessOutputs()
						.getOutputArray(0));

			}

			return result;
		}

		return null;
	}

	private void checkForBase64Encoding(SupportedComplexDataType complexData){
		
		if(complexData == null){
			return;
		}
		
		Set<Format> genericFileParserMimeTypes = new GenericFileParser().getSupportedFormats();
		
		ComplexDataDescriptionType[] supportedTypes = complexData.getSupported().getFormatArray();
		
		for (ComplexDataDescriptionType complexDataDescriptionType : supportedTypes) {
			String supportedMimeType = complexDataDescriptionType.getMimeType();
			
			String supportedEncoding = complexDataDescriptionType.getEncoding();
			
			if(supportedMimeType != null && supportedEncoding == null){			
				for (Format format : genericFileParserMimeTypes) {
					if(format.hasMimeType(IOHandler.MIME_TYPE_ZIPPED_SHP)){
						continue;
					}
					if(format.hasMimeType(supportedMimeType)){
						ComplexDataDescriptionType base64Format = complexData.getSupported().addNewFormat();						
						base64Format.setMimeType(supportedMimeType);
						base64Format.setEncoding(IOHandler.ENCODING_BASE64);
					}
				}			
			}
		}
	}
	
	private void checkForKMLMimeType(InputDescriptionType inputDescriptionType) {
		
		SupportedComplexDataInputType complexData = inputDescriptionType.getComplexData();
		
		if(complexData == null){
			return;
		}
		
		if(complexData.getDefault().getFormat().getSchema() != null && complexData.getDefault().getFormat().getSchema().equals(XmlConstants.SCHEMA_LOCATION_KML_2_2_0)){
			complexData.getDefault().getFormat().setMimeType(GenericFileDataConstants.MIME_TYPE_KML);
			return; 
		}
		ComplexDataDescriptionType[] supportedTypes = complexData.getSupported().getFormatArray();
		
		for (ComplexDataDescriptionType complexDataDescriptionType : supportedTypes) {
			if(complexDataDescriptionType.getSchema() != null && complexDataDescriptionType.getSchema().equals(XmlConstants.SCHEMA_LOCATION_KML_2_2_0)){
				complexDataDescriptionType.setMimeType(GenericFileDataConstants.MIME_TYPE_KML);
				return;
			}
		}
		
	}
	
	private void addZippedSHPMimeType(InputDescriptionType inputDescriptionType) {
		
		SupportedComplexDataInputType complexData = inputDescriptionType.getComplexData();
		
		if(complexData == null){
			return;
		}
		ComplexDataDescriptionType[] supportedTypes = complexData.getSupported().getFormatArray();
		
		for (ComplexDataDescriptionType complexDataDescriptionType : supportedTypes) {
			if(complexDataDescriptionType.getSchema() != null && complexDataDescriptionType.getSchema().equals(XmlConstants.SCHEMA_LOCATION_GML_2_1_2)){
				inputDescriptionType.getComplexData().getSupported().addNewFormat().setMimeType(IOHandler.MIME_TYPE_ZIPPED_SHP);
				return;
			}
		}
		
	}

	private void checkForKMLMimeType(OutputDescriptionType outputDescriptionType) {
		
		SupportedComplexDataType complexData = outputDescriptionType.getComplexOutput();
		
		if(complexData == null){
			return;
		}
		
		if(complexData.getDefault().getFormat().getSchema() != null && complexData.getDefault().getFormat().getSchema().equals(XmlConstants.SCHEMA_LOCATION_KML_2_2_0)){
			complexData.getDefault().getFormat().setMimeType(GenericFileDataConstants.MIME_TYPE_KML);
			return; 
		}
		ComplexDataDescriptionType[] supportedTypes = complexData.getSupported().getFormatArray();
		
		for (ComplexDataDescriptionType complexDataDescriptionType : supportedTypes) {
			if(complexDataDescriptionType.getSchema() != null && complexDataDescriptionType.getSchema().equals(XmlConstants.SCHEMA_LOCATION_KML_2_2_0)){
				complexDataDescriptionType.setMimeType(GenericFileDataConstants.MIME_TYPE_KML);
				return;
			}
		}
		
	}

	private String[] getEnvp() {

		if (envp == null) {

			
			if (GrassIOHandler.OS_NAME.startsWith("Windows")) {

				envp = new String[] {
						"GISRC=" + gisrcDir,
						"GDAL_DATA=" + grassHome + fileSeparator + "etc"
								+ fileSeparator + "ogr_csv",
						"GISBASE=" + grassHome,
						"PATH=" + grassHome + fileSeparator + "lib;"
								+ grassHome + fileSeparator + "bin;"
								+ grassHome + fileSeparator + "scripts;"
								+ pythonHome + ";" + grassHome + fileSeparator
								+ "extralib;" + grassHome + fileSeparator
								+ "extrabin",
						"LD_LIBRARY_PATH=" + grassHome + fileSeparator + "lib",
						"PWD=" + grassHome,
						"PYTHONHOME=" + pythonHome,
						"PYTHONPATH=" + grassHome + fileSeparator + "etc"
								+ fileSeparator + "python",
						"GRASS_CONFIG_DIR=.grass7",
						"GRASS_GNUPLOT=gnuplot -persist", "GRASS_PAGER=less",
						"GRASS_PYTHON=python", "GRASS_SH=/bin/sh",
						"GRASS_VERSION=7.0.svn", "WINGISBASE=" + grassHome };
			}else{
				
				envp = new String[] {
						"GISRC=" + gisrcDir,
						"GDAL_DATA=" + grassHome + fileSeparator + "etc"
								+ fileSeparator + "ogr_csv",
						"GISBASE=" + grassHome,
						"PATH=" + grassHome + fileSeparator + "lib:"
								+ grassHome + fileSeparator + "bin:"
								+ grassHome + fileSeparator + "scripts:"
								+ pythonHome + ":" + grassHome + fileSeparator
								+ "extralib:" + grassHome + fileSeparator
								+ "extrabin",
						"LD_LIBRARY_PATH=" + grassHome + fileSeparator + "lib",
						"PWD=" + grassHome,
						"PYTHONHOME=" + pythonHome,
						"PYTHONPATH=" + grassHome + fileSeparator + "etc"
								+ fileSeparator + "python" + ":" + pythonPath,
						"GRASS_CONFIG_DIR=.grass7",
						"GRASS_GNUPLOT=gnuplot -persist", "GRASS_PAGER=less",
						"GRASS_PYTHON=python", "GRASS_SH=/bin/sh",
						"GRASS_VERSION=7.0.svn"};
				
			}
		}
		return envp;
	}
	
}