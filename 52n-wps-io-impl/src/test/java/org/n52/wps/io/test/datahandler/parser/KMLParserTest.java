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
package org.n52.wps.io.test.datahandler.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.n52.wps.commons.Format;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.io.datahandler.parser.KMLParser;
import org.n52.wps.io.test.datahandler.AbstractTestCase;

public class KMLParserTest extends AbstractTestCase<KMLParser> {

	public void testParser(){	
		
		if(!isDataHandlerActive()){
			return;
		}
		
//		String testFilePath = projectRoot + "/52n-wps-io/src/test/resources/streams.kml";//from geoserver, fail
//		String testFilePath = projectRoot + "/52n-wps-io/src/test/resources/shape.kml";//can be read by grass gis, fail
//		String testFilePath = projectRoot + "/52n-wps-io/src/test/resources/states.kml";//geotools example kml, fail
		String testFilePath = projectRoot + "/52n-wps-io-impl/src/test/resources/x4.kml";//returned by our own generator, fail
				
		try {
			testFilePath = URLDecoder.decode(testFilePath, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			fail(e1.getMessage());
		}
		
		
		InputStream input = null;
		
		for (Format format : dataHandler.getSupportedFormats()) {
			
			try {
				input = new FileInputStream(new File(testFilePath));
			} catch (FileNotFoundException e) {
				fail(e.getMessage());
			}
			
			GTVectorDataBinding theBinding = dataHandler.parse(input, format);
			
			assertNotNull(theBinding.getPayload());
			
			try {
				File f = theBinding.getPayloadAsShpFile();				
				assertTrue(f.exists());	
			} catch (Exception e) {
				e.printStackTrace();
				fail(e.getMessage());
			}	
			assertTrue(!theBinding.getPayload().isEmpty());	
			
		}
		
	}

	@Override
	protected void initializeDataHandler() {
		dataHandler = new KMLParser();
	}
	
}
