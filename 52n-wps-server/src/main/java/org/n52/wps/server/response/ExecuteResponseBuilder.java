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

import java.io.InputStream;
import java.util.Calendar;

import net.opengis.ows.x11.DomainMetadataType;
import net.opengis.ows.x11.LanguageStringType;
import net.opengis.wps.x100.DocumentOutputDefinitionType;
import net.opengis.wps.x100.ExecuteResponseDocument;
import net.opengis.wps.x100.OutputDefinitionType;
import net.opengis.wps.x100.OutputDescriptionType;
import net.opengis.wps.x100.ProcessBriefType;
import net.opengis.wps.x100.ProcessDescriptionType;
import net.opengis.wps.x100.ResponseDocumentType;
import net.opengis.wps.x100.ResponseFormType;
import net.opengis.wps.x100.StatusType;

import org.apache.xmlbeans.XmlCursor;
import org.n52.wps.commons.Format;
import org.n52.wps.io.data.IData;
import org.n52.wps.server.CapabilitiesConfiguration;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.InvalidParameterValueException;
import org.n52.wps.server.RepositoryManager;
import org.n52.wps.server.WPSConstants;
import org.n52.wps.server.database.DatabaseFactory;
import org.n52.wps.server.request.ExecuteRequest;
import org.n52.wps.util.XMLBeansHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WPS Execute operation response. By default, this XML document is delivered to
 * the client in response to an Execute request. If "status" is "false" in the
 * Execute operation request, this document is normally returned when process
 * execution has been completed.
 * If "status" in the Execute request is "true", this response shall be returned
 * as soon as the Execute request has been accepted for processing. In this
 * case, the same XML document is also made available as a web-accessible
 * resource from the URL identified in the statusLocation, and the WPS server
 * shall repopulate it once the process has completed. It may repopulate it on
 * an ongoing basis while the process is executing.
 * However, the response to an Execute request will not include this element in
 * the special case where the output is a single complex value result and the
 * Execute request indicates that "store" is "false".
 * Instead, the server shall return the complex result (e.g., GIF image or GML)
 * directly, without encoding it in the ExecuteResponse. If processing fails in
 * this special case, the normal ExecuteResponse shall be sent, with the error
 * condition indicated. This option is provided to simplify the programming
 * required for simple clients and for service chaining.
 *
 * @author Timon ter Braak
 *
 */
