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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geojson.geom.GeometryJSON;
import org.n52.wps.commons.Format;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.io.data.binding.complex.JTSGeometryBinding;
import org.opengis.feature.simple.SimpleFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.CharStreams;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;

/**
 * 
 * 
 * This class parses json into JTS geometries.
 *         
 *  @author BenjaminPross(bpross-52n)
 * 
 */
public class GeoJSONParser extends AbstractParser {

	private static final Logger LOGGER = LoggerFactory.getLogger(GeoJSONParser.class);

	public GeoJSONParser() {
		super(JTSGeometryBinding.class, GTVectorDataBinding.class);
	}

	@Override
	public IData parse(InputStream input, Format format) {

		String geojsonstring;
		try {
            geojsonstring = CharStreams.toString(new InputStreamReader(input,
                format.getEncoding().or(DEFAULT_ENCODING)));
		} catch (IOException e) {
			LOGGER.error("Exception while reading inputstream.", e);
            throw new RuntimeException(e);
		} finally {
			try {
				input.close();
			} catch (IOException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}

		if (geojsonstring.contains("FeatureCollection")) {

			try {
				FeatureCollection<?, ?> featureCollection = new FeatureJSON()
						.readFeatureCollection(geojsonstring);

				return new GTVectorDataBinding(featureCollection);

			} catch (IOException e) {
				LOGGER.info("Could not read FeatureCollection from inputstream");
			}

		} else if (geojsonstring.contains("Feature")) {

			try {
				SimpleFeature feature = new FeatureJSON().readFeature(geojsonstring);

				List<SimpleFeature> featureList = new ArrayList<SimpleFeature>();

				featureList.add(feature);
				
				ListFeatureCollection featureCollection = new ListFeatureCollection(
						feature.getFeatureType(), featureList);

				return new GTVectorDataBinding(featureCollection);

			} catch (IOException e) {
				LOGGER.info("Could not read Feature from inputstream");
			}

		} else if (geojsonstring.contains("GeometryCollection")) {

			try {
				GeometryCollection g = new GeometryJSON().readGeometryCollection(geojsonstring);

				return new JTSGeometryBinding(g);

			} catch (IOException e) {
				LOGGER.info("Could not read GeometryCollection from inputstream.");
			}

		} else if(geojsonstring.contains("Point") || 
				geojsonstring.contains("LineString") ||
				geojsonstring.contains("Polygon") ||
				geojsonstring.contains("MultiPoint") ||
				geojsonstring.contains("MultiLineString") ||
				geojsonstring.contains("MultiPolygon")){

			try {
				Geometry g = new GeometryJSON().read(geojsonstring);

				return new JTSGeometryBinding(g);

			} catch (IOException e) {
				LOGGER.info("Could not read single Geometry from inputstream.");
			}

		}
		LOGGER.error("Could not parse inputstream, returning null.");
		return null;
	}

}
