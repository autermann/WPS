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
			throw new NoApplicableCodeException(e, "Error while generating raw data out of the process result");
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
