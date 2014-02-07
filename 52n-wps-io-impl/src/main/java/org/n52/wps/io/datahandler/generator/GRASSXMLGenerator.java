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
