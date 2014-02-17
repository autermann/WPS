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
package org.n52.wps.server;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.opengis.ows.x11.DomainMetadataType;
import net.opengis.wps.x100.CRSsType;
import net.opengis.wps.x100.ComplexDataCombinationsType;
import net.opengis.wps.x100.ComplexDataDescriptionType;
import net.opengis.wps.x100.InputDescriptionType;
import net.opengis.wps.x100.LiteralInputType;
import net.opengis.wps.x100.LiteralOutputType;
import net.opengis.wps.x100.OutputDescriptionType;
import net.opengis.wps.x100.ProcessDescriptionType;
import net.opengis.wps.x100.ProcessDescriptionType.DataInputs;
import net.opengis.wps.x100.ProcessDescriptionType.ProcessOutputs;
import net.opengis.wps.x100.ProcessDescriptionsDocument;
import net.opengis.wps.x100.ProcessDescriptionsDocument.ProcessDescriptions;
import net.opengis.wps.x100.SupportedCRSsType;
import net.opengis.wps.x100.SupportedComplexDataInputType;
import net.opengis.wps.x100.SupportedComplexDataType;

import org.n52.wps.commons.Format;
import org.n52.wps.io.GeneratorFactory;
import org.n52.wps.io.IOHandler;
import org.n52.wps.io.LiteralDataFactory;
import org.n52.wps.io.ParserFactory;
import org.n52.wps.io.data.IBBOXData;
import org.n52.wps.io.data.IComplexData;
import org.n52.wps.io.data.ILiteralData;
import org.n52.wps.server.observerpattern.IObserver;
import org.n52.wps.server.observerpattern.ISubject;

