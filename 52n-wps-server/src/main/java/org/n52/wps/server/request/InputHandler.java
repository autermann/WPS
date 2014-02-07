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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import net.opengis.ows.x11.DomainMetadataType;
import net.opengis.ows.x11.RangeType;
import net.opengis.ows.x11.ValueType;
import net.opengis.wps.x100.ComplexDataType;
import net.opengis.wps.x100.InputDescriptionType;
import net.opengis.wps.x100.InputType;
import net.opengis.wps.x100.ProcessDescriptionType;

import org.apache.commons.io.FileUtils;
import org.n52.wps.commons.Format;
import org.n52.wps.commons.XMLUtil;
import org.n52.wps.io.BasicXMLTypeFactory;
import org.n52.wps.io.IOHandler;
import org.n52.wps.io.IParser;
import org.n52.wps.io.ParserFactory;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.bbox.GTReferenceEnvelope;
import org.n52.wps.io.data.binding.literal.AbstractLiteralDataBinding;
import org.n52.wps.io.data.binding.literal.LiteralByteBinding;
import org.n52.wps.io.data.binding.literal.LiteralDoubleBinding;
import org.n52.wps.io.data.binding.literal.LiteralFloatBinding;
import org.n52.wps.io.data.binding.literal.LiteralIntBinding;
import org.n52.wps.io.data.binding.literal.LiteralLongBinding;
import org.n52.wps.io.data.binding.literal.LiteralShortBinding;
import org.n52.wps.io.datahandler.parser.GML2BasicParser;
import org.n52.wps.io.datahandler.parser.GML3BasicParser;
import org.n52.wps.io.datahandler.parser.SimpleGMLParser;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.RepositoryManager;
import org.n52.wps.server.handler.DataInputInterceptors;
import org.n52.wps.server.handler.DataInputInterceptors.DataInputInterceptorImplementations;
import org.n52.wps.server.handler.DataInputInterceptors.InterceptorInstance;
import org.n52.wps.server.request.strategy.ReferenceInputStream;
import org.n52.wps.server.request.strategy.ReferenceStrategyRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import com.google.common.annotations.VisibleForTesting;

/**
 * Handles the input of the client and stores it into a Map.
 */
public class InputHandler {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(InputHandler.class);
    private Map<String, List<IData>> inputData = new HashMap<>();
    private ProcessDescriptionType processDesc;
    private String algorithmIdentifier = null; // Needed to take care of handling a conflict between different parsers.

    /**
     * Initializes a parser that handles each (line of) input based on the type
     * of input.
     *
     * @see #handleComplexData(IOValueType)
     * @see #handleComplexValueReference(IOValueType)
     * @see #handleLiteralData(IOValueType)
     * @see #handleBBoxValue(IOValueType)
     * @param builder
     *
     * @throws ExceptionReport
     */
    private InputHandler(Builder builder) throws ExceptionReport {
        this.algorithmIdentifier = builder.algorithmIdentifier;
        this.processDesc = RepositoryManager.getInstance()
                .getProcessDescription(algorithmIdentifier);

        if (processDesc == null) {
            throw new ExceptionReport("Error while accessing the process description for " +
                                      algorithmIdentifier,
                                      ExceptionReport.INVALID_PARAMETER_VALUE);
        }

        Map<String, InterceptorInstance> inputInterceptors
                = resolveInputInterceptors(algorithmIdentifier);

        for (InputType input : builder.inputs) {
            String inputId = input.getIdentifier().getStringValue().trim();
            if (inputInterceptors.containsKey(inputId)) {
                InterceptorInstance interceptor = inputInterceptors.get(inputId);
                List<IData> result = interceptor.applyInterception(input);

                if (result != null && !result.isEmpty()) {
                    this.inputData.put(inputId, result);
                    continue;
                }
            }

            if (input.getData() != null) {
                if (input.getData().getComplexData() != null) {
                    handleComplexData(input, inputId);
                } else if (input.getData().getLiteralData() != null) {
                    handleLiteralData(input);
                } else if (input.getData().getBoundingBoxData() != null) {
                    handleBBoxValue(input);
                }
            } else if (input.getReference() != null) {
                handleComplexValueReference(input);
            } else {
                throw new ExceptionReport("Error while accessing the inputValue: " +
                                          inputId,
                                          ExceptionReport.INVALID_PARAMETER_VALUE);
            }
        }
    }

