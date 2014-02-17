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

import org.junit.ClassRule;
import org.junit.Test;

import org.n52.wps.commons.Format;
import org.n52.wps.commons.WPSConfigRule;
import org.n52.wps.io.data.binding.complex.GenericFileDataBinding;
import org.n52.wps.io.datahandler.parser.GenericFileParser;

public class GenericFileParserTest {
    @ClassRule
    public static final WPSConfigRule wpsConfig
            = new WPSConfigRule("/wps_config.xml");

    @Test
    public void testParser() {
        GenericFileParser dataHandler = new GenericFileParser();
        for (Format mimetype : dataHandler.getSupportedFormats()) {
            InputStream input = getClass().getResourceAsStream("/testfile");
            assertThat(input, is(notNullValue()));
            GenericFileDataBinding theBinding = dataHandler
                    .parse(input, mimetype);
            assertThat(theBinding.getPayload().getBaseFile(true).exists(), is(true));
        }
    }
}
