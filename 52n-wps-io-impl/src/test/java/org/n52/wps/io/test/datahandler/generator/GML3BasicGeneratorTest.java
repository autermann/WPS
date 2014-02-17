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
package org.n52.wps.io.test.datahandler.generator;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStream;

import org.junit.ClassRule;
import org.junit.Test;

import org.n52.wps.commons.Format;
import org.n52.wps.commons.WPSConfigRule;
import org.n52.wps.io.datahandler.generator.GML3BasicGenerator;
import org.n52.wps.io.datahandler.parser.GML3BasicParser;
import org.n52.wps.io.geotools.data.GTVectorDataBinding;
import org.n52.wps.server.ExceptionReport;

public class GML3BasicGeneratorTest {
    @ClassRule
    public static final WPSConfigRule wpsConfig
            = new WPSConfigRule("/wps_config.xml");

    @Test
    public void testParser()
            throws IOException, ExceptionReport {
        GML3BasicGenerator dataHandler = new GML3BasicGenerator();
        GML3BasicParser theParser = new GML3BasicParser();

        Format format = new Format("text/xml; subtype=gml/3.2.1", "UTF-8",
                                   "http://schemas.opengis.net/gml/3.2.1/base/feature.xsd");
        GTVectorDataBinding binding1, binding2, binding64;
        try (InputStream in = getClass()
                .getResourceAsStream("/spearfish_restricted_sites_gml3.xml")) {
            binding1 = theParser.parse(in, format);
        }
        assertThat(binding1.getPayload(), is(notNullValue()));

        try (InputStream in = dataHandler .generateStream(binding1, format)) {
            binding2 = theParser.parse(in, format);
        }
        assertThat(binding2.getPayload(), is(notNullValue()));
        assertThat(binding2.getPayload().size(), is(binding1.getPayload().size()));
        assertThat(binding2.getPayloadAsShpFile().exists(), is(true));
        assertThat(binding2.getPayload().isEmpty(), is(false));

        try (InputStream in = dataHandler.generate(binding1, format.withBase64Encoding())) {
            binding64 = (GTVectorDataBinding) theParser .parseBase64(in, format);
        }
        assertThat(binding64.getPayload(), is(notNullValue()));
        assertThat(binding64.getPayload().size(), is(binding1.getPayload().size()));
        assertThat(binding64.getPayloadAsShpFile().exists(), is(true));
        assertThat(binding64.getPayload().isEmpty(), is(false));

    }

}
