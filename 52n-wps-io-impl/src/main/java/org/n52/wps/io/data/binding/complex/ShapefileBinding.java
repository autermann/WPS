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
package org.n52.wps.io.data.binding.complex;

import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.commons.io.FileUtils;
import org.geotools.data.DataStore;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.FeatureCollection;

import org.n52.wps.io.IOHandler;
import org.n52.wps.io.IOUtils;
import org.n52.wps.io.data.IComplexData;
import org.n52.wps.io.data.IComplexData;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.NoApplicableCodeException;

public class ShapefileBinding implements IComplexData {
    private final File shpFile;

    public ShapefileBinding(File shapeFile) {
        this.shpFile = shapeFile;
    }

    @Override
    public File getPayload() {
        return shpFile;
    }

    @Override
    public Class<?> getSupportedClass() {
        return File.class;
    }

    public String getMimeType() {
        return IOHandler.MIME_TYPE_ZIPPED_SHP;
    }

    public File getZippedPayload() throws ExceptionReport {
        String path = shpFile.getAbsolutePath();
        String baseName = path.substring(0, path.length() - ".shp".length());
        File shx = new File(baseName + ".shx");
        File dbf = new File(baseName + ".dbf");
        File prj = new File(baseName + ".prj");
        try {
            return IOUtils.zip(shpFile, shx, dbf, prj);
        } catch (IOException e) {
            throw new NoApplicableCodeException("Could not generate ZIP file").causedBy(e);
        }
    }

    public GTVectorDataBinding getPayloadAsGTVectorDataBinding() throws ExceptionReport {
        try {
            DataStore store = new ShapefileDataStore(shpFile.toURI().toURL());
            FeatureCollection<?, ?> features = store.getFeatureSource(store
                    .getTypeNames()[0]).getFeatures();
            return new GTVectorDataBinding(features);
        } catch (MalformedURLException e) {
            throw new NoApplicableCodeException("Something went wrong while creating data store.").causedBy(e);
        } catch (IOException e) {
            throw new NoApplicableCodeException("Something went wrong while converting shapefile to FeatureCollection").causedBy(e);
        }
    }

    @Override
    public void dispose() {
        FileUtils.deleteQuietly(shpFile);
    }

}
