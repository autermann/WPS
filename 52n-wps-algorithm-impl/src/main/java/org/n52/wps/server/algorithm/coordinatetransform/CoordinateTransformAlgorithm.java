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
package org.n52.wps.server.algorithm.coordinatetransform;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollections;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.n52.wps.io.GTHelper;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.io.data.binding.literal.LiteralStringBinding;
import org.n52.wps.server.AbstractSelfDescribingAlgorithm;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.InvalidParameterValueException;
import org.n52.wps.server.MissingParameterValueException;
import org.n52.wps.server.NoApplicableCodeException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Geometry;

public class CoordinateTransformAlgorithm
        extends AbstractSelfDescribingAlgorithm {

	private static final Logger LOGGER = LoggerFactory.getLogger(CoordinateTransformAlgorithm.class);

    private static final String INPUT_ID_FEATURES = "InputData";
	private static final String INPUT_ID_TRANSFORMATION = "Transformation";
	private static final String INPUT_ID_TARGET_CRS = "TargetCRS";
	private static final String INPUT_ID_SOURCE_CRS = "SourceCRS";
	private static final String OUTPUT_ID_RESULT = "TransformedData";
	private SimpleFeatureType featureType;

	@Override
	public List<String> getInputIdentifiers() {
        return ImmutableList.of(INPUT_ID_FEATURES,
                                INPUT_ID_SOURCE_CRS,
                                INPUT_ID_TARGET_CRS,
                                INPUT_ID_TRANSFORMATION);
    }

	@Override
	public List<String> getOutputIdentifiers() {
		return ImmutableList.of(OUTPUT_ID_RESULT);
	}

	@Override
	public Class<?> getInputDataType(String id) {
        switch (id) {
            case INPUT_ID_FEATURES:
                return GTVectorDataBinding.class;
            case INPUT_ID_TARGET_CRS:
            case INPUT_ID_SOURCE_CRS:
            case INPUT_ID_TRANSFORMATION:
                return LiteralStringBinding.class;
        }
		return null;
	}

	@Override
	public Class<?> getOutputDataType(String id) {
		return GTVectorDataBinding.class;
	}

	@SuppressWarnings( { "unchecked" })
	@Override
	public Map<String, IData> run(Map<String, List<IData>> inputData)
            throws ExceptionReport{

		if (inputData == null || !inputData.containsKey(INPUT_ID_FEATURES) || !inputData.containsKey(INPUT_ID_TARGET_CRS)) {
			LOGGER.error("Error while allocating input parameters");
			throw new MissingParameterValueException("Error while allocating input parameters");
		}

		List<IData> dataList = inputData.get(INPUT_ID_FEATURES);
		if (dataList == null || dataList.size() != 1) {
			throw new MissingParameterValueException("Error while allocating input parameters");
		}

		IData firstInputData = dataList.get(0);
		FeatureCollection<?, ?> featureCollection = ((GTVectorDataBinding) firstInputData).getPayload();

		FeatureIterator<?> featureIterator = featureCollection.features();

		List<IData> secondDataList = inputData.get(INPUT_ID_TARGET_CRS);
		if (secondDataList == null || secondDataList.size() != 1) {
			throw new MissingParameterValueException("Error while allocating input parameters");
		}

		IData secondInputData = secondDataList.get(0);

		// crs in epsg code
		String crs = ((LiteralStringBinding) secondInputData).getPayload();
        CoordinateReferenceSystem toCRS = decodeCRS(crs);

		List<IData> thirdDataList = inputData.get(INPUT_ID_SOURCE_CRS);
		if (thirdDataList == null || thirdDataList.size() != 1) {
			throw new MissingParameterValueException("Error while allocating input parameters");
		}

		IData thirdInputData = thirdDataList.get(0);

		// crs in epsg code
		String fromCRSString = ((LiteralStringBinding) thirdInputData).getPayload();
        CoordinateReferenceSystem fromCRS = decodeCRS(fromCRSString);


		SimpleFeatureCollection fOut = DefaultFeatureCollections.newCollection();

		try {
			MathTransform tx = CRS.findMathTransform(fromCRS, toCRS, true);
			while (featureIterator.hasNext()) {
				SimpleFeature feature = (SimpleFeature) featureIterator.next();
				Geometry geometry = (Geometry) feature.getDefaultGeometry();
				Geometry newGeometry = JTS.transform(geometry, tx);
				SimpleFeature newFeature = createFeature(feature.getID(),
                        newGeometry, toCRS, feature.getProperties());
                fOut.add(newFeature);
			}
		} catch (NoSuchElementException | MismatchedDimensionException | FactoryException | TransformException e) {
			throw new NoApplicableCodeException("Error while transforming").causedBy(e);
		}
        return ImmutableMap.of(OUTPUT_ID_RESULT, (IData) new GTVectorDataBinding(fOut));
    }

    private CoordinateReferenceSystem decodeCRS(String crs)
            throws ExceptionReport {
        CoordinateReferenceSystem toCRS = null;
        try {
            toCRS = CRS.decode(crs);
        } catch (FactoryException e) {
            throw new InvalidParameterValueException("Could not determine target CRS. Valid EPSG code needed.").causedBy(e);
        }
        if (toCRS == null) {
            throw new InvalidParameterValueException("Could not determine target CRS. Valid EPSG code needed.");
        }
        return toCRS;
    }

    private SimpleFeature createFeature(String id, Geometry geometry,
                                  CoordinateReferenceSystem crs,
                                  Collection<Property> properties) {
        if (featureType == null) {
            String uuid = UUID.randomUUID().toString();
            featureType = GTHelper.createFeatureType(
                    properties, geometry, uuid, crs);
            GTHelper.createGML3SchemaForFeatureType(featureType);
        }
        return GTHelper.createFeature(id, geometry, featureType, properties);
    }

}
