/**
 * ﻿Copyright (C) 2007 - 2014 52°North Initiative for Geospatial Open Source
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
 *       • Apache License, version 2.0
 *       • Apache Software License, version 1.0
 *       • GNU Lesser General Public License, version 3
 *       • Mozilla Public License, versions 1.0, 1.1 and 2.0
 *       • Common Development and Distribution License (CDDL), version 1.0
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
package org.n52.wps.server.grass;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.opengis.wps.x100.InputDescriptionType;
import net.opengis.wps.x100.OutputDefinitionType;
import net.opengis.wps.x100.OutputDescriptionType;
import net.opengis.wps.x100.ProcessDescriptionType;
import net.opengis.wps.x100.SupportedComplexDataType;

import org.n52.wps.commons.context.ExecutionContextFactory;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GenericFileDataBinding;
import org.n52.wps.io.data.binding.literal.LiteralBooleanBinding;
import org.n52.wps.io.data.binding.literal.LiteralDoubleBinding;
import org.n52.wps.io.data.binding.literal.LiteralFloatBinding;
import org.n52.wps.io.data.binding.literal.LiteralIntBinding;
import org.n52.wps.io.data.binding.literal.LiteralStringBinding;
import org.n52.wps.server.grass.io.GrassIOHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

/**
 * @author Benjamin Pross (bpross-52n)
 *
 */
public class GrassProcessDelegator extends GenericGrassAlgorithm{
	private static final Logger LOGGER = LoggerFactory.getLogger(GrassProcessDelegator.class);
	private static final String DATA_TYPE_FLOAT = "float";
	private static final String DATA_TYPE_BOOLEAN = "boolean";
	private static final String DATA_TYPE_STRING = "string";
	private static final String DATA_TYPE_INTEGER ="integer";
	private static final String DATA_TYPE_DOUBLE = "double";
	private final boolean isAddon;
	private final ProcessDescriptionType processDescription;
	private final HashMap<String, Class<?>> complexInputTypes = new HashMap<>();
	private final HashMap<String, Class<?>> literalInputTypes = new HashMap<>();
	private final HashMap<String, String> outputTypeMimeTypeMap = new HashMap<>();
	
	
	public GrassProcessDelegator(String processID, ProcessDescriptionType processDescriptionType, boolean isAddon){
        super(processID);
		this.isAddon = isAddon;
		this.processDescription = processDescriptionType;
		mapInputAndOutputTypes();
	}
	
	private void mapInputAndOutputTypes(){
        for (InputDescriptionType input : processDescription.getDataInputs().getInputArray()) {
			String identifier = input.getIdentifier().getStringValue();

			if (input.getComplexData() != null) {
				complexInputTypes.put(identifier, GenericFileDataBinding.class);
			} else if (input.getLiteralData() != null) {
                switch (input.getLiteralData().getDataType().getStringValue()) {
                    case DATA_TYPE_FLOAT:
                        literalInputTypes.put(identifier, LiteralFloatBinding.class);
                        break;
                    case DATA_TYPE_BOOLEAN:
                        literalInputTypes.put(identifier, LiteralBooleanBinding.class);
                        break;
                    case DATA_TYPE_STRING:
                        literalInputTypes.put(identifier, LiteralStringBinding.class);
                        break;
                    case DATA_TYPE_INTEGER:
                        literalInputTypes.put(identifier, LiteralIntBinding.class);
                        break;
                    case DATA_TYPE_DOUBLE:
                        literalInputTypes.put(identifier, LiteralDoubleBinding.class);
                        break;
                }
			}
		}
		
		for (OutputDescriptionType output : processDescription.getProcessOutputs().getOutputArray()) {
			SupportedComplexDataType type = output.getComplexOutput();
			String outputIdentifier = output.getIdentifier().getStringValue();
			String defaultMimeType = type.getDefault().getFormat().getMimeType();
			outputTypeMimeTypeMap.put(outputIdentifier, defaultMimeType);
		}
	}

    @Override
    protected ProcessDescriptionType initializeDescription() {
        return processDescription;
    }

	@Override
	public List<String> getErrors() {
		return Collections.emptyList();
	}

	@Override
	public Class<?> getInputDataType(String id) {
		if(complexInputTypes.containsKey(id)){
			return complexInputTypes.get(id);
		}else if(literalInputTypes.containsKey(id)){
			return literalInputTypes.get(id);
		}else {
			return null;
		}		
	}

	@Override
	public Class<?> getOutputDataType(String id) {		
		return GenericFileDataBinding.class;
	}

	@Override
	public boolean processDescriptionIsValid() {
		return processDescription.validate();
	}

	@Override
	public Map<String, IData> run(Map<String, List<IData>> inputData) {

        LOGGER.info("Executing GRASS process {}.", getWellKnownName());
		
		OutputDefinitionType output = ExecutionContextFactory.getContext().getOutputs().get(0);
		String outputSchema = output.getSchema();
		String outputMimeType = output.getMimeType();
		String outputIdentifier = output.getIdentifier().getStringValue();

		HashMap<String, List<IData>> firstInputMap = new HashMap<>();
		for (String key : complexInputTypes.keySet()) {
			if (inputData.containsKey(key)) {
				firstInputMap.put(key, inputData.get(key));
			}
		}
		
		HashMap<String, List<IData>> secondInputMap = new HashMap<>();
		for (String key : literalInputTypes.keySet()) {
			if (inputData.containsKey(key)) {
				secondInputMap.put(key, inputData.get(key));
			}
		}
		
		if(outputMimeType == null || outputMimeType.isEmpty()){
			outputMimeType = outputTypeMimeTypeMap.get(outputIdentifier);
		}
		
		IData outputFileDB = new GrassIOHandler().executeGrassProcess(
				getWellKnownName(), firstInputMap, secondInputMap,
                outputIdentifier, outputMimeType, outputSchema, isAddon);
		
		if(outputIdentifier == null || outputIdentifier.isEmpty()){
			outputIdentifier = "output";
		}
		return ImmutableMap.of(outputIdentifier, outputFileDB);
	}
}
