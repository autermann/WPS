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
package org.n52.wps.io.test.datahandler.parser;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.InputStream;

import org.apache.commons.codec.binary.Base64InputStream;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.junit.ClassRule;
import org.junit.Test;
import org.opengis.feature.Feature;

import org.n52.wps.commons.Format;
import org.n52.wps.commons.WPSConfigRule;
import org.n52.wps.io.datahandler.parser.GTBinZippedWKT64Parser;
import org.n52.wps.io.geotools.data.GTVectorDataBinding;

/**
 * This class is for testing the GTBinZippedWKT64Parser. A base64 encoded zip
 * file containing WKT files will be
 * read into a Base64InputStream. This stream will be handed to the parser.
 * It will be checked, whether the resulting FeatureCollection not null, not
 * empty and whether it can be written to a shapefile.
 * The parsed geometries are printed out.
 *
 * @author BenjaminPross
 *
 */
public class GTBinZippedWKT64ParserTest {
    @ClassRule
    public static final WPSConfigRule wpsConfig
            = new WPSConfigRule("/wps_config.xml");

    @Test
    public void testParser() {
        GTBinZippedWKT64Parser dataHandler = new GTBinZippedWKT64Parser();
        for (Format mimetype : dataHandler.getSupportedFormats()) {

            InputStream input = new Base64InputStream(getClass()
                    .getResourceAsStream("/wktgeometries.base64.zip"));
            assertThat(input, is(notNullValue()));
            GTVectorDataBinding theBinding = dataHandler.parse(input, mimetype);

            assertThat(theBinding.getPayload(), is(notNullValue()));
            assertThat(theBinding.getPayload().isEmpty(), is(false));

            FeatureCollection<?, ?> collection = theBinding.getPayload();

            FeatureIterator<?> featureIterator = collection.features();

            while (featureIterator.hasNext()) {
                Feature f = featureIterator.next();
                assertThat(f, is(notNullValue()));
            }
            assertThat(theBinding.getPayloadAsShpFile().exists(), is(true));

        }
    }
}
