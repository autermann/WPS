/**
 * ﻿Copyright (C) 2007
 * by 52 North Initiative for Geospatial Open Source Software GmbH
 *
 * Contact: Andreas Wytzisk
 * 52 North Initiative for Geospatial Open Source Software GmbH
 * Martin-Luther-King-Weg 24
 * 48155 Muenster, Germany
 * info@52north.org
 *
 * This program is free software; you can redistribute and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the
 * Free Software Foundation.
 *
 * This program is distributed WITHOUT ANY WARRANTY; even without the implied
 * WARRANTY OF MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program (see gnu-gpl v2.txt). If not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA or
 * visit the Free Software Foundation web page, http://www.fsf.org.
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
import org.n52.wps.io.data.binding.complex.GTRasterDataBinding;
import org.n52.wps.io.datahandler.generator.GeotiffGenerator;
import org.n52.wps.io.datahandler.parser.GeotiffParser;
import org.n52.wps.io.test.datahandler.AbstractTestCase;

public class GeotiffGeneratorTest extends AbstractTestCase<GeotiffGenerator> {

	public void testGenerator() {
		
		if(!isDataHandlerActive()){
			return;
		}

		String testFilePath = projectRoot
				+ "/52n-wps-io-impl/src/test/resources/6_UTM2GTIF.TIF";
		
		try {
			testFilePath = URLDecoder.decode(testFilePath, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			fail(e1.getMessage());
		}

		GeotiffParser theParser = new GeotiffParser();


		InputStream input = null;

		try {
			input = new FileInputStream(new File(testFilePath));
		} catch (FileNotFoundException e) {
			fail(e.getMessage());
		}


        Format mimeType = dataHandler.getSupportedFormats().iterator().next();
		GTRasterDataBinding theBinding = theParser.parse(input, mimeType);

		assertTrue(theBinding.getPayload() != null);
		

		for (Format format : dataHandler.getSupportedFormats()) {
			try {
				InputStream resultStream = dataHandler.generateStream(theBinding, format);
				GTRasterDataBinding rasterBinding = theParser.parse(resultStream, mimeType);
				assertTrue(rasterBinding.getPayload() != null);
				assertTrue(rasterBinding.getPayload().getDimension() != 0);
				assertTrue(rasterBinding.getPayload().getEnvelope() != null);
			} catch (IOException e) {
				e.printStackTrace();
				fail(e.getMessage());
			}

		}

	}

	@Override
	protected void initializeDataHandler() {
		dataHandler = new GeotiffGenerator();
		
	}

	
}
