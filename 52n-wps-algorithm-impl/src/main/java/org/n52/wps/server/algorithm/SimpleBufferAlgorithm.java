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
package org.n52.wps.server.algorithm;

import java.util.Iterator;
import java.util.UUID;

import javax.xml.namespace.QName;

import org.geotools.feature.DefaultFeatureCollections;
import org.geotools.feature.FeatureCollection;
import org.n52.wps.algorithm.annotation.Algorithm;
import org.n52.wps.algorithm.annotation.ComplexDataInput;
import org.n52.wps.algorithm.annotation.ComplexDataOutput;
import org.n52.wps.algorithm.annotation.Execute;
import org.n52.wps.algorithm.annotation.LiteralDataInput;
import org.n52.wps.io.GTHelper;
import org.n52.wps.io.SchemaRepository;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.server.AbstractAnnotatedAlgorithm;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Geometry;

@Algorithm(version = "1.1.0")
public class SimpleBufferAlgorithm extends AbstractAnnotatedAlgorithm {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(SimpleBufferAlgorithm.class);
    private Double percentage;

    private FeatureCollection<SimpleFeatureType, SimpleFeature> result;
    private FeatureCollection<SimpleFeatureType, SimpleFeature> data;
    private double width;

    @ComplexDataOutput(identifier = "result", binding = GTVectorDataBinding.class)
    public FeatureCollection getResult() {
        return result;
    }

    @ComplexDataInput(identifier = "data", binding = GTVectorDataBinding.class)
    public void setData(FeatureCollection data) {
        this.data = data;
    }

    @LiteralDataInput(identifier = "width")
    public void setWidth(double width) {
        this.width = width;
    }

    @Execute
    public void runBuffer() {
        //Collection resultColl = new ArrayList();
        int i = 0;
        int totalNumberOfFeatures = data.size();
        String uuid = UUID.randomUUID().toString();
        result = DefaultFeatureCollections.newCollection();
        SimpleFeatureType featureType = null;
        for (Iterator<SimpleFeature> ia = data.iterator(); ia.hasNext();) {
            /**
             * ******* How to publish percentage results ************
             */
            i += 1;
            percentage = ((double) i / totalNumberOfFeatures) * 100;
            this.update(new Integer(percentage.intValue()));

            /**
             * ******************
             */
            SimpleFeature feature = ia.next();
            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            Geometry geometryBuffered = runBuffer(geometry, width);

            if (i == 1) {
                CoordinateReferenceSystem crs = feature.getFeatureType().getCoordinateReferenceSystem();
                if (geometry.getUserData() instanceof CoordinateReferenceSystem) {
                    crs = ((CoordinateReferenceSystem) geometry.getUserData());
                }
                featureType = GTHelper.createFeatureType(feature.getProperties(), geometryBuffered, uuid, crs);
                QName qname = GTHelper.createGML3SchemaForFeatureType(featureType);
                SchemaRepository.registerSchemaLocation(qname.getNamespaceURI(), qname.getLocalPart());

            }

            if (geometryBuffered != null) {
                SimpleFeature createdFeature = GTHelper.createFeature("ID" + i, geometryBuffered, featureType, feature.getProperties());
                feature.setDefaultGeometry(geometryBuffered);
                result.add(createdFeature);
            } else {
                LOGGER
                        .warn("GeometryCollections are not supported, or result null. Original dataset will be returned");
            }
        }

    }

    private Geometry runBuffer(Geometry a, double width) {
        Geometry buffered = null;

        try {
            buffered = a.buffer(width);
            return buffered;
        } catch (RuntimeException ex) {
            // simply eat exceptions and report them by returning null
        }
        return null;
    }
}
