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
package org.n52.wps.io.datahandler.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.GeometryAttributeImpl;
import org.geotools.feature.type.GeometryDescriptorImpl;
import org.geotools.feature.type.GeometryTypeImpl;
import org.geotools.filter.identity.GmlObjectIdImpl;
import org.geotools.gml2.GMLConfiguration;
import org.geotools.gml3.ApplicationSchemaConfiguration;
import org.geotools.xml.Configuration;
import org.geotools.xml.Parser;
import org.n52.wps.commons.Format;
import org.n52.wps.io.SchemaRepository;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.filter.identity.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.google.common.io.ByteStreams;
import com.vividsolutions.jts.geom.Geometry;

/**
 * This parser handles xml files compliant to GML2.
 * 
 * @author foerster
 * 
 */
public class GML2BasicParser extends AbstractParser {
	private static final Logger LOGGER = LoggerFactory.getLogger(GML2BasicParser.class);

	public GML2BasicParser() {
		super(GTVectorDataBinding.class);
	}

    @Override
    public GTVectorDataBinding parse(InputStream stream, Format format) 
            throws IOException {
        File tempFile = registerTempFile(File.createTempFile(UUID.randomUUID().toString(), ".gml2"));
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            ByteStreams.copy(stream, fos);
        }
        return new GTVectorDataBinding(parseSimpleFeatureCollection(tempFile));
    }

	public SimpleFeatureCollection parseSimpleFeatureCollection(File file) {
		QName schematypeTuple = determineFeatureTypeSchema(file);
		Configuration configuration;
		boolean shouldSetParserStrict = true;
		String schemaLocation = schematypeTuple.getLocalPart();
		if (schemaLocation != null && schematypeTuple.getNamespaceURI() != null) {
			SchemaRepository.registerSchemaLocation(schematypeTuple.getNamespaceURI(), schemaLocation);
			configuration = new ApplicationSchemaConfiguration(schematypeTuple.getNamespaceURI(), schemaLocation);
		} else {
			configuration = new GMLConfiguration();
			shouldSetParserStrict = false;
		}

		Parser parser = new Parser(configuration);

		// parse
		SimpleFeatureCollection fc;
		try {
			Object parsedData;
			try {
				parser.setStrict(shouldSetParserStrict);
				parsedData = parser.parse(new FileInputStream(file));
			} catch (SAXException e5) {
				// assume the xsd containing the schema was not found
				configuration = new GMLConfiguration();
				parser = new Parser(configuration);
				parser.setStrict(false);
				parsedData = parser.parse(new FileInputStream(file));
			}
			if (parsedData instanceof SimpleFeatureCollection) {
				fc = (SimpleFeatureCollection) parsedData;
			} else {
				List<?> possibleSimpleFeatureList = (List) ((Map) parsedData).get("featureMember");

				if (possibleSimpleFeatureList != null) {
					List<SimpleFeature> simpleFeatureList = new ArrayList<>(possibleSimpleFeatureList.size());

					SimpleFeatureType sft = null;

					for (Object possibleSimpleFeature : possibleSimpleFeatureList) {

						if (possibleSimpleFeature instanceof SimpleFeature) {
							SimpleFeature sf = ((SimpleFeature) possibleSimpleFeature);
							if (sft == null) {
								sft = sf.getType();
							}
							simpleFeatureList.add(sf);
						}
					}

					fc = new ListFeatureCollection(sft, simpleFeatureList);
				} else {
					fc = (SimpleFeatureCollection) ((Map) parsedData).get("FeatureCollection");
				}
			}

			SimpleFeatureIterator featureIterator = fc.features();
			while (featureIterator.hasNext()) {
				SimpleFeature feature = featureIterator.next();
				if (feature.getDefaultGeometry() == null) {
					Collection<Property> properties = feature.getProperties();
					for (Property property : properties) {
                        if (property.getValue() instanceof Geometry) {
                            Geometry g = (Geometry) property.getValue();
                            GeometryAttribute old = feature.getDefaultGeometryProperty();
                            GeometryType type = new GeometryTypeImpl(property.getName(),
                                    old.getType().getBinding(),
                                    old.getType().getCoordinateReferenceSystem(),
                                    old.getType().isIdentified(),
                                    old.getType().isAbstract(),
                                    old.getType().getRestrictions(),
                                    old.getType().getSuper(),
                                    old.getType().getDescription());

                            GeometryDescriptor newGeometryDescriptor = new GeometryDescriptorImpl(type, property.getName(), 0, 1, true, null);
                            Identifier identifier = new GmlObjectIdImpl(feature.getID());
                            GeometryAttributeImpl geo = new GeometryAttributeImpl(g, newGeometryDescriptor, identifier);
                            feature.setDefaultGeometryProperty(geo);
                            feature.setDefaultGeometry(g);
                        }
					}
				}
			}
			return fc;
		} catch (IOException | ParserConfigurationException | SAXException | NoSuchElementException e) {
			LOGGER.error("Exception while trying to parse GML2 FeatureCollection.", e);
			throw new RuntimeException(e);
		}
	}

	private QName determineFeatureTypeSchema(File file) {
		try {
			GML2Handler handler = new GML2Handler();
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setNamespaceAware(true);
			factory.newSAXParser().parse(new FileInputStream(file), handler);
			return new QName(handler.getNameSpaceURI(), handler.getSchemaUrl());
		} catch (ParserConfigurationException | SAXException | IOException e) {
			LOGGER.error("Exception while trying to determining GML2 FeatureType schema.", e);
			throw new IllegalArgumentException(e);
		}
	}

}
