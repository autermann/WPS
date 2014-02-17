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
import java.io.InputStreamReader;

import org.junit.ClassRule;
import org.junit.Test;

import org.n52.wps.commons.Format;
import org.n52.wps.commons.WPSConfigRule;
import org.n52.wps.io.datahandler.generator.GeoserverWCSGenerator;
import org.n52.wps.io.datahandler.parser.GeotiffParser;
import org.n52.wps.io.data.binding.complex.GTRasterDataBinding;
import org.n52.wps.server.ExceptionReport;

import com.google.common.io.CharStreams;

public class GeoserverWCSGeneratorTest {


    @ClassRule
    public static final WPSConfigRule wpsConfig
            = new WPSConfigRule("/wps_config.xml");

    @Test
	public void testGenerator() throws IOException, ExceptionReport {

        GeoserverWCSGenerator wcsGenerator = new GeoserverWCSGenerator();
		GeotiffParser parser = new GeotiffParser();
        GTRasterDataBinding binding;
        try (InputStream in = getClass().getResourceAsStream("/6_UTM2GTIF.TIF")) {
            binding = parser.parse(in, parser.getSupportedFormats().iterator().next());
        }

		assertThat(binding.getPayload(), is(notNullValue()));

		for (Format format : wcsGenerator.getSupportedFormats()) {
            try (InputStream resultStream = wcsGenerator.generateStream(binding, format);
                 InputStreamReader reader = new InputStreamReader(resultStream)) {
                String result = CharStreams.toString(reader);
            }

            //FIXME no test!?
            String request = "http://localhost:8181/geoserver/wms?service=WMS&version=1.1.0&request=GetMap&layers=N52:primary738239570087452915.tif_72e5aa87-5e2e-4c70-b913-53f4bf910245&styles=&bbox=444650.0,4631220.0,451640.0,4640510.0&width=385&height=512&srs=EPSG:26716&format=image/tiff";
		}
	}
}
