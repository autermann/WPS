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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.ClassRule;
import org.junit.Test;

import org.n52.wps.commons.Format;
import org.n52.wps.commons.WPSConfigRule;
import org.n52.wps.io.data.binding.complex.JTSGeometryBinding;
import org.n52.wps.io.datahandler.generator.WKTGenerator;
import org.n52.wps.io.datahandler.parser.WKTParser;

import com.google.common.io.CharStreams;

/**
 * Test class for WKT parser and generator
 *
 * @author Benjamin Pross
 *
 */
public class WKTParserGeneratorTest {

    @ClassRule
    public static final WPSConfigRule wpsConfig
            = new WPSConfigRule("/wps_config.xml");

    @Test
    public void testGenerator()
            throws IOException {
        final WKTParser parser = new WKTParser();
        final WKTGenerator generator = new WKTGenerator();
        final Format mimetype = parser.getSupportedFormats().iterator().next();
        final String inputString = "POLYGON ((30 10, 10 20, 20 40, 40 40, 30 10))";

        JTSGeometryBinding binding;
        try (InputStream in = new ByteArrayInputStream(inputString.getBytes())) {
            binding = parser.parse(in, mimetype);
        }
        assertThat(binding.getPayload(), is(notNullValue()));

        String outputString;
        try (InputStream in = generator.generateStream(binding, mimetype);
             InputStreamReader reader = new InputStreamReader(in)) {
            outputString = CharStreams.toString(reader);
        }
        assertThat(outputString, is(equalTo(inputString)));
    }

}
