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
import java.util.Set;

import org.n52.wps.commons.Format;
import org.n52.wps.io.IOHandler;
import org.n52.wps.io.data.binding.complex.AsciiGrassDataBinding;
import org.n52.wps.io.data.binding.complex.GTRasterDataBinding;
import org.n52.wps.io.datahandler.generator.AsciiGrassGenerator;
import org.n52.wps.io.datahandler.parser.AsciiGrassParser;
import org.n52.wps.io.datahandler.parser.GeotiffParser;
import org.n52.wps.io.test.datahandler.AbstractTestCase;
import org.n52.wps.server.ExceptionReport;

public class AsciiGrassGeneratorTest extends AbstractTestCase<AsciiGrassGenerator> {

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

		Set<Format> formats = theParser.getSupportedFormats();

		InputStream input = null;

		try {
			input = new FileInputStream(new File(testFilePath));
		} catch (FileNotFoundException e) {
			fail(e.getMessage());
		}
        Format format = formats.iterator().next();

		GTRasterDataBinding theBinding = theParser.parse(input, format);

		assertTrue(theBinding.getPayload() != null);
		
		Set<Format> formats2 = dataHandler.getSupportedFormats();

		AsciiGrassParser asciiGrassParser = new AsciiGrassParser();
		
		for (Format format2 : formats2) {
			try {
				InputStream resultStream = dataHandler.generateStream(theBinding, format2);
				
				AsciiGrassDataBinding rasterBinding = asciiGrassParser.parse(resultStream, format);
				
				assertTrue(rasterBinding.getPayload() != null);
				assertTrue(rasterBinding.getPayload().getDimension() != 0);
				assertTrue(rasterBinding.getPayload().getEnvelope() != null);
				
				InputStream resultStreamBase64 = dataHandler.generate(theBinding, format2.withEncoding(IOHandler.ENCODING_BASE64));
				
				AsciiGrassDataBinding rasterBindingBase64 = (AsciiGrassDataBinding) asciiGrassParser.parseBase64(resultStreamBase64, format);
				
				assertTrue(rasterBindingBase64.getPayload() != null);
				assertTrue(rasterBindingBase64.getPayload().getDimension() != 0);
				assertTrue(rasterBindingBase64.getPayload().getEnvelope() != null);
				
            } catch (IOException | ExceptionReport e) {
				e.printStackTrace();
				fail(e.getMessage());
			}

		}

	}

	@Override
	protected void initializeDataHandler() {
		dataHandler = new AsciiGrassGenerator();
		
	}

	
}
