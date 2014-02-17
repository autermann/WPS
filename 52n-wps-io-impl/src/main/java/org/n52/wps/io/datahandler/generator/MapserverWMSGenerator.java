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
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.httpclient.HttpException;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.n52.wps.PropertyDocument.Property;
import org.n52.wps.commons.Format;
import org.n52.wps.commons.WPSConfig;
import org.n52.wps.commons.XMLUtil;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.io.datahandler.generator.mapserver.MSMapfileBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Generator for saving results of a WPS Process in an UMNMapserver. Right now
 * this generator only supports publishing results over an Mapserver-WMS. As
 * input this generator right now only supports GTVectorDataBinding. As template
 * for this class served the GeoserverWMSGenerator.
 * 
 * @author Jacob Mendt
 * 
 * @TODO Support more inputs (shapefile, raster)
 * @TODO Generator for WCS and WFS
 */
public class MapserverWMSGenerator extends AbstractGenerator {

	private String mapfile;
	private String workspace;
	private String shapefileRepository;
	private String wmsUrl;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MapserverWMSGenerator.class);

	/**
	 * Initialize a new MapserverWMSGenerator object. Parse the parameter
	 * Mapserver_workspace, Mapserver_mapfile, Mapserver_dataRepository and
	 * Mapserver_wmsUrl from the config.xml of the WPS.
	 */
	public MapserverWMSGenerator() {

		super(GTVectorDataBinding.class);

		Property[] properties = WPSConfig.getInstance()
				.getPropertiesForGeneratorClass(this.getClass().getName());

		for (Property property : properties) {
			if (property.getName().equalsIgnoreCase("Mapserver_workspace")) {
				workspace = property.getStringValue();
			}
			if (property.getName().equalsIgnoreCase("Mapserver_mapfile")) {
				mapfile = property.getStringValue();
			}
			if (property.getName().equalsIgnoreCase("Mapserver_dataRepository")) {
				shapefileRepository = property.getStringValue();
			}
			if (property.getName().equalsIgnoreCase("Mapserver_wmsUrl")) {
				wmsUrl = property.getStringValue();
			}
		}
	}

	@Override
	public InputStream generateStream(IData data, Format format)
			throws IOException {

		InputStream stream = null;
		try {
			Document doc = storeLayer(data);
			String xmlString = XMLUtil.nodeToString(doc);
			stream = new ByteArrayInputStream(xmlString.getBytes("UTF-8"));
	    } catch( TransformerException | IOException | ParserConfigurationException ex){
	    	LOGGER.error("Error generating MapServer WMS output. Reason: " + ex);
	    	throw new RuntimeException("Error generating MapServer WMS output. Reason: " + ex);
	    }
		return stream;
	}

	/**
	 * Stores the input data as an layer in the mapserver and creates an
	 * response document.
	 *
	 * @param coll
	 *            IData has to be instanceof GTVectorDataBinding
	 *
	 * @return Document XML response document.
	 *
	 * @throws HttpException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	private Document storeLayer(IData coll) throws HttpException, IOException,
			ParserConfigurationException {

		// tests if the mapscript.jar was loaded correctly
		try {
			//MapserverProperties.getInstance().testMapscriptLibrary();
			LOGGER.info("Mapscript is running correctly");
		} catch (Exception e){
			LOGGER.warn("Mapscript isn't running correctly", e);
			return null;
		}

		// adds the IData to the mapserver.
		String wmsLayerName = "";
		if (coll instanceof GTVectorDataBinding) {
			GTVectorDataBinding gtData = (GTVectorDataBinding) coll;
			SimpleFeatureCollection ftColl = (SimpleFeatureCollection) gtData.getPayload();
			wmsLayerName = MSMapfileBinding.getInstance().addFeatureCollectionToMapfile(ftColl, workspace,
					mapfile, shapefileRepository);
			LOGGER.info("Layer was added to the mapfile");
			System.gc();
		}

		// creates the response document
		String capabilitiesLink = wmsUrl + "?Service=WMS&Request=GetCapabilities";
		Document doc = createXML(wmsLayerName, capabilitiesLink);
		LOGGER.info("Capabilities document was generated.");

		return doc;

	}

	/**
	 * Creates an response xml, which contains the layer name, the resource link
	 * and a getCapabilities request for the publishing service.
	 *
	 * @param layerName
	 *            Name of the layer which was added to the mapserver.
	 * @param getCapabilitiesLink
	 *            GetCapabilties request to the publishing service.
	 *
	 * @return Document XML response document.
	 *
	 * @throws ParserConfigurationException
	 */
	private Document createXML(String layerName, String getCapabilitiesLink)
			throws ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		Document doc = factory.newDocumentBuilder().newDocument();

		Element root = doc.createElement("OWSResponse");
		root.setAttribute("type", "WMS");

		Element resourceIDElement = doc.createElement("ResourceID");
		resourceIDElement.appendChild(doc.createTextNode(layerName));
		root.appendChild(resourceIDElement);

		Element getCapabilitiesLinkElement = doc
				.createElement("GetCapabilitiesLink");
		getCapabilitiesLinkElement.appendChild(doc
				.createTextNode(getCapabilitiesLink));
		root.appendChild(getCapabilitiesLinkElement);
		/*
		 * Element directResourceLinkElement =
		 * doc.createElement("DirectResourceLink");
		 * directResourceLinkElement.appendChild
		 * (doc.createTextNode(getMapRequest));
		 * root.appendChild(directResourceLinkElement);
		 */
		doc.appendChild(root);

		return doc;
	}

}
