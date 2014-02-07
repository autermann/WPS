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
package org.n52.wps.server.algorithm.spatialquery;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.io.data.binding.literal.LiteralBooleanBinding;
import org.n52.wps.server.AbstractSelfDescribingAlgorithm;
import org.opengis.feature.simple.SimpleFeature;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Geometry;

/**
 * @author Benjamin Pross (bpross-52n) 
 *
 */
public class CrossesAlgorithm extends AbstractSelfDescribingAlgorithm {

	private final String inputID1 = "LAYER1";
	private final String inputID2 = "LAYER2";
	private final String outputID = "RESULT";
	private final List<String> errors = new ArrayList<>();

    @Override
	public List<String> getErrors() {
		return errors;
	}

    @Override
	public Class<GTVectorDataBinding> getInputDataType(String id) {
		if (id.equalsIgnoreCase(inputID1) || id.equalsIgnoreCase(inputID2)) {
			return GTVectorDataBinding.class;
		}
		return null;
	}

    @Override
	public Class<LiteralBooleanBinding> getOutputDataType(String id) {
		if(id.equalsIgnoreCase(outputID)){
			return LiteralBooleanBinding.class;
		}
		return null;
	}

	@Override
	public Map<String, IData> run(Map<String, List<IData>> inputData) {

		      if (inputData == null) {
            throw new RuntimeException("Error while allocating input parameters");
        }
				
		FeatureCollection<?, ?> firstCollection = getSingleInput(inputID1, inputData);
		FeatureCollection<?, ?> secondCollection = getSingleInput(inputID2, inputData);
		FeatureIterator<?> firstIterator = firstCollection.features();
		FeatureIterator<?> secondIterator = secondCollection.features();
		
		if(!firstIterator.hasNext()){
			throw new RuntimeException("Error while iterating over features in layer 1");
		}
		
		if(!secondIterator.hasNext()){
			throw new RuntimeException("Error while iterating over features in layer 2");
		}
		
		SimpleFeature firstFeature = (SimpleFeature) firstIterator.next();
		SimpleFeature secondFeature = (SimpleFeature) secondIterator.next();
		
		boolean crosses = ((Geometry)firstFeature.getDefaultGeometry()).crosses((Geometry)secondFeature.getDefaultGeometry());
		
		return ImmutableMap.of(outputID, (IData) new LiteralBooleanBinding(crosses));
	}

    private FeatureCollection<?, ?> getSingleInput(String key, Map<String, List<IData>> inputData) {
        List<IData> firstDataList = inputData.get(key);
        if(firstDataList == null || firstDataList.size() != 1){
            throw new RuntimeException("Error while allocating input parameters");
        }
        IData firstInputData = firstDataList.get(0);
        return ((GTVectorDataBinding) firstInputData).getPayload();
    }

	@Override
	public List<String> getInputIdentifiers() {
        return Lists.newArrayList(inputID1, inputID2);
	}

	@Override
	public List<String> getOutputIdentifiers() {
		return Lists.newArrayList(outputID);
	}

}
