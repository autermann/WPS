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
package org.n52.wps.io.datahandler.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.factory.Hints;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.n52.wps.commons.Format;
import org.n52.wps.io.IOUtils;
import org.n52.wps.io.data.binding.complex.GTRasterDataBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeotiffZippedParser extends AbstractParser {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(GeotiffZippedParser.class);
	
	public GeotiffZippedParser() {
		super(GTRasterDataBinding.class);
	}
	
	@Override
	public GTRasterDataBinding parse(InputStream input, Format format) {
		//unzip
		File zippedFile;
		try {
			zippedFile = IOUtils.writeStreamToFile(input, "zip");
			finalizeFiles.add(zippedFile); // mark for final delete
		
			List<File> files = IOUtils.unzipAll(zippedFile);
			finalizeFiles.addAll(files); // mark for final delete
			
			for(File file : files){
				if(file.getName().toLowerCase().endsWith(".tif") || file.getName().toLowerCase().endsWith(".tiff")){
					return parseTiff(file);
				}
			}
			
		} catch (IOException e) {
			LOGGER.error("Exception while trying to unzip tiff.", e);
		}
		throw new RuntimeException("Could not parse zipped geotiff.");
	}
	
	private GTRasterDataBinding parseTiff(File file){
		Hints hints = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER,
				Boolean.TRUE);
		GeoTiffReader reader;
		try {
			reader = new GeoTiffReader(file, hints);
			GridCoverage2D coverage = reader.read(null);
			return new GTRasterDataBinding(coverage);
		} catch (Exception e) {
			LOGGER.error("Exception while trying to create GTRasterDataBinding out of tiff.", e);
			throw new RuntimeException(e);
		} 
	}

}
