/**
 * ﻿Copyright (C) 2007 - 2014 52°North Initiative for Geospatial Open Source
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
 *       • Apache License, version 2.0
 *       • Apache Software License, version 1.0
 *       • GNU Lesser General Public License, version 3
 *       • Mozilla Public License, versions 1.0, 1.1 and 2.0
 *       • Common Development and Distribution License (CDDL), version 1.0
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.n52.wps.commons.Format;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.io.datahandler.generator.GML3BasicGenerator;
import org.n52.wps.io.datahandler.parser.GML3BasicParser;
import org.n52.wps.io.test.datahandler.AbstractTestCase;
import org.n52.wps.server.ExceptionReport;

public class GML3BasicGeneratorTest extends AbstractTestCase<GML3BasicGenerator> {

	public void testParser() {
		
        assertTrue(isDataHandlerActive());

		String testFilePath = projectRoot
				+ "/52n-wps-io-impl/src/test/resources/spearfish_restricted_sites_gml3.xml";

		try {
			testFilePath = URLDecoder.decode(testFilePath, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			fail(e1.getMessage());
		}
		
		GML3BasicParser theParser = new GML3BasicParser();

//		String[] mimetypes = theParser.getSupportedFormats();

		InputStream input = null;

		try {
			input = new FileInputStream(new File(testFilePath));
		} catch (FileNotFoundException e) {
			fail(e.getMessage());
		}

        Format format = new Format("text/xml; subtype=gml/3.2.1","UTF-8",
				"http://schemas.opengis.net/gml/3.2.1/base/feature.xsd");

		GTVectorDataBinding theBinding = theParser.parse(input,format);
		
		try {
			InputStream resultStream = dataHandler.generateStream(theBinding, format);
			
			GTVectorDataBinding parsedGeneratedBinding = theParser.parse(resultStream, format);
			
			assertNotNull(parsedGeneratedBinding.getPayload());
			assertTrue(theBinding.getPayload().size()==theBinding.getPayload().size());
			assertTrue(parsedGeneratedBinding.getPayloadAsShpFile().exists());
			assertTrue(!parsedGeneratedBinding.getPayload().isEmpty());

			InputStream resultStreamBase64 = dataHandler.generate(theBinding, format.withBase64Encoding());
			
			GTVectorDataBinding parsedGeneratedBindingBase64 = (GTVectorDataBinding) theParser.parseBase64(resultStreamBase64, format);
			
			assertNotNull(parsedGeneratedBindingBase64.getPayload());
			assertTrue(parsedGeneratedBindingBase64.getPayloadAsShpFile().exists());
			assertTrue(!parsedGeneratedBindingBase64.getPayload().isEmpty());
			
		} catch (IOException | ExceptionReport e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		// }

	}

	@Override
	protected void initializeDataHandler() {
		dataHandler = new GML3BasicGenerator();
		
	}

}
