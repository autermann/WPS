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

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.DataSourceException;
import org.geotools.gce.arcgrid.ArcGridWriter;
import org.opengis.coverage.grid.GridCoverageWriter;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;

import org.n52.wps.commons.Format;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GTRasterDataBinding;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.NoApplicableCodeException;

public class AsciiGrassGenerator extends AbstractGenerator {

	public AsciiGrassGenerator() {
		super(GTRasterDataBinding.class);
	}

    @Override
	public InputStream generateStream(IData data, Format format) throws IOException, ExceptionReport {
		try {
            GridCoverage2D grid = ((GTRasterDataBinding) data).getPayload();
            File outputFile = registerTempFile();
            GridCoverageWriter writer = new ArcGridWriter(outputFile);

			// setting write parameters
			ParameterValueGroup params = writer.getFormat().getWriteParameters();
			params.parameter("GRASS").setValue(true);
			GeneralParameterValue[] gpv = { params.parameter("GRASS") };

			writer.write(grid, gpv);
			writer.dispose();

            return new FileInputStream(outputFile);
		} catch (DataSourceException e) {
			throw new NoApplicableCodeException("AsciiGRID cannot be read from source").causedBy(e);
		} catch (IllegalArgumentException e) {
			throw new NoApplicableCodeException("Illegal configuration of AsciiGRID writer").causedBy(e);
		} catch (IOException e) {
			throw new NoApplicableCodeException("AsciiGrassGenerator could not create output due to an IO error").causedBy(e);
		}
	}

}
