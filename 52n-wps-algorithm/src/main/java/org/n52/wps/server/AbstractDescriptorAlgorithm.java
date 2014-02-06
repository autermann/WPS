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
package org.n52.wps.server;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.opengis.ows.x11.AllowedValuesDocument.AllowedValues;
import net.opengis.wps.x100.ComplexDataCombinationsType;
import net.opengis.wps.x100.InputDescriptionType;
import net.opengis.wps.x100.LiteralInputType;
import net.opengis.wps.x100.OutputDescriptionType;
import net.opengis.wps.x100.ProcessDescriptionType;
import net.opengis.wps.x100.ProcessDescriptionType.DataInputs;
import net.opengis.wps.x100.ProcessDescriptionType.ProcessOutputs;
import net.opengis.wps.x100.ProcessDescriptionsDocument;
import net.opengis.wps.x100.ProcessDescriptionsDocument.ProcessDescriptions;
import net.opengis.wps.x100.SupportedComplexDataInputType;
import net.opengis.wps.x100.SupportedComplexDataType;

import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlValidationError;
import org.n52.wps.algorithm.descriptor.AlgorithmDescriptor;
import org.n52.wps.algorithm.descriptor.ComplexDataInputDescriptor;
import org.n52.wps.algorithm.descriptor.ComplexDataOutputDescriptor;
import org.n52.wps.algorithm.descriptor.InputDescriptor;
import org.n52.wps.algorithm.descriptor.LiteralDataInputDescriptor;
import org.n52.wps.algorithm.descriptor.LiteralDataOutputDescriptor;
import org.n52.wps.algorithm.descriptor.OutputDescriptor;
import org.n52.wps.commons.Format;
import org.n52.wps.io.GeneratorFactory;
import org.n52.wps.io.IGenerator;
import org.n52.wps.io.IOHandler;
import org.n52.wps.io.IParser;
import org.n52.wps.io.ParserFactory;
import org.n52.wps.io.data.IData;
import org.n52.wps.server.observerpattern.IObserver;
import org.n52.wps.server.observerpattern.ISubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDescriptorAlgorithm implements IAlgorithm, ISubject {

    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractDescriptorAlgorithm.class);

    private final List<IObserver> observers = new LinkedList<>();
    private final List<String> errorList = new LinkedList<>();
    private Object state = null;

    private AlgorithmDescriptor descriptor;
    private ProcessDescriptionType description;
    
    public AbstractDescriptorAlgorithm() {
        super();
    }

    @Override
    public synchronized ProcessDescriptionType getDescription() {
        if (description == null) {
            description = createProcessDescription();
        }
        return description;
    }

    @Override
    public String getWellKnownName() {
        return getAlgorithmDescriptor().getIdentifier();
    }

    private ProcessDescriptionType createProcessDescription() {

        AlgorithmDescriptor algorithmDescriptor = getAlgorithmDescriptor();

        ProcessDescriptionsDocument document = ProcessDescriptionsDocument.Factory.newInstance();
        ProcessDescriptions processDescriptions = document.addNewProcessDescriptions();
        ProcessDescriptionType processDescription = processDescriptions.addNewProcessDescription();

        if (algorithmDescriptor == null) {
            throw new IllegalStateException("Instance must have an algorithm descriptor");
        } else {

            // 1. Identifier
            processDescription.setStatusSupported(algorithmDescriptor.getStatusSupported());
            processDescription.setStoreSupported(algorithmDescriptor.getStoreSupported());
            processDescription.setProcessVersion(algorithmDescriptor.getVersion());
            processDescription.addNewIdentifier().setStringValue(algorithmDescriptor.getIdentifier());
            processDescription.addNewTitle().setStringValue( algorithmDescriptor.hasTitle() ?
                    algorithmDescriptor.getTitle() :
                    algorithmDescriptor.getIdentifier());
            if (algorithmDescriptor.hasAbstract()) {
                processDescription.addNewAbstract().setStringValue(algorithmDescriptor.getAbstract());
            }

            // 2. Inputs
            Collection<InputDescriptor> inputDescriptors = algorithmDescriptor.getInputDescriptors();
            DataInputs dataInputs = null;
            if (inputDescriptors.size() > 0) {
                dataInputs = processDescription.addNewDataInputs();
                for (InputDescriptor inputDescriptor : inputDescriptors) {
                    InputDescriptionType dataInput = dataInputs.addNewInput();
                    dataInput.setMinOccurs(inputDescriptor.getMinOccurs());
                    dataInput.setMaxOccurs(inputDescriptor.getMaxOccurs());

                    dataInput.addNewIdentifier().setStringValue(inputDescriptor.getIdentifier());
                    dataInput.addNewTitle().setStringValue( inputDescriptor.hasTitle() ?
                            inputDescriptor.getTitle() :
                            inputDescriptor.getIdentifier());
                    if (inputDescriptor.hasAbstract()) {
                        dataInput.addNewAbstract().setStringValue(inputDescriptor.getAbstract());
                    }

                    if (inputDescriptor instanceof LiteralDataInputDescriptor) {
                        LiteralDataInputDescriptor literalDescriptor = (LiteralDataInputDescriptor)inputDescriptor;

                        LiteralInputType literalData = dataInput.addNewLiteralData();
                        literalData.addNewDataType().setReference(literalDescriptor.getDataType());

                        if (literalDescriptor.hasDefaultValue()) {
                            literalData.setDefaultValue(literalDescriptor.getDefaultValue());
                        }
                        if (literalDescriptor.hasAllowedValues()) {
                            AllowedValues allowed = literalData.addNewAllowedValues();
                            for (String allowedValue : literalDescriptor.getAllowedValues()) {
                                allowed.addNewValue().setStringValue(allowedValue);
                            }
                        } else {
                            literalData.addNewAnyValue();
                        }

                    } else if (inputDescriptor instanceof ComplexDataInputDescriptor) {
                        SupportedComplexDataInputType complexDataType = dataInput.addNewComplexData();
                        ComplexDataInputDescriptor complexInputDescriptor =
                                (ComplexDataInputDescriptor)inputDescriptor;
                        if (complexInputDescriptor.hasMaximumMegaBytes()) {
                            complexDataType.setMaximumMegabytes(complexInputDescriptor.getMaximumMegaBytes());
                        }
                        describeComplexDataInputType(complexDataType, inputDescriptor.getBinding());
                    }
                }
            }
            // 3. Outputs
            ProcessOutputs dataOutputs = processDescription.addNewProcessOutputs();
            Collection<OutputDescriptor> outputDescriptors = algorithmDescriptor.getOutputDescriptors();
            if (outputDescriptors.size() < 1) {
               LOGGER.error("No outputs found for algorithm {}", algorithmDescriptor.getIdentifier());
            }
            for (OutputDescriptor outputDescriptor : outputDescriptors) {

                OutputDescriptionType dataOutput = dataOutputs.addNewOutput();
                dataOutput.addNewIdentifier().setStringValue(outputDescriptor.getIdentifier());
                dataOutput.addNewTitle().setStringValue( outputDescriptor.hasTitle() ?
                        outputDescriptor.getTitle() :
                        outputDescriptor.getIdentifier());
                if (outputDescriptor.hasAbstract()) {
                    dataOutput.addNewAbstract().setStringValue(outputDescriptor.getAbstract());
                }

                if (outputDescriptor instanceof LiteralDataOutputDescriptor) {
                    LiteralDataOutputDescriptor literalDescriptor = (LiteralDataOutputDescriptor)outputDescriptor;
                    dataOutput.addNewLiteralOutput().addNewDataType().
                            setReference(literalDescriptor.getDataType());
                } else if (outputDescriptor instanceof ComplexDataOutputDescriptor) {
                    describeComplexDataOutputType(dataOutput.addNewComplexOutput(), outputDescriptor.getBinding());
               }
            }
        }
        return document.getProcessDescriptions().getProcessDescriptionArray(0);
    }

    private void describeComplexDataInputType(SupportedComplexDataType complexData, Class<?> dataTypeClass) {
        List<IParser> foundParsers = new LinkedList<>();
        for (IParser parser : ParserFactory.getInstance().getAllParsers()) {
            if (parser.isSupportedDataBinding(dataTypeClass)) {
                foundParsers.add(parser);
            }
        }
        describeComplexDataType(complexData, foundParsers);
    }

    private void describeComplexDataOutputType(SupportedComplexDataType complexData, Class<?> dataTypeClass) {
        List<IGenerator> foundGenerators = new LinkedList<>();
        for (IGenerator generator : GeneratorFactory.getInstance().getAllGenerators()) {
            if (generator.isSupportedDataBinding(dataTypeClass)) {
                foundGenerators.add(generator);
            }
        }
        describeComplexDataType(complexData, foundGenerators);
    }

    private void describeComplexDataType(SupportedComplexDataType complexData,
                                         List<? extends IOHandler> handlers) {
        ComplexDataCombinationsType supportedFormatType = complexData.addNewSupported();
        for (IOHandler handler : handlers) {
            Set<Format> fullFormats = handler.getSupportedFormats();
            if (fullFormats.isEmpty()) {
                LOGGER.warn("Skipping IOHandler {} in ProcessDescription generation for {}, no formats specified",
                            handler.getClass().getSimpleName(), getWellKnownName());
            }
            for (Format format : fullFormats) {
                if (complexData.getDefault() == null) {
                    format.encodeTo(complexData.addNewDefault().addNewFormat());
                }
                format.encodeTo(supportedFormatType.addNewFormat());
            }
        }
    }
    
    @Override
    public boolean processDescriptionIsValid() {
        XmlOptions xmlOptions = new XmlOptions();
        List<XmlValidationError> xmlValidationErrorList = new LinkedList<>();
            xmlOptions.setErrorListener(xmlValidationErrorList);
        boolean valid = getDescription().validate(xmlOptions);
        if (!valid) {
            LOGGER.error("Error validating process description for " + getClass().getCanonicalName());
            for (XmlValidationError xmlValidationError : xmlValidationErrorList) {
                LOGGER.error("\tMessage: {}", xmlValidationError.getMessage());
                LOGGER.error("\tLocation of invalid XML: {}",
                     xmlValidationError.getCursorLocation().xmlText());
            }
        }
        return valid;
    }

    protected final synchronized AlgorithmDescriptor getAlgorithmDescriptor() {
        if (descriptor == null) {
            descriptor = createAlgorithmDescriptor();
        }
        return descriptor;
    }
    
    protected abstract AlgorithmDescriptor createAlgorithmDescriptor();

    @Override
    public Class<? extends IData> getInputDataType(String identifier) {
        AlgorithmDescriptor algorithmDescriptor = getAlgorithmDescriptor();
        if (algorithmDescriptor != null) {
            return getAlgorithmDescriptor().getInputDescriptor(identifier).getBinding();
        } else {
            throw new IllegalStateException("Instance must have an algorithm descriptor");
        }
    }

    @Override
    public Class<? extends IData> getOutputDataType(String identifier) {
        AlgorithmDescriptor algorithmDescriptor = getAlgorithmDescriptor();
        if (algorithmDescriptor != null) {
            return getAlgorithmDescriptor().getOutputDescriptor(identifier).getBinding();
        } else {
            throw new IllegalStateException("Instance must have an algorithm descriptor");
        }
    }

    

    @Override
    public Object getState() {
        return state;
    }

    @Override
    public void update(Object state) {
        this.state = state;
        notifyObservers();
    }

    @Override
    public void addObserver(IObserver o) {
        observers.add(o);
    }

    @Override
    public void removeObserver(IObserver o) {
        observers.remove(o);
    }

    public void notifyObservers() {
        for (IObserver o : this.observers) {
            o.update(this);
        }
    }
    
    protected List<String> addError(String error) {
        errorList.add(error);
        return errorList;
    }

    @Override
    public List<String> getErrors() {
        return Collections.unmodifiableList(errorList);
    }
}