public abstract class AbstractSelfDescribingAlgorithm
        extends AbstractAlgorithm implements ISubject {

    public static final String PROCESS_VERSION = "1.0.0";
    private final List<IObserver> observers = new ArrayList<>();
    private Object state = null;

    @Override
	protected ProcessDescriptionType initializeDescription() {
		ProcessDescriptionsDocument document = ProcessDescriptionsDocument.Factory.newInstance();
		ProcessDescriptions processDescriptions = document.addNewProcessDescriptions();
		ProcessDescriptionType processDescription = processDescriptions.addNewProcessDescription();
		processDescription.setStatusSupported(true);
		processDescription.setStoreSupported(true);
		processDescription.setProcessVersion(PROCESS_VERSION);

		//1. Identifier
		processDescription.addNewIdentifier().setStringValue(this.getClass().getName());
		processDescription.addNewTitle().setStringValue(this.getClass().getCanonicalName());

		//2. Inputs
		List<String> identifiers = this.getInputIdentifiers();
		DataInputs dataInputs = null;
		if(identifiers.size()>0){
			dataInputs = processDescription.addNewDataInputs();
		}

		for(String identifier : identifiers){
			InputDescriptionType dataInput = dataInputs.addNewInput();
			dataInput.setMinOccurs(getMinOccurs(identifier));
			dataInput.setMaxOccurs(getMaxOccurs(identifier));
			dataInput.addNewIdentifier().setStringValue(identifier);
			dataInput.addNewTitle().setStringValue(identifier);

			Class<?> inputDataTypeClass = this.getInputDataType(identifier);

			//we have to add this because of the new AbstractLiteralDataBinding class


            if (ILiteralData.class.isAssignableFrom(inputDataTypeClass)) {
                LiteralInputType literalData = dataInput.addNewLiteralData();
                @SuppressWarnings("unchecked")
                Class<? extends ILiteralData> literalInputDataTypeClass
                        = (Class<? extends ILiteralData>) inputDataTypeClass;
                DomainMetadataType datatype = literalData.addNewDataType();
                datatype.setReference(LiteralDataFactory
                        .getTypeforBindingType(literalInputDataTypeClass));
                literalData.addNewAnyValue();
            } else if (IBBOXData.class.isAssignableFrom(inputDataTypeClass)) {
                SupportedCRSsType bboxData = dataInput.addNewBoundingBoxData();
                String[] supportedCRSAray
                        = getSupportedCRSForBBOXInput(identifier);
                Iterator<String> crs = Arrays.asList(supportedCRSAray)
                        .iterator();
                if (crs.hasNext()) {
                    CRSsType supported = bboxData.addNewSupported();
                    String defaultCRS = crs.next();
                    bboxData.addNewDefault().setCRS(defaultCRS);
                    supported.addCRS(defaultCRS);
                    while (crs.hasNext()) {
                        supported.addCRS(crs.next());
                    }
                }
            } else if (IComplexData.class.isAssignableFrom(inputDataTypeClass)) {
                SupportedComplexDataInputType complexData = dataInput.addNewComplexData();
                encodeFormats(complexData, ParserFactory.getInstance().findParsers(inputDataTypeClass));
            }
		}

		//3. Outputs
		ProcessOutputs dataOutputs = processDescription.addNewProcessOutputs();
		List<String> outputIdentifiers = this.getOutputIdentifiers();
		for(String identifier : outputIdentifiers){
			OutputDescriptionType dataOutput = dataOutputs.addNewOutput();
			dataOutput.addNewIdentifier().setStringValue(identifier);
			dataOutput.addNewTitle().setStringValue(identifier);
			dataOutput.addNewAbstract().setStringValue(identifier);

            Class<?> outputDataTypeClass = this.getOutputDataType(identifier);
            if (ILiteralData.class.isAssignableFrom(outputDataTypeClass)) {
                LiteralOutputType literalData = dataOutput.addNewLiteralOutput();
                @SuppressWarnings("unchecked")
                Class<? extends ILiteralData> literalOutputDataTypeClass
                        = (Class<? extends ILiteralData>) outputDataTypeClass;
                literalData.addNewDataType().setReference(LiteralDataFactory
                        .getTypeforBindingType(literalOutputDataTypeClass));
            } else if (IBBOXData.class.isAssignableFrom(outputDataTypeClass)) {
                SupportedCRSsType bboxData = dataOutput .addNewBoundingBoxOutput();
                String[] supportedCRSAray = getSupportedCRSForBBOXOutput(identifier);
                Iterator<String> crs = Arrays.asList(supportedCRSAray).iterator();
                if (crs.hasNext()) {
                    CRSsType supported = bboxData.addNewSupported();
                    String defaultCRS = crs.next();
                    bboxData.addNewDefault().setCRS(defaultCRS);
                    supported.addCRS(defaultCRS);
                    while (crs.hasNext()) {
                        supported.addCRS(crs.next());
                    }
                }
            } else if (IComplexData.class.isAssignableFrom(outputDataTypeClass)) {
                SupportedComplexDataType complexData = dataOutput.addNewComplexOutput();
                encodeFormats(complexData, GeneratorFactory.getInstance().findGenerators(outputDataTypeClass));
            }
        }

		return document.getProcessDescriptions().getProcessDescriptionArray(0);
	}

	/**
	 * Override this class for BBOX input data to set supported mime types. The first one in the resulting array will be the default one.
	 * @param identifier ID of the input BBOXType
	 * @return
	 */
	public String[] getSupportedCRSForBBOXInput(String identifier){
		return new String[0];
	}

	/**
	 * Override this class for BBOX output data to set supported mime types. The first one in the resulting array will be the default one.
	 * @param identifier ID of the input BBOXType
	 * @return
	 */
	public String[] getSupportedCRSForBBOXOutput(String identifier){
		return new String[0];
	}

	public BigInteger getMinOccurs(String identifier){
		return BigInteger.ONE;
	}
	public BigInteger getMaxOccurs(String identifier){
		return BigInteger.ONE;
	}

	public abstract List<String> getInputIdentifiers();
	public abstract List<String> getOutputIdentifiers();

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

    @Override
    public List<String> getErrors() {
        return Collections.emptyList();
    }

    private void encodeFormats(SupportedComplexDataType complexData,
                               Iterable<? extends IOHandler> handlers) {
        ComplexDataCombinationsType xbSupported = complexData.addNewSupported();
        for (IOHandler parser : handlers) {
            for (Format format : parser.getSupportedFormats()) {
                ComplexDataDescriptionType xbFormat;
                if (complexData.getDefault() == null) {
                    xbFormat = complexData.addNewDefault().addNewFormat();
                } else {
                    xbFormat = xbSupported.addNewFormat();
                }
                format.encodeTo(xbFormat);
            }
        }
    }
}
