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
package org.n52.wps.server.algorithm.convexhull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.geotools.feature.DefaultFeatureCollections;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.n52.wps.io.GTHelper;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.server.AbstractSelfDescribingAlgorithm;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.algorithm.ConvexHull;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * @author Benjamin Pross (bpross-52n) 
 *
 */
public class ConvexHullAlgorithm extends AbstractSelfDescribingAlgorithm {

    private static final String RESULT = "RESULT";
    private static final String FEATURES = "FEATURES";
	private final List<String> errors = new ArrayList<>();

    @Override
	public List<String> getErrors() {
		return Collections.unmodifiableList(errors);
	}

    @Override
	public Class<?> getInputDataType(String id) {
		if (id.equalsIgnoreCase(FEATURES)) {
			return GTVectorDataBinding.class;
		}
		return null;
	}

    @Override
	public Class<?> getOutputDataType(String id) {
		return GTVectorDataBinding.class;
	}
	
    @Override
	public Map<String, IData> run(Map<String, List<IData>> inputData) {

		if (inputData == null || !inputData.containsKey(FEATURES)) {
			throw new RuntimeException(
					"Error while allocating input parameters");
		}
		
		List<IData> dataList = inputData.get(FEATURES);
		if (dataList == null || dataList.size() != 1) {
			throw new RuntimeException(
					"Error while allocating input parameters");
		}
		
		IData firstInputData = dataList.get(0);
		FeatureCollection featureCollection = ((GTVectorDataBinding) firstInputData)
				.getPayload();

		FeatureIterator iter = featureCollection.features();

		List<Coordinate> coordinateList = new ArrayList<Coordinate>();
		
		int counter = 0;
		
		while (iter.hasNext()) {
			SimpleFeature  feature = (SimpleFeature) iter.next();

			if (feature.getDefaultGeometry() == null) {
				throw new NullPointerException(
						"defaultGeometry is null in feature id: "
								+ feature.getID());
			}
			
			Geometry geom = (Geometry) feature.getDefaultGeometry();
			
			Coordinate[] coordinateArray = geom.getCoordinates();
            coordinateList.addAll(Arrays.asList(coordinateArray));
			
		}	
		
		Coordinate[] coordinateArray = new Coordinate[coordinateList.size()];
		
		for(int i = 0; i<coordinateList.size(); i++){
			coordinateArray[i] = coordinateList.get(i);
		}
		ConvexHull convexHull = new ConvexHull(coordinateArray, new GeometryFactory());		
		
		Geometry out = convexHull.getConvexHull();

		Feature feature = createFeature(out, featureCollection.getSchema().getCoordinateReferenceSystem());
		
		FeatureCollection fOut = DefaultFeatureCollections.newCollection();
		
		fOut.add(feature);

		return ImmutableMap.of(RESULT, (IData) new GTVectorDataBinding(fOut));
	}
	
	private Feature createFeature(Geometry geometry, CoordinateReferenceSystem crs) {
		String uuid = UUID.randomUUID().toString();
		SimpleFeatureType featureType = GTHelper.createFeatureType(geometry, uuid, crs);
		GTHelper.createGML3SchemaForFeatureType(featureType);
		
		Feature feature = GTHelper.createFeature("0", geometry, featureType);
		
		return feature;
	}	
	
	@Override
	public List<String> getInputIdentifiers() {
        return ImmutableList.of(FEATURES);
	}

	@Override
	public List<String> getOutputIdentifiers() {
		return ImmutableList.of(RESULT);
	}
}