    Map<String, InterceptorInstance> resolveInputInterceptors(
            String algorithmClassName) {
        Map<String, InterceptorInstance> result = new HashMap<>();
        Class<?> clazz;

        try {
            //(by Matthias) This method causes exceptions for each R process because they are represented as
            //instances from GenericRProcess. Hence classes do not match algorithm-names in WPS4R.
            //The following is a quick workaround, please review:
            //------------------------------------
//			if(algorithmClassName.startsWith("org.n52.wps.server.r."))
//				return result;
            //------------------------------------
            //TODO (by Matthes) check if sufficient. Good point, the followin should work as well. If an exception is thrown
            //go on with the default way. This has the benefit that its not hardcoded and should work for
            //every algorithm which is created at runtime.

            clazz = Class.forName(algorithmClassName, false, getClass()
                    .getClassLoader());
        } catch (ClassNotFoundException e) {
            LOGGER.warn("Could not find class " + algorithmClassName, e);
            return result;
        }

        DataInputInterceptorImplementations annotation = clazz
                .getAnnotation(DataInputInterceptors.DataInputInterceptorImplementations.class);
        if (annotation != null) {
            Class<?> interceptorClazz;
            try {
                interceptorClazz = Class.forName(annotation.value());
            } catch (ClassNotFoundException e) {
                LOGGER.warn("Could not find class " + annotation.value(), e);
                return result;
            }

            if (DataInputInterceptors.class.isAssignableFrom(interceptorClazz)) {
                DataInputInterceptors instance;
                try {
                    instance = (DataInputInterceptors) interceptorClazz
                            .newInstance();
                } catch (InstantiationException e) {
                    LOGGER.warn("Could not instantiate class " +
                                interceptorClazz, e);
                    return result;
                } catch (IllegalAccessException e) {
                    LOGGER.warn("Could not access class " + interceptorClazz, e);
                    return result;
                }

                return instance.getInterceptors();
            }
        }
        return result;
    }

    @VisibleForTesting
    InputDescriptionType getInputDescription(String inputId) throws
            ExceptionReport {
        for (InputDescriptionType tempDesc : this.processDesc.getDataInputs()
                .getInputArray()) {
            if (inputId.equals(tempDesc.getIdentifier().getStringValue())) {
                return tempDesc;
            }
        }
        throw new ExceptionReport(
                "Input cannot be found in description for " + processDesc
                .getIdentifier().getStringValue() + "," + inputId,
                ExceptionReport.NO_APPLICABLE_CODE);
    }

    protected String getComplexValueNodeString(Node complexValueNode) {
        String complexValue;
        try {
            complexValue = XMLUtil.nodeToString(complexValueNode);
            complexValue = complexValue
                    .substring(complexValue.indexOf('>') + 1, complexValue
                            .lastIndexOf("</"));
        } catch (TransformerFactoryConfigurationError | TransformerException e1) {
            throw new TransformerFactoryConfigurationError("Could not parse inline data. Reason " +
                                                           e1);
        }
        return complexValue;
    }

    /**
     * Handles the complexValue, which in this case should always include XML
     * which can be parsed into a FeatureCollection.
     *
     * @param input   The client input
     * @param inputId
     *
     * @throws ExceptionReport If error occured while parsing XML
     */
    protected void handleComplexData(InputType input, String inputId)
            throws ExceptionReport {
        ComplexDataType data = input.getData().getComplexData();
        Node complexValueNode = input.getData().getComplexData().getDomNode();
        String complexValue = getComplexValueNodeString(complexValueNode);
        InputDescriptionType idt = getInputDescription(inputId);
        Format dataFormat = Format.of(data);
        FormatHandler formatHandler = new FormatHandler(idt);
        Format format = formatHandler.select(dataFormat);
        IParser parser = getParser(format, inputId);
        IData complexData = parseComplexValue(complexValue, format, parser);

        //enable maxoccurs of parameters with the same name.
        List<IData> list = inputData.get(inputId);
        if (list == null) {
            inputData.put(inputId, list = new ArrayList<>());
        }
        list.add(complexData);
    }

    private IParser getParser(Format format, String inputId)
            throws ExceptionReport {
        if (format == null) {
            throw new ExceptionReport("Can not determine input format",
                                      ExceptionReport.INVALID_PARAMETER_VALUE);
        }
        IParser parser = null;
        try {
            LOGGER.debug("Looking for matching Parser ... {}", format);

            Class<?> algorithmInput = RepositoryManager.getInstance()
                    .getInputDataTypeForAlgorithm(this.algorithmIdentifier, inputId);

            parser = ParserFactory.getInstance()
                    .getParser(format, algorithmInput);
        } catch (RuntimeException e) {
            throw new ExceptionReport("Error obtaining input data",
                                      ExceptionReport.NO_APPLICABLE_CODE, e);
        }
        if (parser == null) {
            throw new ExceptionReport("Error. No applicable parser found for " +
                                      format, ExceptionReport.NO_APPLICABLE_CODE);
        }
        return parser;
    }

