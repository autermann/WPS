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
package org.n52.wps.server.request;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import net.opengis.wps.x100.ExecuteResponseDocument;
import net.opengis.wps.x100.StatusType;

import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlValidationError;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import org.n52.wps.server.database.DatabaseFactory;

/**
 *
 * @author bpross-52n
 */
public class ExecuteRequestTest {

    private DocumentBuilderFactory fac;

	@Before
    public void setUp(){
		System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
		fac = DocumentBuilderFactory.newInstance();
		fac.setNamespaceAware(true);
    }

	@Test
    public void testUpdateStatusError() throws Exception {
		FileInputStream fis = new FileInputStream(new File("src/test/resources/LRDTCCorruptInputResponseDocStatusTrue.xml"));
        DatabaseFactory.getInstance().init(null);
		// parse the InputStream to create a Document
		Document doc = fac.newDocumentBuilder().parse(fis);
    	ExecuteRequest request = new ExecuteRequest(doc);
    	String exceptionText = "TestError";
    	request.updateStatusError(exceptionText);
    	File response = DatabaseFactory.getDatabase().getResponseAsFile(request.getUniqueId().toString());
    	ExecuteResponseDocument responseDoc = ExecuteResponseDocument.Factory.parse(response);
    	StatusType statusType = responseDoc.getExecuteResponse().getStatus();
    	assertTrue(validateExecuteResponse(responseDoc));
    	assertTrue(statusType.isSetProcessFailed());
    	assertTrue(statusType.getProcessFailed().getExceptionReport().getExceptionArray(0).getExceptionTextArray(0).equals(exceptionText));

    }

    private boolean validateExecuteResponse(ExecuteResponseDocument responseDoc) {
        XmlOptions xmlOptions = new XmlOptions();
        List<XmlValidationError> xmlValidationErrorList = new ArrayList<XmlValidationError>();
            xmlOptions.setErrorListener(xmlValidationErrorList);
        boolean valid = responseDoc.validate(xmlOptions);
        if (!valid) {
            System.err.println("Error validating process description for " + getClass().getCanonicalName());
            for (XmlValidationError xmlValidationError : xmlValidationErrorList) {
                System.err.println("\tMessage: " +  xmlValidationError.getMessage());
                System.err.println("\tLocation of invalid XML: " +
                     xmlValidationError.getCursorLocation().xmlText());
            }
        }
        return valid;
    }
}
