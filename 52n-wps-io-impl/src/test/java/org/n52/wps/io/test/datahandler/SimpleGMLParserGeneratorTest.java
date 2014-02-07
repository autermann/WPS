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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.n52.wps.commons.Format;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.io.datahandler.generator.SimpleGMLGenerator;
import org.n52.wps.io.datahandler.parser.SimpleGMLParser;
import org.n52.wps.server.ExceptionReport;

/**
 * This class is for testing the SimpleGMLParser and -Generator.
 * 
 * @author Benjamin Pross(bpross-52n)
 *
 */
public class SimpleGMLParserGeneratorTest extends
		AbstractTestCase<SimpleGMLGenerator> {

	public void testDataHandler() {

		assertTrue(isDataHandlerActive());

		String testFilePath = projectRoot
				+ "/52n-wps-io-impl/src/test/resources/gmlpacket.xml";

		try {
			testFilePath = URLDecoder.decode(testFilePath, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			fail(e1.getMessage());
		}

		InputStream input = null;

		SimpleGMLParser parser = new SimpleGMLParser();

        int i= 0;

		for (Format format : parser.getSupportedFormats()) {
			try {
				input = new FileInputStream(new File(testFilePath));
			} catch (FileNotFoundException e) {
				fail(e.getMessage());
			}
			
			GTVectorDataBinding binding = parser.parse(input, format);

			assertNotNull(binding.getPayload());
			//TODO: seems, we can not generate shapefiles out of SimpleGML GTVectorDataBindings...
//			assertTrue(binding.getPayloadAsShpFile().exists());
			assertTrue(!binding.getPayload().isEmpty());

			if (i == 0) {

				try {
					InputStream resultStream = dataHandler.generateStream(binding, format);

					GTVectorDataBinding parsedGeneratedBinding = parser.parse(resultStream, format);

					assertNotNull(parsedGeneratedBinding.getPayload());
//					assertTrue(parsedGeneratedBinding.getPayloadAsShpFile()
//							.exists());
					assertTrue(!parsedGeneratedBinding.getPayload().isEmpty());

					InputStream resultStreamBase64 = dataHandler.generate(binding, format.withBase64Encoding());

					GTVectorDataBinding parsedGeneratedBindingBase64 = (GTVectorDataBinding) parser.parseBase64(resultStreamBase64, format);

					assertNotNull(parsedGeneratedBindingBase64.getPayload());
//					assertTrue(parsedGeneratedBindingBase64
//							.getPayloadAsShpFile().exists());
					assertTrue(!parsedGeneratedBindingBase64.getPayload().isEmpty());

				} catch (IOException | ExceptionReport e) {
					System.err.println(e);
					fail(e.getMessage());
				}
			}
            ++i;

		}

	}

	@Override
	protected void initializeDataHandler() {
		dataHandler = new SimpleGMLGenerator();
	}

}
