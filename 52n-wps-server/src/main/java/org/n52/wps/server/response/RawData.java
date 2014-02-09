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
package org.n52.wps.server.response;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import net.opengis.wps.x100.ProcessDescriptionType;

import org.n52.wps.commons.Format;
import org.n52.wps.io.data.IBBOXData;
import org.n52.wps.io.data.IComplexData;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.ILiteralData;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.NoApplicableCodeException;

import com.google.common.primitives.Doubles;

/*
 * @author foerster
 *
 */
public class RawData extends ResponseData {
    private static final String DEFAULT_ENCODING = "UTF-8";

    public RawData(IData obj, String id, Format format,
                   ProcessDescriptionType description) throws ExceptionReport {
        super(obj, id, format, description);
        if (obj instanceof IComplexData) {
            prepareGenerator();
        }

    }

    public InputStream getAsStream() throws ExceptionReport {
        try {
            IData payload = getPayload();
            if (payload instanceof ILiteralData) {
                return encodeLiteralData();
            } else  if(payload instanceof IBBOXData){
                return encodeBBOXData();
            } else if (payload instanceof IComplexData) {
                return encodeComplexData();
            } else {
                throw new NoApplicableCodeException("vUnknown data type: %s", payload);
            }
            
		} catch (IOException e) {
			throw new NoApplicableCodeException("Error while generating raw data out of the process result").causedBy(e);
		}
	}

    private InputStream encodeComplexData() throws IOException, ExceptionReport {
        return getGenerator().generate(getPayload(), getFormat());
    }

    private InputStream encodeLiteralData() throws UnsupportedEncodingException {
        String result = String.valueOf(getPayload().getPayload());
        return new ByteArrayInputStream(result.getBytes(
                getFormat().getEncoding().or(DEFAULT_ENCODING)));
    }

    private InputStream encodeBBOXData() {

//        DataType dt = DataType.Factory.newInstance();
//        BoundingBoxType bbd = dt.addNewBoundingBoxData();
        IBBOXData data = (IBBOXData) getPayload();

        StringBuilder resultString = new StringBuilder();
        resultString.append("<wps:BoundingBoxData");
        resultString.append(" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\"");
        if(data.getCRS() != null && !data.getCRS().isEmpty()){
            resultString.append(" crs=\"").append(data.getCRS()).append("\"");
            resultString.append(" dimensions=\"").append(data.getDimension()).append("\"");
        }
        resultString.append(">");

        resultString.append("<ows:LowerCorner>");
        resultString.append(Doubles.join(" ", data.getLowerCorner()));
        resultString.append("</ows:LowerCorner>");

        resultString.append("<ows:UpperCorner>");
        resultString.append(Doubles.join(" ", data.getUpperCorner()));
        resultString.append("</ows:UpperCorner>");

        resultString.append("</wps:BoundingBoxData>");
        return new ByteArrayInputStream(resultString.toString().getBytes());
    }

	
}
