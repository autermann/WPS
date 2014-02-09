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
package org.n52.wps.transactional.algorithm;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import net.opengis.wps.x100.OutputDataType;
import net.opengis.wps.x100.OutputDescriptionType;
import net.opengis.wps.x100.ProcessDescriptionType;

import org.n52.wps.commons.Format;
import org.n52.wps.io.IOHandler;
import org.n52.wps.io.IParser;
import org.n52.wps.io.ParserFactory;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.ILiteralData;
import org.n52.wps.io.data.binding.complex.GTRasterDataBinding;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.io.LiteralDataFactory;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.InvalidParameterValueException;
import org.n52.wps.server.NoApplicableCodeException;
import org.n52.wps.server.OperationNotSupportedException;


public class OutputParser {
	
	
	/**
	 * Handles the ComplexValueReference
	 * @param class1 
	 * @param input The client input
	 * @throws ExceptionReport If the input (as url) is invalid, or there is an error while parsing the XML.
	 */
	protected  static String handleComplexValueReference(OutputDataType output) throws ExceptionReport{
		return output.getReference().getHref();
		
	}
	
	/**
	 * Handles the complexValue, which in this case should always include XML 
	 * which can be parsed into a FeatureCollection.
	 * @param class1 
	 * @param input The client input
	 * @throws ExceptionReport If error occured while parsing XML
	 */
	protected static IData handleComplexValue(OutputDataType output, ProcessDescriptionType processDescription) throws ExceptionReport{
		String outputID = output.getIdentifier().getStringValue();
		String complexValue = output.getData().getComplexData().toString();
		OutputDescriptionType outputDesc = null;
		for(OutputDescriptionType tempDesc : processDescription.getProcessOutputs().getOutputArray()) {
			if((tempDesc.getIdentifier().getStringValue().startsWith(outputID))) {
				outputDesc = tempDesc;
				break;
			}
		}

		if(outputDesc == null) {
			throw new RuntimeException("output cannot be found in description for " + processDescription.getIdentifier().getStringValue() + "," + outputID);
		}
		
		// get data specification from request
		String schema = output.getData().getComplexData().getSchema();
		String encoding = output.getData().getComplexData().getEncoding();
		String mimeType = output.getData().getComplexData().getMimeType();
		
		// check for null elements in request and replace by defaults
		if(schema == null) {
			schema = outputDesc.getComplexOutput().getDefault().getFormat().getSchema();
		}
		if(mimeType == null) {
			mimeType = outputDesc.getComplexOutput().getDefault().getFormat().getMimeType();
		}
		if(encoding == null) {
			encoding = outputDesc.getComplexOutput().getDefault().getFormat().getEncoding();
		}
		
		Class<?> outputDataType = determineOutputDataType(outputID, outputDesc);
		Format format = new Format(mimeType, encoding, schema);
		IParser parser = ParserFactory.getInstance().getParser(format, outputDataType);
//		if(parser == null) {
//			parser = ParserFactory.getInstance().getSimpleParser();
//		}
		IData collection = null;
		// encoding is UTF-8 (or nothing and we default to UTF-8)
		// everything that goes to this condition should be inline xml data
		if (!format.getEncoding().isPresent() || encoding.equalsIgnoreCase(IOHandler.DEFAULT_ENCODING)){
			try {
                InputStream stream = new ByteArrayInputStream(complexValue
                        .getBytes());
                collection = parser.parse(stream, format);
            } catch (IOException | RuntimeException e) {
                throw new NoApplicableCodeException("Error occured, while XML parsing").causedBy(e);
            }
		}
		else {
			throw new ExceptionReport("parser does not support operation: " + parser.getClass().getName(), ExceptionReport.INVALID_PARAMETER_VALUE);
		}
		return collection;
	}
	
    private static Class<?> determineOutputDataType(String outputID,
                                                    OutputDescriptionType output) {
        if (output.isSetLiteralOutput()) {
            String datatype = output.getLiteralOutput().getDataType()
                    .getStringValue();
            Class<? extends ILiteralData> t
                    = LiteralDataFactory.getBindingForType(datatype);
            if (t != null) {
                return t;
            }
		}
		if(output.isSetComplexOutput()){
			String mimeType = output.getComplexOutput().getDefault().getFormat().getMimeType();
			if(mimeType.contains("xml") || (mimeType.contains("XML"))){
				return GTVectorDataBinding.class;
			}else{
				return GTRasterDataBinding.class;
			}
		}
		
		throw new RuntimeException("Could not determie internal inputDataType");
	}

	protected static IData handleLiteralValue(OutputDataType output) throws ExceptionReport {
		
		String parameter = output.getData().getLiteralData().getStringValue();
		String xmlDataType = output.getData().getLiteralData().getDataType();
		IData parameterObj = null;
		try {
			parameterObj = LiteralDataFactory.create(xmlDataType, parameter);
		} catch(RuntimeException e) {
			throw new InvalidParameterValueException("The passed parameterValue: %s, but should be of type: %s", parameter, xmlDataType);
		}
		if(parameterObj == null) {
			throw new InvalidParameterValueException("XML datatype as LiteralParameter is not supported by the server: dataType %s",xmlDataType);
		}
		return parameterObj;
		
	}
	
	/**
	 * Handles BBoxValue
	 * @param input The client input
	 * @param class1 
	 */
	protected static IData handleBBoxValue(OutputDataType input) throws ExceptionReport{
		//String inputID = input.getIdentifier().getStringValue();
		throw new OperationNotSupportedException("BBox is not supported");
	}

}
