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
import org.n52.wps.io.data.GenericFileData;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GTRasterDataBinding;
import org.n52.wps.io.data.binding.complex.GeotiffBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


//TODO: compact the 3 OWS Generators into a single one

public class GeoserverWCSGenerator extends AbstractGenerator {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(GeoserverWCSGenerator.class);
	private String username;
	private String password;
	private String host;
	private String port;
	
	public GeoserverWCSGenerator() {
		super(GTRasterDataBinding.class, GeotiffBinding.class);
		
		for(Property property : WPSConfig.getInstance().getPropertiesForGeneratorClass(this.getClass().getName())){
			if(property.getName().equalsIgnoreCase("Geoserver_username")){
				username = property.getStringValue();
			}
			if(property.getName().equalsIgnoreCase("Geoserver_password")){
				password = property.getStringValue();
			}
			if(property.getName().equalsIgnoreCase("Geoserver_host")){
				host = property.getStringValue();
			}
			if(property.getName().equalsIgnoreCase("Geoserver_port")){
				port = property.getStringValue();
			}
		}
		if(port == null){
			port = WPSConfig.getInstance().getWPSConfig().getServer().getHostport();
		}
	}
	
	@Override
	public InputStream generateStream(IData data, Format format) throws IOException {

		InputStream stream = null;	
		try {
			Document doc = storeLayer(data);
			String xmlString = XMLUtil.nodeToString(doc);			
			stream = new ByteArrayInputStream(xmlString.getBytes("UTF-8"));			
	    } catch( TransformerException | IOException | ParserConfigurationException e){
	    	LOGGER.error("Error generating WCS output. Reason: ", e);
	    	throw new RuntimeException("Error generating WCS output. Reason: " + e);
	    }	
		return stream;
	}
	
	private Document storeLayer(IData coll) throws HttpException, IOException, ParserConfigurationException{
		File file = null;
		String storeName = "";
		
		if(coll instanceof GTRasterDataBinding){
			GTRasterDataBinding gtData = (GTRasterDataBinding) coll;
			GenericFileData fileData = new GenericFileData(gtData.getPayload(), null);
			file = fileData.getBaseFile(true);			
		}
		if(coll instanceof GeotiffBinding){
			GeotiffBinding data = (GeotiffBinding) coll;
			file = data.getPayload();
		}
		
		storeName = file.getName();			
	
		storeName = storeName +"_" + UUID.randomUUID();
		GeoServerUploader geoserverUploader = new GeoServerUploader(username, password, host, port);
		
		String result = geoserverUploader.createWorkspace();
		LOGGER.debug(result);
		if(coll instanceof GTRasterDataBinding){
			result = geoserverUploader.uploadGeotiff(file, storeName);
		}		
		LOGGER.debug(result);
				
		String capabilitiesLink = "http://"+host+":"+port+"/geoserver/wcs?Service=WCS&Request=GetCapabilities&Version=1.1.1";
				
		Document doc = createXML(storeName, capabilitiesLink);
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