    protected IData parseComplexValue(String complexValue, Format format,
                                      IParser parser) throws ExceptionReport {
        IData idata;
        String complexValueCopy = complexValue.toString();
        // encoding is UTF-8 (or nothing and we default to UTF-8)
        // everything that goes to this condition should be inline xml data
        if (!format.hasEncoding() || format
                .hasEncoding(IOHandler.DEFAULT_ENCODING)) {
            try {
                if (!complexValueCopy
                        .contains("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")) {
                    complexValueCopy = complexValueCopy
                            .replace("xsi:schemaLocation", "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation");
                }
                idata = parser.parse(new ByteArrayInputStream(complexValueCopy
                        .getBytes()), format);
            } catch (RuntimeException e) {
                throw new ExceptionReport("Error occured, while XML parsing", ExceptionReport.NO_APPLICABLE_CODE, e);
            }
        } else if (format.hasEncoding(IOHandler.ENCODING_BASE64)) {
            // in case encoding is base64
            // everything that goes to this condition should be inline base64 data
            idata = getBase64EncodedData(complexValue, parser, format);
        } else {
            throw new ExceptionReport("Unable to generate encoding " +
                                      format, ExceptionReport.NO_APPLICABLE_CODE);
        }
        return idata;
    }

    //TODO-- Needs testing
    protected IData getBase64EncodedData(String complexValue, IParser parser,
                                         Format format) throws ExceptionReport {
        File f = null;
        String complexValueCopy = complexValue.toString();

        try {
            f = File.createTempFile("wps" + UUID.randomUUID(), "tmp");

            if (complexValueCopy.startsWith("<xml-fragment")) {
                int startIndex = complexValueCopy.indexOf('>');
                complexValueCopy = complexValueCopy.substring(startIndex + 1);

                int endIndex = complexValueCopy.indexOf("</xml-fragment");
                complexValueCopy = complexValueCopy.substring(0, endIndex);
            }

            FileUtils.write(f, complexValueCopy);

            return parser.parseBase64(new FileInputStream(f), format);

        } catch (IOException e) {
            throw new ExceptionReport("Error occured, while Base64 extracting", ExceptionReport.NO_APPLICABLE_CODE, e);
        } finally {
            FileUtils.deleteQuietly(f);
            System.gc();
        }
    }

