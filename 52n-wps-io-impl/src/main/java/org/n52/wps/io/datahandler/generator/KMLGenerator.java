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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.geotools.feature.FeatureCollection;
import org.geotools.kml.KML;
import org.geotools.kml.KMLConfiguration;
import org.geotools.xml.Encoder;

import org.n52.wps.commons.Format;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.NoApplicableCodeException;

/**
 * @author Bastian Schaeffer, IfGI; Matthias Mueller, TU Dresden
 *
 */
public class KMLGenerator extends AbstractGenerator {

	public KMLGenerator(){
		super(GTVectorDataBinding.class);
	}

    @Override
    public InputStream generateStream(IData data, Format format)
            throws IOException, ExceptionReport {
        try {
            File tempFile = registerTempFile();
            try (FileOutputStream os = new FileOutputStream(tempFile)) {
                FeatureCollection<?, ?> fc = (FeatureCollection) data
                        .getPayload();
                Encoder encoder = new Encoder(new KMLConfiguration());
                encoder.encode(fc, KML.kml, os);
                os.flush();
            }
            return new FileInputStream(tempFile);
        } catch (IOException e) {
            throw new NoApplicableCodeException("Unable to generate KML")
                    .causedBy(e);
        }
    }
}