public class ExecuteResponseBuilder {
	private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteResponseBuilder.class);
    private static final String SCHEMA_LOCATION_WPS_EXECUTE_RESPONSE
            = "http://schemas.opengis.net/wps/1.0.0/wpsExecute_response.xsd";

	private final String identifier;
	private final ExecuteRequest request;
	private final ExecuteResponseDocument document;
	private RawData rawDataHandler = null; 
	private final ProcessDescriptionType description;
	private final Calendar creationTime;
    private final ResponseFormType xbResponseForm;
    private final ResponseDocumentType xbResponseDocument;
    private final ExecuteResponseDocument.ExecuteResponse xbExecuteResponse;

    private final ResponseEncoding encoding;
	
	public ExecuteResponseBuilder(ExecuteRequest request) throws ExceptionReport{
		this.request = request;
        this.identifier = this.request.getExecute().getIdentifier().getStringValue().trim();
        this.xbResponseForm = this.request.getExecute().getResponseForm();

        if (request.isRawData()) {
            encoding = ResponseEncoding.RAW;
        } else if (request.hasResponseDocument()) {
            encoding = ResponseEncoding.INLINE;
        } else {
            encoding = ResponseEncoding.UNSPECIFIED;
        }

        this.xbResponseDocument = encoding == ResponseEncoding.INLINE ? null
                                  : this.xbResponseForm.getResponseDocument();
        
        this.description = RepositoryManager.getInstance().getProcessDescription(this.identifier);
        
        if (this.description == null) {
            throw new RuntimeException("Error while accessing the process description for " +
                                       identifier);
        }
		this.document = ExecuteResponseDocument.Factory.newInstance();
        this.xbExecuteResponse = this.document.addNewExecuteResponse();
        setSchemaLocation(this.document);

        this.xbExecuteResponse.setServiceInstance(getServiceInstanceURL());
        this.xbExecuteResponse.setLang(WPSConstants.DEFAULT_LANGUAGE);
		this.xbExecuteResponse.setService(WPSConstants.WPS_SERVICE_TYPE);
		this.xbExecuteResponse.setVersion(WPSConstants.WPS_SERVICE_VERSION);
        
        if (this.request.isLineage()) {
            this.xbExecuteResponse.setDataInputs(request.getExecute().getDataInputs());
        }

        ProcessBriefType xbProcess = xbExecuteResponse.addNewProcess();
        xbProcess.setIdentifier(this.description.getIdentifier());
        if (this.description.getTitle()!=null) {
            xbProcess.setTitle(this.description.getTitle());
        }
        if (this.description.getAbstract() != null) {
            xbProcess.setAbstract(this.description.getAbstract());
        }
        if (this.description.getMetadataArray().length>0) {
            xbProcess.setMetadataArray(this.description.getMetadataArray());
        }
        if (this.description.getProfileArray().length>0) {
            xbProcess.setProfileArray(this.description.getProfileArray());
        }
        if (this.description.getWSDL() != null) {
            xbProcess.setWSDL(this.description.getWSDL());
        }
        if (this.description.getProcessVersion() != null) {
            xbProcess.setProcessVersion(this.description.getProcessVersion());
        }

        			// the response only include dataInputs, if the property is set to true;
        this.creationTime = Calendar.getInstance();
	}

    private void setSchemaLocation(ExecuteResponseDocument document) {
        XmlCursor c = document.newCursor();
        c.toFirstChild();
        c.toLastAttribute();
        c.setAttributeText(WPSConstants.QN_SCHEMA_LOCATION,
                           WPSConstants.NS_WPS + " " +
                           SCHEMA_LOCATION_WPS_EXECUTE_RESPONSE);
        c.dispose();
    }

    private String getServiceInstanceURL() {
        return CapabilitiesConfiguration.ENDPOINT_URL +
               "?request=GetCapabilities&service=WPS";
    }
	
	public void update() throws ExceptionReport {
		if (this.xbExecuteResponse.getStatus().isSetProcessSucceeded()) {
            updateStatusSucceeded();
        } else if (request.isStoreResponse()) {
            this.xbExecuteResponse.setStatusLocation(getStatusLocation());
        }
	}

    private void updateStatusSucceeded() throws ExceptionReport {
        this.xbExecuteResponse.addNewProcessOutputs();
        switch (encoding) {
            case INLINE:
                updateResponseDocument();
                break;
            case RAW:
                updateRawData();
                break;
            case UNSPECIFIED:
                updateNoResponseForm();
                break;

        }
    }
    
    private void updateNoResponseForm() throws ExceptionReport {
        // THIS IS A WORKAROUND AND ACTUALLY NOT COMPLIANT TO THE SPEC.
        LOGGER.info("OutputDefinitions are not stated explicitly in request");
        for (OutputDescriptionType xbOutputDescription :
                description.getProcessOutputs().getOutputArray()) {
            updateNoResponseForm(xbOutputDescription);
        }
    }

    private void updateNoResponseForm(OutputDescriptionType description)
            throws ExceptionReport {
        String id = description.getIdentifier().getStringValue();
        LanguageStringType title = description.getTitle();
        IData data = request.getAttachedResult().get(id);
        switch(OutputType.of(description)) {
            case BBOX:
                generateInlineBBOXDataOutput(data, id, title);
                break;
            case COMPLEX:
                Format format = Format.getDefault(description);
                generateInlineComplexDataOutput(data, id, format, title);
                break;
            case LITERAL:
                String type = description.getLiteralOutput().getDataType().getReference();
                generateInlineLiteralDataOutput(data, id, title, type);
                break;
        }
    }

    private void updateResponseDocument() throws ExceptionReport {
        for (DocumentOutputDefinitionType requestedOutput : this.xbResponseDocument.getOutputArray()) {
            updateResponseDocument(requestedOutput);
        }
    }

    private void updateResponseDocument(DocumentOutputDefinitionType requestedOutput)
            throws ExceptionReport {
        String id = requestedOutput.getIdentifier().getStringValue();
        OutputDescriptionType desc = XMLBeansHelper.findOutputByID(id, description.getProcessOutputs().getOutputArray());
        if (desc == null) {
            throw new InvalidParameterValueException("Could not find the output id %s", id);
        }

        IData data = request.getAttachedResult().get(id);
        LanguageStringType title = desc.getTitle();
        switch(OutputType.of(desc)) {
            case COMPLEX:
                Format format = getFormat(requestedOutput);
                if (requestedOutput.getAsReference()) {
                    generateReferenceComplexDataOutput(data, id, format, title);
                } else {
                    generateInlineComplexDataOutput(data, id, format, title);
                }
                break;
            case LITERAL:
                DomainMetadataType dataType = desc.getLiteralOutput().getDataType();
                String reference = dataType != null ? dataType.getReference() : null;
                generateInlineLiteralDataOutput(data, id, title, reference);
                break;
            case BBOX:
                generateInlineBBOXDataOutput(data, id, title);
                break;
        }
    }

    private void updateRawData() throws ExceptionReport {
        OutputDescriptionType[] outputDescs = description.getProcessOutputs().getOutputArray();
        OutputDefinitionType rawDataOutput = xbResponseForm.getRawDataOutput();
        String id = rawDataOutput.getIdentifier().getStringValue();
        OutputDescriptionType desc = XMLBeansHelper.findOutputByID(id, outputDescs);
        IData data = request.getAttachedResult().get(id);
        switch (OutputType.of(desc)) {
            case COMPLEX:
                Format format = getFormat(rawDataOutput);
                generateRawComplexDataOutput(data, id, format);
                break;
            case LITERAL:
                DomainMetadataType dataType = desc.getLiteralOutput().getDataType();
                String reference = dataType != null ? dataType.getReference(): null;
                generateRawLiteralDataOutput(data, reference);
                break;
            case BBOX:
                generateRawBBOXDataOutput(data, id);
                break;
        }
    }

    private String getStatusLocation() {
        return DatabaseFactory.getDatabase().generateRetrieveResultURL((request.getUniqueId()).toString());
    }

    private Format getFormat(OutputDefinitionType definition) {
        String mimeType = getMimeType(definition);
        String schema = ExecuteResponseBuilder.getSchema(definition);
        String encoding = ExecuteResponseBuilder.getEncoding(definition);
        return new Format(mimeType, encoding, schema);
    }

	public String getMimeType() {
        switch (encoding) {
            case INLINE:
                return WPSConstants.MIME_TYPE_TEXT_XML;
            case UNSPECIFIED:
                return WPSConstants.MIME_TYPE_TEXT_XML;
            case RAW:
                OutputDefinitionType raw = request.getExecute().getResponseForm().getRawDataOutput();
                String id = raw.getIdentifier().getStringValue();
                OutputDescriptionType outputDes = getOutputDescription(id);
                if (raw.getMimeType() != null && !raw.getMimeType().isEmpty()) {
                    return raw.getMimeType();
                } 
                switch(OutputType.of(outputDes)) {
                    case BBOX:
                        return WPSConstants.MIME_TYPE_TEXT_XML;
                    case LITERAL:
                        return WPSConstants.MIME_TYPE_TEXT_PLAIN;
                    case COMPLEX:
                        String mimeType = outputDes.getComplexOutput().getDefault().getFormat().getMimeType();
                        LOGGER.warn("Using default mime type: {} for input: {}", mimeType, id);
                        return mimeType;
                    default:
                        throw new RuntimeException("Unknown OutputType " + outputDes);
                }
            default:
                throw new RuntimeException("Unknown Encoding " + encoding);
        }
    }

	public String getMimeType(OutputDefinitionType def) {
        if (def == null) {
            return getMimeType();
        }

        final String id = def.getIdentifier().getStringValue();
        final OutputDescriptionType outputDes = getOutputDescription(id);
        final OutputType type = OutputType.of(outputDes);

        switch (type) {
            case BBOX:
                return WPSConstants.MIME_TYPE_TEXT_XML;
            case LITERAL:
                return WPSConstants.MIME_TYPE_TEXT_PLAIN;
            case COMPLEX:
                if (def.getMimeType() != null && !def.getMimeType().isEmpty()) {
                    return def.getMimeType();
                }
                String mt = outputDes.getComplexOutput().getDefault().getFormat().getMimeType();
                LOGGER.warn("Using default mime type: {} for input: {}", mt,id);
                return mt;
            default:
                throw new RuntimeException("Unknown OutputType " + outputDes);
        }
    }

    private OutputDescriptionType getOutputDescription(String id) {
        for (OutputDescriptionType tmpOutputDes : description.getProcessOutputs().getOutputArray()) {
            if (id.equalsIgnoreCase(tmpOutputDes.getIdentifier().getStringValue())) {
                return tmpOutputDes;
            }
        }
        return null;
    }
	
    private void generateRawComplexDataOutput(IData obj, 
                                              String id,
                                              Format format) throws ExceptionReport {
        rawDataHandler = new RawData(obj, id, format, description);
    }

    private void generateInlineComplexDataOutput(IData obj,
                                                 String id,
                                                 Format format,
                                                 LanguageStringType title)
            throws ExceptionReport {
        OutputDataItem handler = new OutputDataItem(obj, id, format, title, description);
        handler.updateResponseForInlineComplexData(document);
    }

    private void generateReferenceComplexDataOutput(IData obj,
                                                    String id,
                                                    Format format,
                                                    LanguageStringType title)
            throws ExceptionReport {
        OutputDataItem handler = new OutputDataItem(obj, id, format, title, description);
        handler.updateResponseAsReference(document, request.getUniqueId().toString(), format.getMimeType().orNull());
    }

    private void generateInlineLiteralDataOutput(IData obj,
                                                 String id,
                                                 LanguageStringType title,
                                                 String dataTypeReference)
            throws ExceptionReport {
        OutputDataItem handler = new OutputDataItem(obj, id, null, title, description);
        handler.updateResponseForLiteralData(document, dataTypeReference);
    }

    private void generateRawLiteralDataOutput(IData obj, 
                                              String responseID)
            throws ExceptionReport {
        rawDataHandler = new RawData(obj, responseID, null, description);
    }

    private void generateInlineBBOXDataOutput(IData obj,
                                              String id,
                                              LanguageStringType title)
            throws ExceptionReport {
        OutputDataItem handler = new OutputDataItem(obj, id, null, title, description);
        handler.updateResponseForBBOXData(document, obj);
    }

    private void generateRawBBOXDataOutput(IData obj,
                                           String id)
            throws ExceptionReport {
        rawDataHandler = new RawData(obj, id, null, description);
    }

    public InputStream getAsStream() throws ExceptionReport {
        if (encoding == ResponseEncoding.RAW.RAW && rawDataHandler != null) {
            return rawDataHandler.getAsStream();
        }
        if (request.isStoreResponse()) {
            document.getExecuteResponse().setStatusLocation(getStatusLocation());
        }
        try {
            return document.newInputStream(XMLBeansHelper.getXmlOptions());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
	
	public void setStatus(StatusType status) {
        //workaround, should be generated either at the creation of the document or when the process has been finished.
        status.setCreationTime(creationTime);
        document.getExecuteResponse().setStatus(status);
    }

	/**
     * Returns the schema according to the given output description and type.
     */
    private static String getSchema(OutputDefinitionType def){
        return def != null ? def.getSchema() : null;
    }
	
	private static String getEncoding(OutputDefinitionType def) {
        return def != null ? def.getEncoding() : null;
    }

    enum OutputType {
        LITERAL,
        COMPLEX,
        BBOX;

        public static OutputType of(OutputDescriptionType description) {
            if (description.isSetLiteralOutput()) {
                return OutputType.LITERAL;
            } else if (description.isSetBoundingBoxOutput()) {
                return OutputType.BBOX;
            } else if (description.isSetComplexOutput()) {
                return OutputType.COMPLEX;
            } else {
                return null;
            }

        }
    }

    enum ResponseEncoding {
        INLINE,
        RAW,
        UNSPECIFIED
    }
}

