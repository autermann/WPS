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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import net.opengis.ows.x11.DomainMetadataType;
import net.opengis.wps.x100.ComplexDataType;
import net.opengis.wps.x100.InputDescriptionType;
import net.opengis.wps.x100.InputType;
import net.opengis.wps.x100.ProcessDescriptionType;

import org.apache.commons.io.FileUtils;
import org.n52.wps.commons.Format;
import org.n52.wps.commons.XMLUtil;
import org.n52.wps.io.IOHandler;
import org.n52.wps.io.IParser;
import org.n52.wps.io.LiteralDataFactory;
import org.n52.wps.io.ParserFactory;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.ILiteralData;
import org.n52.wps.io.data.binding.literal.AbstractLiteralDataBinding;
import org.n52.wps.io.datahandler.parser.GML2BasicParser;
import org.n52.wps.io.datahandler.parser.GML3BasicParser;
import org.n52.wps.io.datahandler.parser.SimpleGMLParser;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.InvalidParameterValueException;
import org.n52.wps.server.NoApplicableCodeException;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(InputHandler.class);
    private Map<String, List<IData>> inputData = new HashMap<>();
    private ProcessDescriptionType processDescription;
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
        this.processDescription = RepositoryManager.getInstance().getProcessDescription(algorithmIdentifier);

        if (processDescription == null) {
            throw new InvalidParameterValueException("Error while accessing the process description for %s", algorithmIdentifier);
        }

        Map<String, InterceptorInstance> inputInterceptors = resolveInputInterceptors(algorithmIdentifier);

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
                throw new InvalidParameterValueException("Error while accessing the inputValue: %s",inputId);
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
    InputDescriptionType getInputDescription(String inputId) throws ExceptionReport {
        for (InputDescriptionType tempDesc : this.processDescription.getDataInputs().getInputArray()) {
            if (inputId.equals(tempDesc.getIdentifier().getStringValue())) {
                return tempDesc;
            }
        }
        throw new NoApplicableCodeException("Input %s cannot be found in description for process %s", inputId, algorithmIdentifier);
    }

    protected String getComplexValueNodeString(Node complexValueNode) 
            throws ExceptionReport {
        try {
            String complexValue = XMLUtil.nodeToString(complexValueNode);
            int begin = complexValue.indexOf('>') + 1;
            int end = complexValue.lastIndexOf("</");
            return complexValue.substring(begin, end);
        } catch (TransformerFactoryConfigurationError | TransformerException e) {
            throw new NoApplicableCodeException("Could not parse inline data.").causedBy(e);
        }
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
        addInput(inputId, complexData);
    }

    private IParser getParser(Format format, String inputId)
            throws ExceptionReport {
        if (format == null) {
            throw new InvalidParameterValueException("Can not determine input format");
        }
        IParser parser = null;
        try {
            LOGGER.debug("Looking for matching Parser ... {}", format);
            Class<?> algorithmInput = RepositoryManager.getInstance()
                    .getInputDataTypeForAlgorithm(this.algorithmIdentifier, inputId);
            parser = ParserFactory.getInstance().getParser(format, algorithmInput);
        } catch (RuntimeException e) {
            throw new NoApplicableCodeException("Error obtaining input data").causedBy(e);
        }
        if (parser == null) {
            throw new NoApplicableCodeException("Error. No applicable parser found for %s", format);
        }
        return parser;
    }

    protected IData parseComplexValue(String complexValue, Format format,
                                      IParser parser) throws ExceptionReport {
        IData idata;
        String complexValueCopy = complexValue.toString();
        // encoding is UTF-8 (or nothing and we default to UTF-8)
        // everything that goes to this condition should be inline xml data
        if (!format.hasEncoding() || format.hasEncoding(IOHandler.DEFAULT_ENCODING)) {
            try {
                if (!complexValueCopy
                        .contains("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")) {
                    complexValueCopy = complexValueCopy
                            .replace("xsi:schemaLocation", "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation");
                }
                idata = parser.parse(new ByteArrayInputStream(complexValueCopy.getBytes()), format);
            } catch (IOException | RuntimeException e) {
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
        
        String uom = input.getData().getLiteralData().getUom();

        InputDescriptionType inputDesc = getInputDescription(inputID);
        String dataType = getDataType(input, inputDesc);

        ILiteralData parameterObj = null;
        try {
            parameterObj = LiteralDataFactory.create(dataType, parameter);
        } catch (ExceptionReport e) {
            throw e.locatedAt(parameter);
        }

        LiteralDataChecker checker = new LiteralDataChecker(inputDesc);
        if (!checker.apply(parameterObj)) {
            throw new InvalidParameterValueException("Input %s does not match %s", inputID, checker);
        }

        if (uom != null && !uom.isEmpty() &&
            parameterObj instanceof AbstractLiteralDataBinding) {
            ((AbstractLiteralDataBinding) parameterObj).setUnitOfMeasurement(uom);
        }
        addInput(inputID, parameterObj);
    }

    private String getDataType(InputType input, InputDescriptionType inputDesc) {
        String dataType = input.getData().getLiteralData().getDataType();
        if (dataType == null) {
            DomainMetadataType dataTypeDefinition = inputDesc.getLiteralData().getDataType();
            dataType = dataTypeDefinition != null ? dataTypeDefinition.getReference() : null;
        }
        //still null, assume string as default
        if (dataType == null) {
            dataType = LiteralDataFactory.XS_STRING;
        } else if (dataType.contains("http://www.w3.org/TR/xmlschema-2#")) {
            dataType = dataType.replace("http://www.w3.org/TR/xmlschema-2#", "xs:");
        }
        dataType = dataType.toLowerCase();
        return dataType;
    }

    /**
     * Handles the ComplexValueReference
     *
     * @param input The client input
     *
     * @throws ExceptionReport If the input (as url) is invalid, or there is an
     *                         error while parsing the XML.
     */
    private void handleComplexValueReference(InputType input) 
            throws ExceptionReport {
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
        try {
            addInput(inputID, parser.parse(stream, format));
        } catch (IOException ex) {
            throw new NoApplicableCodeException("Error parsing reference input %s", inputID).causedBy(ex);
        }
    }

    private void addInput(String inputID, IData parsedInputData) {
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
        BoundingBoxParser parser = new GeotoolsBoundingBoxParser();
        IData envelope = parser.parse(input.getData().getBoundingBoxData());
        addInput(input.getIdentifier().getStringValue(), envelope);
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