    /**
     * Handles the literalData
     *
     * @param input The client's input
     *
     * @throws ExceptionReport If the type of the parameter is invalid.
     */
    private void handleLiteralData(InputType input) throws ExceptionReport {
        String inputID = input.getIdentifier().getStringValue();
        String parameter = input.getData().getLiteralData().getStringValue();
        String xmlDataType = input.getData().getLiteralData().getDataType();
        String uom = input.getData().getLiteralData().getUom();

        InputDescriptionType inputDesc = getInputDescription(inputID);

        if (xmlDataType == null) {
            DomainMetadataType dataType = inputDesc.getLiteralData()
                    .getDataType();
            xmlDataType = dataType != null ? dataType.getReference() : null;
        }
        //still null, assume string as default
        if (xmlDataType == null) {
            xmlDataType = BasicXMLTypeFactory.STRING_URI;
        } else if (xmlDataType.contains("http://www.w3.org/TR/xmlschema-2#")) {
            xmlDataType = xmlDataType
                    .replace("http://www.w3.org/TR/xmlschema-2#", "xs:");
        }
        xmlDataType = xmlDataType.toLowerCase();

        IData parameterObj = null;
        try {
            parameterObj = BasicXMLTypeFactory
                    .getBasicJavaObject(xmlDataType, parameter);
        } catch (RuntimeException e) {
            throw new ExceptionReport("The passed parameterValue: " + parameter +
                                      ", but should be of type: " + xmlDataType, ExceptionReport.INVALID_PARAMETER_VALUE);
        }

        //validate allowed values.
        if (inputDesc.getLiteralData().isSetAllowedValues()) {
            if ((!inputDesc.getLiteralData().isSetAnyValue())) {
                ValueType[] allowedValues = inputDesc.getLiteralData()
                        .getAllowedValues().getValueArray();
                boolean foundAllowedValue = false;
                for (ValueType allowedValue : allowedValues) {
                    if (input.getData().getLiteralData().getStringValue()
                            .equals(allowedValue.getStringValue())) {
                        foundAllowedValue = true;

                    }
                }
                RangeType[] allowedRanges = {};
                if (parameterObj instanceof LiteralIntBinding ||
                    parameterObj instanceof LiteralDoubleBinding ||
                    parameterObj instanceof LiteralShortBinding ||
                    parameterObj instanceof LiteralFloatBinding ||
                    parameterObj instanceof LiteralLongBinding ||
                    parameterObj instanceof LiteralByteBinding) {

                    allowedRanges = inputDesc.getLiteralData()
                            .getAllowedValues().getRangeArray();
                    for (RangeType allowedRange : allowedRanges) {
                        foundAllowedValue
                                = checkRange(parameterObj, allowedRange);
                    }
                }

                if (!foundAllowedValue && (allowedValues.length != 0 ||
                                           allowedRanges.length != 0)) {
                    throw new ExceptionReport("Input with ID " + inputID +
                                              " does not contain an allowed value. See ProcessDescription.", ExceptionReport.INVALID_PARAMETER_VALUE);
                }

            }
        }

        if (parameterObj == null) {
            throw new ExceptionReport("XML datatype as LiteralParameter is not supported by the server: dataType " +
                                      xmlDataType, ExceptionReport.INVALID_PARAMETER_VALUE);
        }

        if (uom != null && !uom.isEmpty() &&
            parameterObj instanceof AbstractLiteralDataBinding) {
            ((AbstractLiteralDataBinding) parameterObj)
                    .setUnitOfMeasurement(uom);
        }

        //enable maxxoccurs of parameters with the same name.
        List<IData> list = inputData.get(inputID);
        if (list == null) {
            inputData.put(inputID, list = new ArrayList<>());
        }
        list.add(parameterObj);

    }

    @SuppressWarnings("unchecked")
    private boolean checkRange(IData parameterObj, RangeType allowedRange) {

        List<?> l = allowedRange.getRangeClosure();

        if (l != null && !l.isEmpty() && !l.get(0).equals("closed")) {
            return false;
        }
        String minString = allowedRange.getMinimumValue().getStringValue();
        String maxString = allowedRange.getMaximumValue().getStringValue();
        Object payload = parameterObj.getPayload();

        if (payload instanceof Integer) {
            int value = (Integer) payload;
            int min = Integer.parseInt(minString);
            int max = Integer.parseInt(maxString);
            return value >= min && value <= max;
        } else if (payload instanceof Double) {
            double value = (Double) payload;
            double min = Double.parseDouble(minString);
            double max = Double.parseDouble(maxString);
            return value >= min && value <= max;
        } else if (payload instanceof Short) {
            short value = (Short) payload;
            short min = Short.parseShort(minString);
            short max = Short.parseShort(maxString);
            return value >= min && value <= max;
        } else if (payload instanceof Float) {
            float value = (Float) payload;
            float min = Float.parseFloat(minString);
            float max = Float.parseFloat(maxString);
            return value >= min && value <= max;
        } else if (payload instanceof Long) {
            long value = (Long) payload;
            long min = Long.parseLong(minString);
            long max = Long.parseLong(maxString);
            return value >= min && value <= max;
        } else if (payload instanceof Byte) {
            byte value = (Byte) payload;
            byte min = Byte.parseByte(minString);
            byte max = Byte.parseByte(maxString);
            return value >= min && value <= max;
        } else if (payload instanceof BigInteger) {
            BigInteger value = (BigInteger) payload;
            BigInteger min = new BigInteger(minString);
            BigInteger max = new BigInteger(maxString);
            return value.compareTo(min) >= 0 &&
                   value.compareTo(max) <= 0;
        } else if (payload instanceof BigDecimal) {
            BigDecimal value = (BigDecimal) payload;
            BigDecimal min = new BigDecimal(minString);
            BigDecimal max = new BigDecimal(maxString);
            return value.compareTo(min) >= 0 &&
                   value.compareTo(max) <= 0;
        }
        return false;
    }

