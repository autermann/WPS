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
package org.n52.wps.io.datahandler.generator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geojson.geom.GeometryJSON;
import org.n52.wps.commons.Format;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.io.data.binding.complex.JTSGeometryBinding;

import com.vividsolutions.jts.geom.Geometry;

/**
 * This class generates a GeoJSON String representation out of a JTS Geometry.
 *
 * @author BenjaminPross(bpross-52n)
 *
 */
public class GeoJSONGenerator extends AbstractGenerator {

    public GeoJSONGenerator() {
        super(JTSGeometryBinding.class, GTVectorDataBinding.class);
    }

    @Override
    public InputStream generateStream(IData data, Format format)
            throws IOException {

        if (data instanceof JTSGeometryBinding) {
            Geometry g = ((JTSGeometryBinding) data).getPayload();

            File tempFile = registerTempFile(File.createTempFile("wps", "json"));

            new GeometryJSON().write(g, tempFile);

            InputStream is = new FileInputStream(tempFile);

            return is;
        } else if (data instanceof GTVectorDataBinding) {

            SimpleFeatureCollection f = (SimpleFeatureCollection) data
                    .getPayload();

            File tempFile = registerTempFile(File.createTempFile("wps", "json"));
            new FeatureJSON().writeFeatureCollection(f, tempFile);

            InputStream is = new FileInputStream(tempFile);

            return is;
        }

        return null;
    }

}
