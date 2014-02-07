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
package org.n52.wps.io.datahandler.generator;

import java.io.IOException;
import java.io.InputStream;

import org.n52.wps.commons.Format;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GenericFileDataBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Benjamin Pross(bpross-52n)
 *
 */
public class GRASSXMLGenerator extends AbstractGenerator {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(GRASSXMLGenerator.class);
	private static final String[] SUPPORTED_SCHEMAS = new String[]{
//		"http://schemas.opengis.net/gml/2.1.1/feature.xsd",
		"http://schemas.opengis.net/gml/2.1.2/feature.xsd",
//		"http://schemas.opengis.net/gml/2.1.2.1/feature.xsd",
//		"http://schemas.opengis.net/gml/3.0.0/base/feature.xsd",
//		"http://schemas.opengis.net/gml/3.0.1/base/feature.xsd",
//		"http://schemas.opengis.net/gml/3.1.1/base/feature.xsd"
		};
	
	public GRASSXMLGenerator(){
		super(GenericFileDataBinding.class);
	}
	
	public InputStream generateStream(IData data, Format format) throws IOException {
		return ((GenericFileDataBinding)data).getPayload().getDataStream();
	}
	
}
