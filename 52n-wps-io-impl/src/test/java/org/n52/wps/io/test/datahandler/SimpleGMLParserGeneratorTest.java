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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStream;

import org.junit.ClassRule;
import org.junit.Test;

import org.n52.wps.commons.Format;
import org.n52.wps.commons.WPSConfigRule;
import org.n52.wps.io.datahandler.generator.SimpleGMLGenerator;
import org.n52.wps.io.datahandler.parser.SimpleGMLParser;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.server.ExceptionReport;

/**
 * This class is for testing the SimpleGMLParser and -Generator.
 *
 * @author Benjamin Pross(bpross-52n)
 *
 */
//TODO: seems, we can not generate shapefiles out of SimpleGML GTVectorDataBindings...
public class SimpleGMLParserGeneratorTest {
    @ClassRule
    public static final WPSConfigRule wpsConfig
            = new WPSConfigRule("/wps_config.xml");

    @Test
    public void testDataHandler() throws ExceptionReport, IOException {

        SimpleGMLGenerator generator = new SimpleGMLGenerator();
        SimpleGMLParser parser = new SimpleGMLParser();

        int i = 0;

        for (Format format : parser.getSupportedFormats()) {
            InputStream input = getClass().getResourceAsStream("/gmlpacket.xml");
            assertThat(input, is(notNullValue()));
            GTVectorDataBinding binding = parser.parse(input, format);

            assertThat(binding.getPayload(), is(notNullValue()));
//			assertThat(binding.getPayloadAsShpFile().exists(), is(true);
            assertThat(binding.getPayload().isEmpty(), is(false));

            if (i == 0) {
                InputStream resultStream = generator.generateStream(binding, format);

                GTVectorDataBinding binding2 = parser.parse(resultStream, format);

                assertThat(binding2.getPayload(), is(notNullValue()));
//					assertThat(parsedGeneratedBinding.getPayloadAsShpFile().exists(), is(true));
                assertThat(binding2.getPayload().isEmpty(), is(false));

                InputStream resultStreamBase64 = generator.generate(binding, format.withBase64Encoding());

                GTVectorDataBinding parsedGeneratedBindingBase64 = (GTVectorDataBinding) parser.parseBase64(resultStreamBase64, format);

                assertThat(parsedGeneratedBindingBase64.getPayload(), is(notNullValue()));
//					assertThat(parsedGeneratedBindingBase64.getPayloadAsShpFile().exists(), is(true));
                assertThat(parsedGeneratedBindingBase64.getPayload().isEmpty(), is(false));
            }
            ++i;

        }

    }
}
