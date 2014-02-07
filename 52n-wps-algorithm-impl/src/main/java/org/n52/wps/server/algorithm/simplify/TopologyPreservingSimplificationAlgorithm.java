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

package org.n52.wps.server.algorithm.simplify;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.IllegalAttributeException;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.io.data.binding.literal.LiteralDoubleBinding;
import org.n52.wps.server.AbstractSelfDescribingAlgorithm;
import org.opengis.feature.simple.SimpleFeature;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;

/**
 * @author Theodor Foerster, ITC
 *
 */
public class TopologyPreservingSimplificationAlgorithm
    extends AbstractSelfDescribingAlgorithm {
    private static final String SIMPLIFIED_FEATURES = "SIMPLIFIED_FEATURES";
    private static final String WIDTH = "width";
    private static final String TOLERANCE = "TOLERANCE";
    private static final String FEATURES = "FEATURES";
    private static final String RESULT = "result";

    private final List<String> errors = new LinkedList<>();

    public Map<String, IData> run(Map<String, List<IData>> inputData) {
        if(inputData==null || !inputData.containsKey(FEATURES)){
            throw new RuntimeException("Error while allocating input parameters");
        }
        List<IData> dataList = inputData.get(FEATURES);
        if(dataList == null || dataList.size() != 1){
            throw new RuntimeException("Error while allocating input parameters");
        }
        IData firstInputData = dataList.get(0);

        FeatureCollection<?,?> featureCollection = ((GTVectorDataBinding) firstInputData).getPayload();
        FeatureIterator<?> iter = featureCollection.features();

        if( !inputData.containsKey(WIDTH)){
            throw new RuntimeException("Error while allocating input parameters");
        }
        List<IData> widthDataList = inputData.get(TOLERANCE);
        if(widthDataList == null || widthDataList.size() != 1){
            throw new RuntimeException("Error while allocating input parameters");
        }
        Double tolerance = ((LiteralDoubleBinding) widthDataList.get(0)).getPayload();
        while(iter.hasNext()) {
            SimpleFeature f = (SimpleFeature) iter.next();
            Object userData = ((Geometry)f.getDefaultGeometry()).getUserData();

            try{
                Geometry in = (Geometry)f.getDefaultGeometry();
                Geometry out = TopologyPreservingSimplifier.simplify(in, tolerance);
                /*
                * THIS PASSAGE WAS CONTRIBUTED BY GOBE HOBONA.
                *The simplification of MultiPolygons produces Polygon geometries. This becomes inconsistent with the original schema (which was of MultiPolygons).
                *To ensure that the output geometries match that of the original schema we add the Polygon(from the simplication) to a MultiPolygon object
                *
                *This is issue is known to affect MultiPolygon geometries only, other geometries need to be tested to ensure conformance with the original (input) schema
                */
                if(in.getGeometryType().equals("MultiPolygon") && out.getGeometryType().equals("Polygon"))
                {
                    MultiPolygon mp = (MultiPolygon)in;
                    Polygon[] p = {(Polygon)out};
                    mp = new MultiPolygon(p,mp.getFactory());
                    f.setDefaultGeometry(mp);
                }
                else if(in.getGeometryType().equals("MultiLineString") && out.getGeometryType().equals("LineString")) {
                    MultiLineString ml = (MultiLineString)in;
                    LineString[] l = {(LineString)out};
                    ml = new MultiLineString(l,ml.getFactory());
                    f.setDefaultGeometry(ml);
                }
                else {
                    f.setDefaultGeometry(out);
                }
                ((Geometry)f.getDefaultGeometry()).setUserData(userData);
            } catch(IllegalAttributeException e) {
                throw new RuntimeException("geometrytype of result is not matching", e);
            }
        }
        return ImmutableMap.of(SIMPLIFIED_FEATURES, (IData) new GTVectorDataBinding(featureCollection));
    }

    @Override
	public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    @Override
    public Class<?> getInputDataType(String id) {
        switch (id) {
            case FEATURES:
                return GTVectorDataBinding.class;
            case TOLERANCE:
                return LiteralDoubleBinding.class;
            default:
                return null;
        }
    }

	@Override
	public Class<?> getOutputDataType(String id) {
        switch (id) {
            case RESULT:
                return GTVectorDataBinding.class;
            default:
                return null;
        }
    }

	@Override
	public List<String> getInputIdentifiers() {
        return ImmutableList.of(FEATURES, TOLERANCE);
    }

	@Override
	public List<String> getOutputIdentifiers() {
		return ImmutableList.of(RESULT);
	}
}
