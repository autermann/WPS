/**
 * Copyright (C) 2010 - 2014 52°North Initiative for Geospatial Open Source
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
package org.n52.wps.server.r.metadata;

import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.opengis.ows.x11.DomainMetadataType;
import net.opengis.ows.x11.MetadataType;
import net.opengis.wps.x100.ComplexDataCombinationsType;
import net.opengis.wps.x100.ComplexDataDescriptionType;
import net.opengis.wps.x100.InputDescriptionType;
import net.opengis.wps.x100.LiteralInputType;
import net.opengis.wps.x100.LiteralOutputType;
import net.opengis.wps.x100.OutputDescriptionType;
import net.opengis.wps.x100.ProcessDescriptionType;
import net.opengis.wps.x100.ProcessDescriptionType.DataInputs;
import net.opengis.wps.x100.ProcessDescriptionType.ProcessOutputs;
import net.opengis.wps.x100.SupportedComplexDataType;

import org.n52.wps.commons.Format;
import org.n52.wps.io.GeneratorFactory;
import org.n52.wps.io.IGenerator;
import org.n52.wps.io.IOHandler;
import org.n52.wps.io.IParser;
import org.n52.wps.io.ParserFactory;
import org.n52.wps.io.data.GenericFileDataConstants;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GenericFileDataBinding;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.r.R_Config;
import org.n52.wps.server.r.data.R_Resource;
import org.n52.wps.server.r.syntax.RAnnotation;
import org.n52.wps.server.r.syntax.RAnnotationException;
import org.n52.wps.server.r.syntax.RAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RProcessDescriptionCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(RProcessDescriptionCreator.class);

    /**
     * Usually called from GenericRProcess (extends AbstractObservableAlgorithm)
     * 
     * @param annotations
     *            contain all process description information
     * @param identifier
     *            Process identifier
     * @param fileUrl
     * @return
     * @throws ExceptionReport
     * @throws RAnnotationException
     */
    public ProcessDescriptionType createDescribeProcessType(List<RAnnotation> annotations,
            String identifier,
            URL fileUrl,
            URL sessionInfoUrl) throws ExceptionReport, RAnnotationException
    {
        LOGGER.debug("Creating Process Description for " + identifier);

        try {
            ProcessDescriptionType pdt = ProcessDescriptionType.Factory.newInstance();
            pdt.setStatusSupported(true);
            pdt.setStoreSupported(true);

            MetadataType mt = pdt.addNewMetadata();
            // note that the "about" argument for ows:Metadata has type anyURI
            // so it cannot
            // be used for textual description of the ows:metadata element

            // The "xlin:type"-argument, i.e. mt.setType(TypeType.RESOURCE); was
            // not used for the resources
            // because validation fails with the cause:
            // "cvc-complex-type.3.1: Value 'resource' of attribute 'xlin:type'
            // of element 'ows:Metadata' is not valid
            // with respect to the corresponding attribute use. Attribute
            // 'xlin:type' has a fixed value of 'simple'."
            mt.setTitle("R Script");
            mt.setHref(fileUrl.toExternalForm());

            // Add URL to resource folder > FIXME rathre add resources one by
            // one, see below.
            // mt = pdt.addNewMetadata();
            // mt.setTitle("Resource Directory URL");
            // url = R_Config.getInstance().getResourceDirURL();
            // mt.setHref(url);

            mt = pdt.addNewMetadata();
            mt.setTitle("R Session Info");
            mt.setHref(sessionInfoUrl.toExternalForm());

            ProcessOutputs outputs = pdt.addNewProcessOutputs();
            DataInputs inputs = pdt.addNewDataInputs();

            // iterates over annotations,
            // The annotation type (RAnnotationType - enumeration) determines
            // next method call
            for (RAnnotation annotation : annotations) {
                switch (annotation.getType()) {
                case INPUT:
                    addInput(inputs, annotation);
                    break;
                case OUTPUT:
                    addOutput(outputs, annotation);
                    break;
                case DESCRIPTION:
                    addProcessDescription(pdt, annotation);
                    break;
                case RESOURCE:
                    addProcessResources(pdt, annotation);
                    break;
                default:
                    break;
                }
            }

            // Add SessionInfo-Output
            OutputDescriptionType outdes = outputs.addNewOutput();
            outdes.addNewIdentifier().setStringValue("sessionInfo");
            outdes.addNewTitle().setStringValue("Information about the R session which has been used");
            outdes.addNewAbstract().setStringValue("Output of the sessionInfo()-method after R-script execution");

            SupportedComplexDataType scdt = outdes.addNewComplexOutput();
            ComplexDataDescriptionType datatype = scdt.addNewDefault().addNewFormat();
            datatype.setMimeType(GenericFileDataConstants.MIME_TYPE_PLAIN_TEXT);
            datatype.setEncoding(IOHandler.DEFAULT_ENCODING);
            datatype = scdt.addNewSupported().addNewFormat();
            datatype.setMimeType(GenericFileDataConstants.MIME_TYPE_PLAIN_TEXT);
            datatype.setEncoding(IOHandler.DEFAULT_ENCODING);

            // Add Warnings-Output
            outdes = outputs.addNewOutput();
            outdes.addNewIdentifier().setStringValue("warnings");
            outdes.addNewTitle().setStringValue("Warnings from R");
            outdes.addNewAbstract().setStringValue("Output of the warnings()-method after R-script execution");

            scdt = outdes.addNewComplexOutput();
            datatype = scdt.addNewDefault().addNewFormat();
            datatype.setMimeType(GenericFileDataConstants.MIME_TYPE_PLAIN_TEXT);
            datatype.setEncoding(IOHandler.DEFAULT_ENCODING);
            datatype = scdt.addNewSupported().addNewFormat();
            datatype.setMimeType(GenericFileDataConstants.MIME_TYPE_PLAIN_TEXT);
            datatype.setEncoding(IOHandler.DEFAULT_ENCODING);

            return pdt;
        } catch (Exception e) {
            LOGGER.error("Error creating process description.", e);
            throw new ExceptionReport("Error creating process description.", "NA", RProcessDescriptionCreator.class.getName(), e);
        }
    }

    private void addProcessResources(ProcessDescriptionType pdt,
            RAnnotation annotation)
    {
        // Add URL to resource folder > FIXME rather add resources one by one,
        // see below.

        try {
            Object obj = annotation.getObjectValue(RAttribute.NAMED_LIST);
            if (obj instanceof List<?>) {
                List<?> namedList = (List<?>) obj;
                for (Object object : namedList) {
                    R_Resource resource = null;
                    if (object instanceof R_Resource) {
                        resource = (R_Resource) object;
                    } else {
                        continue;
                    }
                    MetadataType mt = pdt.addNewMetadata();
                    mt.setTitle("Resource: " + resource.getResourceValue());

                    URL url = resource.getFullResourceURL();
                    mt.setHref(url.toExternalForm());
                }
            }
        } catch (RAnnotationException e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * @param pdt
     * @param annotation
     * @throws RAnnotationException
     */
    private static void addProcessDescription(ProcessDescriptionType pdt,
            RAnnotation annotation) throws RAnnotationException
    {
        String id = R_Config.WKN_PREFIX + annotation.getStringValue(RAttribute.IDENTIFIER);
        pdt.addNewIdentifier().setStringValue(id);

        String abstr = annotation.getStringValue(RAttribute.ABSTRACT);
        pdt.addNewAbstract().setStringValue(abstr);

        String title = annotation.getStringValue(RAttribute.TITLE);
        pdt.addNewTitle().setStringValue(title);

        pdt.setProcessVersion(annotation.getStringValue(RAttribute.VERSION));
    }

    private static void addInput(DataInputs inputs,
            RAnnotation annotation) throws RAnnotationException
    {
        InputDescriptionType input = inputs.addNewInput();

        String identifier = annotation.getStringValue(RAttribute.IDENTIFIER);
        input.addNewIdentifier().setStringValue(identifier);

        // title is optional, therefore it could be null
        String title = annotation.getStringValue(RAttribute.TITLE);
        if (title != null) {
            input.addNewTitle().setStringValue(title);
        }

        String abstr = annotation.getStringValue(RAttribute.ABSTRACT);
        // abstract is optional, therefore it could be null
        if (abstr != null) {
            input.addNewAbstract().setStringValue(abstr);
        }

        String min = annotation.getStringValue(RAttribute.MIN_OCCURS);
        BigInteger minOccurs = BigInteger.valueOf(Long.parseLong(min));
        input.setMinOccurs(minOccurs);

        String max = annotation.getStringValue(RAttribute.MAX_OCCURS);
        BigInteger maxOccurs = BigInteger.valueOf(Long.parseLong(max));
        input.setMaxOccurs(maxOccurs);

        if (annotation.isComplex()) {
            addComplexInput(annotation, input);
        } else {
            addLiteralInput(annotation, input);

        }
    }

    /**
     * @param annotation
     * @param input
     * @throws RAnnotationException
     */
    private static void addLiteralInput(RAnnotation annotation,
            InputDescriptionType input) throws RAnnotationException
    {
        LiteralInputType literalInput = input.addNewLiteralData();
        DomainMetadataType dataType = literalInput.addNewDataType();
        dataType.setReference(annotation.getProcessDescriptionType());
        literalInput.setDataType(dataType);
        literalInput.addNewAnyValue();
        String def = annotation.getStringValue(RAttribute.DEFAULT_VALUE);
        if (def != null) {
            literalInput.setDefaultValue(def);
        }
    }

    /**
     * @param annotation
     * @param input
     * @throws RAnnotationException
     */
    private static void addComplexInput(RAnnotation annotation,
            InputDescriptionType input) throws RAnnotationException
    {
        SupportedComplexDataType complexInput = input.addNewComplexData();
        ComplexDataDescriptionType cpldata = complexInput.addNewDefault().addNewFormat();
        cpldata.setMimeType(annotation.getProcessDescriptionType());
        String encod = annotation.getStringValue(RAttribute.ENCODING);
        if (encod != null && !"base64".equals(encod)) {
            cpldata.setEncoding(encod);
        }

        Class<? extends IData> iClass = annotation.getDataClass();
        if (iClass.equals(GenericFileDataBinding.class)) {
            ComplexDataCombinationsType supported = complexInput.addNewSupported();
            ComplexDataDescriptionType format = supported.addNewFormat();
            format.setMimeType(annotation.getProcessDescriptionType());
            encod = annotation.getStringValue(RAttribute.ENCODING);
            if (encod != null) {
                format.setEncoding(encod);
            }
            if ("base64".equals(encod)) {
                // set a format entry such that not encoded data is supported as
                // well
                ComplexDataDescriptionType format2 = supported.addNewFormat();
                format2.setMimeType(annotation.getProcessDescriptionType());
            }
        } else {
            addSupportedInputFormats(complexInput, iClass);
        }
    }

    private static void addOutput(ProcessOutputs outputs,
            RAnnotation out) throws RAnnotationException
    {
        OutputDescriptionType output = outputs.addNewOutput();

        String identifier = out.getStringValue(RAttribute.IDENTIFIER);
        output.addNewIdentifier().setStringValue(identifier);

        // title is optional, therefore it could be null
        String title = out.getStringValue(RAttribute.TITLE);
        if (title != null) {
            output.addNewTitle().setStringValue(title);
        }

        // is optional, therefore it could be null
        String abstr = out.getStringValue(RAttribute.ABSTRACT);
        if (abstr != null) {
            output.addNewAbstract().setStringValue(abstr);
        }

        if (out.isComplex()) {
            addComplexOutput(out, output);
        } else {
            addLiteralOutput(out, output);
        }
    }

    /**
     * @param out
     * @param output
     * @throws RAnnotationException
     */
    private static void addLiteralOutput(RAnnotation out,
            OutputDescriptionType output) throws RAnnotationException
    {
        LiteralOutputType literalOutput = output.addNewLiteralOutput();
        DomainMetadataType dataType = literalOutput.addNewDataType();
        dataType.setReference(out.getProcessDescriptionType());
        literalOutput.setDataType(dataType);
    }

    /**
     * @param out
     * @param output
     * @throws RAnnotationException
     */
    private static void addComplexOutput(RAnnotation out,
            OutputDescriptionType output) throws RAnnotationException
    {
        SupportedComplexDataType complexOutput = output.addNewComplexOutput();
        ComplexDataDescriptionType complexData = complexOutput.addNewDefault().addNewFormat();
        complexData.setMimeType(out.getProcessDescriptionType());

        String encod = out.getStringValue(RAttribute.ENCODING);
        if (encod != null && !"base64".equals(encod)) {
            // base64 shall not be default, but occur in the supported formats
            complexData.setEncoding(encod);
        }
        Class<? extends IData> iClass = out.getDataClass();

        if (iClass.equals(GenericFileDataBinding.class)) {

            ComplexDataCombinationsType supported = complexOutput.addNewSupported();
            ComplexDataDescriptionType format = supported.addNewFormat();
            format.setMimeType(out.getProcessDescriptionType());
            encod = out.getStringValue(RAttribute.ENCODING);

            if (encod != null) {
                format.setEncoding(encod);
                if ("base64".equals(encod)) {
                    // set a format entry such that not encoded data is
                    // supported as well
                    ComplexDataDescriptionType format2 = supported.addNewFormat();
                    format2.setMimeType(out.getProcessDescriptionType());
                }
            }
        } else {
            addSupportedOutputFormats(complexOutput, iClass);
        }

    }

    /**
     * Searches all available datahandlers for supported encodings / schemas /
     * mime-types and adds them to the supported list of an output
     * 
     * @param complex
     *            IData class for which data handlers are searched
     * @param supportedClass
     */
    private static void addSupportedOutputFormats(SupportedComplexDataType complex,
            Class<? extends IData> supportedClass)
    {
        // retrieve a list of generators which support the supportedClass-input
        List<IGenerator> generators = GeneratorFactory.getInstance().getAllGenerators();
        List<IGenerator> foundGenerators = new ArrayList<>();
        for (IGenerator generator : generators) {
            if (generator.isSupportedDataBinding(supportedClass)) {
                foundGenerators.add(generator);
            }
        }

        ComplexDataCombinationsType supported = complex.addNewSupported();
        for (IGenerator generator : foundGenerators) {

            for (Format format : generator.getSupportedFormats()) {
                ComplexDataDescriptionType newSupportedFormat = supported.addNewFormat();
                newSupportedFormat.setEncoding(format.getEncoding().or(IOHandler.DEFAULT_ENCODING));
                newSupportedFormat.setMimeType(format.getMimeType().get());
                if (format.hasSchema()) {
                    newSupportedFormat.setSchema(format.getSchema().get());
                }
            }

        }

    }

    /**
     * Searches all available datahandlers for supported encodings / schemas /
     * mime-types and adds them to the supported list of an output
     * 
     * @param complex
     *            IData class for which data handlers are searched
     * @param supportedClass
     */
    private static void addSupportedInputFormats(SupportedComplexDataType complex,
            Class<? extends IData> supportedClass)
    {
        // retrieve a list of parsers which support the supportedClass-input
        List<IParser> parsers = ParserFactory.getInstance().getAllParsers();
        List<IParser> foundParsers = new LinkedList<>();
        for (IParser parser : parsers) {
            if (parser.isSupportedDataBinding(supportedClass)) {
               foundParsers.add(parser);
            }
        }

        // add properties for each parser which is found
        ComplexDataCombinationsType supported = complex.addNewSupported();
        for (IParser parser : foundParsers) {
            for (Format format : parser.getSupportedFormats()) {
                ComplexDataDescriptionType newSupportedFormat = supported.addNewFormat();
                newSupportedFormat.setEncoding(format.getEncoding().or(IOHandler.DEFAULT_ENCODING));
                newSupportedFormat.setMimeType(format.getMimeType().get());
                if (format.hasSchema()) {
                    newSupportedFormat.setSchema(format.getSchema().get());
                }
            }

        }

    }

}