    /**
     * Handles the ComplexValueReference
     *
     * @param input The client input
     *
     * @throws ExceptionReport If the input (as url) is invalid, or there is an
     *                         error while parsing the XML.
     */
    private void handleComplexValueReference(InputType input) throws
            ExceptionReport {
        String inputID = input.getIdentifier().getStringValue();

        ReferenceStrategyRegister register = ReferenceStrategyRegister
                .getInstance();
        ReferenceInputStream stream = register.resolveReference(input);

        String dataURLString = input.getReference().getHref();
        //dataURLString = URLDecoder.decode(dataURLString);
        //dataURLString = dataURLString.replace("&amp;", "");
        LOGGER.debug("Loading data from: " + dataURLString);

        InputDescriptionType inputPD = getInputDescription(inputID);
        Format dataFormat = Format.of(input.getReference());
        FormatHandler formatHandler = new FormatHandler(inputPD);
        Format format = formatHandler.select(dataFormat);
        IParser parser = getParser(format, inputID);

        /**
         * **PROXY****
         */
        /*String decodedURL = URLDecoder.decode(dataURLString);
         decodedURL = decodedURL.replace("&amp;", "&");
         if(decodedURL.indexOf("&BBOX")==-1){
         decodedURL = decodedURL.replace("BBOX", "&BBOX");
         decodedURL = decodedURL.replace("outputFormat", "&outputFormat");
         decodedURL = decodedURL.replace("SRS", "&SRS");
         decodedURL = decodedURL.replace("REQUEST", "&REQUEST");
         decodedURL = decodedURL.replace("VERSION", "&VERSION");
         decodedURL = decodedURL.replace("SERVICE", "&SERVICE");
         decodedURL = decodedURL.replace("format", "&format");
         }*/
        //lookup WFS
        if (dataURLString.toUpperCase().contains("REQUEST=GETFEATURE") &&
            dataURLString.toUpperCase().contains("SERVICE=WFS")) {
            if (parser instanceof SimpleGMLParser) {
                parser = new GML2BasicParser();
            }
            if (parser instanceof GML2BasicParser && !dataURLString
                    .toUpperCase().contains("OUTPUTFORMAT=GML2")) {
                //make sure we get GML2
                dataURLString += "&outputFormat=GML2";
            }
            if (parser instanceof GML3BasicParser && !dataURLString
                    .toUpperCase().contains("OUTPUTFORMAT=GML3")) {
                //make sure we get GML3
                dataURLString += "&outputFormat=GML3";
            }
        }
        IData parsedInputData = parser.parse(stream, format);

        //enable maxxoccurs of parameters with the same name.
        List<IData> list = inputData.get(inputID);
        if (list == null) {
            inputData.put(inputID, list = new ArrayList<>());
        }
        list.add(parsedInputData);
    }

    /**
     * Handles BBoxValue
     *
     * @param input The client input
     */
    private void handleBBoxValue(InputType input) throws ExceptionReport {
        String crs = input.getData().getBoundingBoxData().getCrs();
        List<?> lowerCorner = input.getData().getBoundingBoxData()
                .getLowerCorner();
        List<?> upperCorner = input.getData().getBoundingBoxData()
                .getUpperCorner();

        if (lowerCorner.size() != 2 || upperCorner.size() != 2) {
            throw new ExceptionReport("Error while parsing the BBOX data", ExceptionReport.INVALID_PARAMETER_VALUE);
        }
        IData envelope = new GTReferenceEnvelope(
                lowerCorner.get(0), lowerCorner
                .get(1), upperCorner.get(0), upperCorner.get(1), crs);

        List<IData> resultList = new ArrayList<>();
        resultList.add(envelope);
        inputData.put(input.getIdentifier().getStringValue(), resultList);
    }

    /**
     * Gets the resulting InputLayers from the parser
     *
     * @return A map with the parsed input
     */
    public Map<String, List<IData>> getParsedInputData() {
        return inputData;
    }

//	private InputStream retrievingZippedContent(URLConnection conn) throws IOException{
//		String contentType = conn.getContentEncoding();
//		if(contentType != null && contentType.equals("gzip")) {
//			return new GZIPInputStream(conn.getInputStream());
//		}
//		else{
//			return conn.getInputStream();
//		}
//	}
    public static class Builder {
        protected InputType[] inputs;
        protected String algorithmIdentifier = null;

        public Builder(InputType[] inputs, String algorithmIdentifier) {
            this.inputs = inputs;
            this.algorithmIdentifier = algorithmIdentifier;
        }

        public Builder inputs(InputType[] val) {
            this.inputs = val;
            return this;
        }

        public Builder algorithmIdentifier(String val) {
            this.algorithmIdentifier = val;
            return this;
        }

        public InputHandler build() throws ExceptionReport {
            return new InputHandler(this);
        }
    }
}
