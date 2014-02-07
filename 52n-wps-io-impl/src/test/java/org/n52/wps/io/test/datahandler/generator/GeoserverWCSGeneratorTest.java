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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.n52.wps.commons.Format;
import org.n52.wps.io.data.binding.complex.GTRasterDataBinding;
import org.n52.wps.io.datahandler.generator.GeoserverWCSGenerator;
import org.n52.wps.io.datahandler.generator.GeoserverWMSGenerator;
import org.n52.wps.io.datahandler.parser.GeotiffParser;
import org.n52.wps.io.test.datahandler.AbstractTestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeoserverWCSGeneratorTest extends AbstractTestCase<GeoserverWCSGenerator> {


	Logger LOGGER = LoggerFactory.getLogger(GeoserverWCSGeneratorTest.class);

	
	public void testGenerator() {
		
		if(!isDataHandlerActive()){
			return;
		}
//		if(!isDataHandlerActive()){
//			return;
//		}
//		
//		String className = t.getClass().getSimpleName();
//		
//		if(!WPSConfig.getInstance().isGeneratorActive(className)){
//			LOGGER.info("Skipping inactive Generator: " + className);
//			return;
//		}
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

		GTRasterDataBinding theBinding = theParser.parse(input, theParser.getSupportedFormats().iterator().next());

		assertTrue(theBinding.getPayload() != null);

		GeoserverWMSGenerator generator = new GeoserverWMSGenerator();

		for (Format format : generator.getSupportedFormats()) {
			try {
				InputStream resultStream = generator.generateStream(theBinding, format);
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resultStream));
				String line;
				while((line = bufferedReader.readLine()) != null){
					System.out.println(line);
				}
				String request = "http://localhost:8181/geoserver/wms?service=WMS&version=1.1.0&request=GetMap&layers=N52:primary738239570087452915.tif_72e5aa87-5e2e-4c70-b913-53f4bf910245&styles=&bbox=444650.0,4631220.0,451640.0,4640510.0&width=385&height=512&srs=EPSG:26716&format=image/tiff";
			} catch (IOException e) {
				e.printStackTrace();
				fail(e.getMessage());
			}

		}

	}


	@Override
	protected void initializeDataHandler() {		
		dataHandler = new GeoserverWCSGenerator();		
	}

}
