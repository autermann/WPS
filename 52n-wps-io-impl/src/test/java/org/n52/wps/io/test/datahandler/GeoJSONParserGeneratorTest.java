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
package org.n52.wps.io.test.datahandler;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.hamcrest.Matchers;
import org.junit.ClassRule;
import org.junit.Test;

import org.n52.wps.commons.Format;
import org.n52.wps.commons.WPSConfigRule;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.JTSGeometryBinding;
import org.n52.wps.io.datahandler.generator.GeoJSONGenerator;
import org.n52.wps.io.datahandler.parser.GeoJSONParser;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;

import com.google.common.io.CharStreams;

/**
 * Test class for GeoJSON parser and generator
 * @author Benjamin Pross(bpross-52n)
 *
 */
public class GeoJSONParserGeneratorTest {

    @ClassRule
    public static final WPSConfigRule wpsConfig
            = new WPSConfigRule("/wps_config.xml");

    @Test
    public void testParseWriteGeoJSONPoint()
            throws IOException {
        String inputString = "{\"type\":\"Point\",\"coordinates\":[100,0.0]}";
        GeoJSONParser theParser = new GeoJSONParser();
        GeoJSONGenerator generator = new GeoJSONGenerator();
        Format format = theParser.getSupportedFormats().iterator().next();
        IData binding;
        try (InputStream in = new ByteArrayInputStream(inputString.getBytes())) {
            binding = theParser.parse(in, format);
        }

        assertThat(binding, is(instanceOf(JTSGeometryBinding.class)));
        assertThat(binding.getPayload(), is(Matchers.notNullValue()));
        String outputString;
        try (InputStream generatedStream = generator.generateStream(binding, format);
             InputStreamReader reader = new InputStreamReader(generatedStream)) {
            outputString = CharStreams.toString(reader);
        }
        assertThat(outputString, is(equalTo(inputString)));
    }

	@Test
	public void testParseWriteGeoJSONFeatureCollection() throws IOException{

		String string = "{ \"type\": \"FeatureCollection\",                               "+
				"  \"features\": [                                                        "+
				"    { \"type\": \"Feature\",                                             "+
				"      \"geometry\": {\"type\": \"Point\", \"coordinates\": [102.0, 0.5]},"+
				"      \"properties\": {\"prop0\": \"value0\"}                            "+
				"      },                                                                 "+
				"    { \"type\": \"Feature\",                                             "+
				"      \"geometry\": {                                                    "+
				"        \"type\": \"LineString\",                                        "+
				"        \"coordinates\": [                                               "+
				"          [102.0, 0.0], [103.0, 1.0], [104.0, 0.0], [105.0, 1.0]         "+
				"          ]                                                              "+
				"        },                                                               "+
				"      \"properties\": {                                                  "+
				"        \"prop0\": \"value0\"                                            "+
				"        }                                                                "+
				"      },                                                                 "+
				"    { \"type\": \"Feature\",                                             "+
				"       \"geometry\": {                                                   "+
				"         \"type\": \"Polygon\",                                          "+
				"         \"coordinates\": [                                              "+
				"           [ [100.0, 0.0], [101.0, 0.0], [101.0, 1.0],                   "+
				"             [100.0, 1.0], [100.0, 0.0] ]                                "+
				"           ]                                                             "+
				"       },                                                                "+
				"       \"properties\": {                                                 "+
				"         \"prop0\": \"value0\"                              "+
				"         }                                                               "+
				"       }                                                                 "+
				"     ]                                                                   "+
				"   }                                                                     ";
        GeoJSONParser parser = new GeoJSONParser();
        GeoJSONGenerator generator = new GeoJSONGenerator();

        Format mimeType = parser.getSupportedFormats().iterator().next();
        IData binding1, binding2;

        try (InputStream in = new ByteArrayInputStream(string.getBytes())) {
            binding1 = parser.parse(in, mimeType);
        }
        assertThat(binding1, is(instanceOf(GTVectorDataBinding.class)));
        assertThat(binding1.getPayload(), is(notNullValue()));

        try (InputStream is = generator.generateStream(binding1, mimeType)) {
            binding2 = parser.parse(is, mimeType);
        }

        assertThat(binding2, is(instanceOf(GTVectorDataBinding.class)));
        assertThat(binding2.getPayload(), is(notNullValue()));

    }

	@Test
	public void testParseWriteGeoJSONFeature() throws IOException{

        String string = "{\"type\":\"Feature\", \"properties\":{}, \"geometry\":{\"type\":\"Polygon\", \"coordinates\":[[[56.390622854233, 29.90625500679], [67.640622854233, 49.59375500679], [82.406247854233, 39.75000500679], [69.749997854233, 23.57813000679], [56.390622854233, 29.90625500679]]]}, \"crs\":{\"type\":\"name\", \"properties\":{\"name\":\"EPSG:4326\"}}}";

        GeoJSONParser parser = new GeoJSONParser();
        GeoJSONGenerator generator = new GeoJSONGenerator();
        Format mimetype = parser.getSupportedFormats().iterator().next();
        IData binding1, binding2;

        try (InputStream in = new ByteArrayInputStream(string.getBytes())) {
            binding1 = parser.parse(in, mimetype);
        }

        assertThat(binding1, is(instanceOf(GTVectorDataBinding.class)));
        assertThat(binding1.getPayload(), is(notNullValue()));

        try (InputStream is = generator.generateStream(binding1, mimetype)) {
            binding2 = parser.parse(is, mimetype);
        }


        assertThat(binding2, is(instanceOf(GTVectorDataBinding.class)));
        assertThat(binding2.getPayload(), is(notNullValue()));
	}
}
