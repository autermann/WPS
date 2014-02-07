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

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.opengis.ows.x11.BoundingBoxType;
import net.opengis.ows.x11.CodeType;
import net.opengis.ows.x11.LanguageStringType;
import net.opengis.wps.x100.ComplexDataType;
import net.opengis.wps.x100.ExecuteResponseDocument;
import net.opengis.wps.x100.LiteralDataType;
import net.opengis.wps.x100.OutputDataType;
import net.opengis.wps.x100.OutputReferenceType;
import net.opengis.wps.x100.ProcessDescriptionType;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.n52.wps.commons.Format;
import org.n52.wps.io.BasicXMLTypeFactory;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.literal.AbstractLiteralDataBinding;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.database.DatabaseFactory;
import org.n52.wps.server.database.IDatabase;
import org.opengis.geometry.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.google.common.primitives.Doubles;

/*
 * @author foerster
 *
 */
public class OutputDataItem extends ResponseData {

	private static final Logger LOGGER = LoggerFactory.getLogger(OutputDataItem.class);
	private static final String COMPLEX_DATA_TYPE = "ComplexDataResponse";
	private final LanguageStringType title;

	public OutputDataItem(IData obj, String id, Format format, LanguageStringType title, ProcessDescriptionType description) throws ExceptionReport {
		super(obj, id, format, description);
		
		this.title = title;
	}
	
	/**
	 * 
	 * @param res
	 * @throws ExceptionReport
	 */
	public void updateResponseForInlineComplexData(ExecuteResponseDocument res) throws ExceptionReport {
		OutputDataType output = prepareOutput(res);
		prepareGenerator();
		ComplexDataType complexData = null;
		
		
		
		try {
			// CHECKING IF STORE IS TRUE AND THEN PROCESSING.... SOMEHOW!
			// CREATING A COMPLEXVALUE	
			
			// in case encoding is NULL -or- empty -or- UTF-8
			// send plain text (XML or not) in response node
			// 
			// in case encoding is base64
			// send base64encoded (binary) data in node
			//
			// in case encoding is 
			// 
			InputStream stream = getGenerator().generate(getPayload(), getFormat());
			complexData = output.addNewData().addNewComplexData();
            if (getFormat().getMimeType().get().contains("xml") ||
                getFormat().getMimeType().get().contains("XML")) {
                complexData.set(XmlObject.Factory.parse(stream));
                stream.close();
			}else{
				DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				Document document = builder.newDocument();
                String text;
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    IOUtils.copy(stream, baos);
                    stream.close();
                    text = baos.toString();
                }
				Node dataNode = document.createTextNode(text);
				complexData.set(XmlObject.Factory.parse(dataNode));
			}
			
		} catch(RuntimeException | IOException e) {
			LOGGER.error(e.getMessage(), e);
			throw new ExceptionReport("Could not create Inline Complex Data from the process result", ExceptionReport.NO_APPLICABLE_CODE, e);
		} catch (XmlException e) {
			LOGGER.error(e.getMessage(), e);
			throw new ExceptionReport("Could not create Inline Complex Data from the process result. Check encoding (base64 for inline binary data or UTF-8 for XML based data)", ExceptionReport.NO_APPLICABLE_CODE, e);
		} catch (ParserConfigurationException e) {
			LOGGER.error(e.getMessage(), e);
			throw new ExceptionReport("Could not create Inline Base64 Complex Data from the process result", ExceptionReport.NO_APPLICABLE_CODE, e);
		}
        getFormat().encodeTo(complexData);
	}
	
	public void updateResponseForLiteralData(ExecuteResponseDocument res, String dataTypeReference){
		OutputDataType output = prepareOutput(res);
		String processValue = BasicXMLTypeFactory.getStringRepresentation(dataTypeReference, getPayload());
		LiteralDataType literalData = output.addNewData().addNewLiteralData();
		if (dataTypeReference != null) {
			literalData.setDataType(dataTypeReference);
		}
	    literalData.setStringValue(processValue);
		if(getPayload() instanceof AbstractLiteralDataBinding) {
            AbstractLiteralDataBinding abstractLiteralDataBinding
                    = (AbstractLiteralDataBinding) getPayload();
            String uom = abstractLiteralDataBinding.getUnitOfMeasurement();
            if (uom != null && !uom.isEmpty()) {
                literalData.setUom(uom);
            }
        }
    }
	
	public void updateResponseAsReference(ExecuteResponseDocument res, String reqID, String mimeType) throws ExceptionReport {
		prepareGenerator();
		OutputDataType output = prepareOutput(res);
		InputStream stream;
		
		OutputReferenceType outReference = output.addNewReference();
        getFormat().encodeTo(outReference);
		IDatabase db = DatabaseFactory.getDatabase();
		String storeID = reqID + "" + getId();
		
		try {
            stream = getGenerator().generate(getPayload(), getFormat());
		} catch (IOException e){
			LOGGER.error(e.getMessage(), e);
			throw new ExceptionReport("Error while generating Complex Data out of the process result", ExceptionReport.NO_APPLICABLE_CODE, e);
		}
		
		String storeReference = db.storeComplexValue(storeID, stream, COMPLEX_DATA_TYPE, mimeType);
		storeReference = storeReference.replace("#", "%23");
		outReference.setHref(storeReference);
		// MSS:  05-02-2009 changed default output type to text/xml to be certain that the calling application doesn't 
		// serve the wrong type as it is a reference in this case.
        setFormat(new Format("text/xml"));
	}
	
	private OutputDataType prepareOutput(ExecuteResponseDocument res){
		OutputDataType output = res.getExecuteResponse().getProcessOutputs().addNewOutput();
		CodeType identifierCode = output.addNewIdentifier();
		identifierCode.setStringValue(getId());
		output.setTitle(title);
		return output;	
	}

	public void updateResponseForBBOXData(ExecuteResponseDocument res, IData obj) {
		Envelope bbox = (Envelope) obj.getPayload();
		OutputDataType output = prepareOutput(res);
		BoundingBoxType bboxData = output.addNewData().addNewBoundingBoxData();
		if (bbox.getCoordinateReferenceSystem() != null &&
            !bbox.getCoordinateReferenceSystem().getIdentifiers().isEmpty()){
			bboxData.setCrs(bbox.getCoordinateReferenceSystem().getIdentifiers().iterator().next().toString());
		}
		List<Double> lowerCornerList = Doubles.asList(bbox.getLowerCorner().getCoordinate());
		List<Double> upperCornerList = Doubles.asList(bbox.getUpperCorner().getCoordinate());
		
		bboxData.setLowerCorner(lowerCornerList);
		bboxData.setUpperCorner(upperCornerList);
		bboxData.setDimensions(BigInteger.valueOf(bbox.getDimension()));
	}
}
