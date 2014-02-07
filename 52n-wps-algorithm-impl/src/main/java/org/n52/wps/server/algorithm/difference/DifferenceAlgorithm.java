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
package org.n52.wps.server.algorithm.difference;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.namespace.QName;

import org.geotools.feature.DefaultFeatureCollections;
import org.geotools.feature.FeatureCollection;
import org.n52.wps.io.GTHelper;
import org.n52.wps.io.SchemaRepository;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.server.AbstractSelfDescribingAlgorithm;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Geometry;



public class DifferenceAlgorithm extends AbstractSelfDescribingAlgorithm {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DifferenceAlgorithm.class);
    private static final String RESULT = "result";
    private static final String POLYGONS2 = "Polygons2";
    private static final String POLYGONS1 = "Polygons1";
	private final List<String> errors = new LinkedList<>();

	@Override
	public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }
	
    @Override
	public Map<String, IData> run(Map<String, List<IData>> inputData) {
		/*----------------------Polygons Input------------------------------------------*/
		if(inputData==null || !inputData.containsKey(POLYGONS1)){
			throw new RuntimeException("Error while allocating input parameters");
		}
		List<IData> dataList = inputData.get(POLYGONS1);
		if(dataList == null || dataList.size() != 1){
			throw new RuntimeException("Error while allocating input parameters");
		}
		IData firstInputData = dataList.get(0);
				
		FeatureCollection polygons = ((GTVectorDataBinding) firstInputData).getPayload();
		
		/*----------------------LineStrings Input------------------------------------------*/
		if(!inputData.containsKey(POLYGONS2)){
			throw new RuntimeException("Error while allocating input parameters");
		}
		List<IData> dataListLS = inputData.get(POLYGONS2);
		if(dataListLS == null || dataListLS.size() != 1){
			throw new RuntimeException("Error while allocating input parameters");
		}
		IData secondInputData = dataListLS.get(0);
				
		FeatureCollection lineStrings = ((GTVectorDataBinding) secondInputData).getPayload();
		
		
		LOGGER.info("difference algorithm started");
		LOGGER.info("polygons size = " + polygons.size());
		LOGGER.info("lineStrings size = " + lineStrings.size());
		
		FeatureCollection featureCollection = DefaultFeatureCollections.newCollection();
		
		Iterator polygonIterator = polygons.iterator();
		int j = 1;
		
		String uuid = UUID.randomUUID().toString();
		while(polygonIterator.hasNext()){
			SimpleFeature polygon = (SimpleFeature) polygonIterator.next();

		
			Iterator lineStringIterator = lineStrings.iterator();
			int i = 1;
			LOGGER.debug("Polygon = " + j +"/"+ polygons.size());
			SimpleFeatureType featureType = null; 
			while(lineStringIterator.hasNext()){
				SimpleFeature lineString = (SimpleFeature) lineStringIterator.next();
				Geometry lineStringGeometry = null;
				lineStringGeometry = (Geometry) lineString.getDefaultGeometry();
				
				try{	
					Geometry polygonGeometry = (Geometry) polygon.getDefaultGeometry();
					Geometry intersection = polygonGeometry.difference(lineStringGeometry);
					if(i==1){
						 featureType = GTHelper.createFeatureType(polygon.getProperties(), intersection, uuid, polygon.getFeatureType().getCoordinateReferenceSystem());
						 QName qname = GTHelper.createGML3SchemaForFeatureType(featureType);
						 SchemaRepository.registerSchemaLocation(qname.getNamespaceURI(), qname.getLocalPart());
					}
					
				
					Feature resultFeature = GTHelper.createFeature(""+j+"_"+i, intersection,featureType, polygon.getProperties());
					if(resultFeature!=null){
								
						featureCollection.add(resultFeature);
						LOGGER.debug("result feature added. resultCollection = " + featureCollection.size());
					}
				}catch(Exception e){
						e.printStackTrace();
					}
				
				i++;
			}
			j++;
			//if(featureCollection.size()>10){
			//	break;
			//}
		}
		return ImmutableMap.of(RESULT, (IData) new GTVectorDataBinding(featureCollection));
	}
	
    @Override
	public Class<?> getInputDataType(String id) {
		return GTVectorDataBinding.class;
	}

    @Override
	public Class<?> getOutputDataType(String id) {
		return GTVectorDataBinding.class;
	}
	
	@Override
	public List<String> getInputIdentifiers() {
		return ImmutableList.of(POLYGONS1, POLYGONS2);
	}

	@Override
	public List<String> getOutputIdentifiers() {
		return ImmutableList.of(RESULT);
	}
	
}