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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.n52.wps.commons.Format;
import org.n52.wps.io.data.binding.complex.GenericFileDataBinding;
import org.n52.wps.io.datahandler.parser.GML2BasicParser4Files;
import org.n52.wps.io.test.datahandler.AbstractTestCase;

/**
 * This class is for testing the GML2BasicParser4Files.
 * 
 * @author Benjamin Pross(bpross-52n)
 *
 */
public class GML2BasicParser4FilesTest extends
		AbstractTestCase<GML2BasicParser4Files> {

	public void testParser() {

		if (!isDataHandlerActive()) {
			return;
		}

		String testFilePath = projectRoot
				+ "/52n-wps-io-impl/src/test/resources/tasmania_roads_gml2.xml";

		try {
			testFilePath = URLDecoder.decode(testFilePath, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			System.err.println(e1);
			fail(e1.getMessage());
		}

		InputStream input = null;

		Format format = dataHandler.getSupportedFormats().iterator().next();


		try {
			input = new FileInputStream(new File(testFilePath));
		} catch (FileNotFoundException e) {
			fail(e.getMessage());
		}

		GenericFileDataBinding theBinding = dataHandler.parse(input, format);

		assertNotNull(theBinding.getPayload());
		assertNotNull(theBinding.getPayload().getBaseFile(true).exists());

	}

	@Override
	protected void initializeDataHandler() {
		dataHandler = new GML2BasicParser4Files();
	}

}
