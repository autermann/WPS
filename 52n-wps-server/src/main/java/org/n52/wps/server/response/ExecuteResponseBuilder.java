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
import net.opengis.wps.x100.DocumentOutputDefinitionType;
import net.opengis.wps.x100.ExecuteResponseDocument;
import net.opengis.wps.x100.ExecuteResponseDocument.ExecuteResponse.ProcessOutputs;
import net.opengis.wps.x100.OutputDefinitionType;
import net.opengis.wps.x100.OutputDescriptionType;
import net.opengis.wps.x100.ProcessBriefType;
import net.opengis.wps.x100.ProcessDescriptionType;
import net.opengis.wps.x100.ResponseFormType;
import net.opengis.wps.x100.StatusType;

import org.apache.xmlbeans.XmlCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.n52.wps.ServerDocument.Server;
import org.n52.wps.commons.Format;
import org.n52.wps.commons.OwsCodeType;
import org.n52.wps.commons.OwsLanguageString;
import org.n52.wps.commons.WPSConfig;
import org.n52.wps.io.LiteralDataFactory;
import org.n52.wps.io.data.IBBOXData;
import org.n52.wps.io.data.IComplexData;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.ILiteralData;
import org.n52.wps.server.CapabilitiesConfiguration;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.InvalidParameterValueException;
import org.n52.wps.server.NoApplicableCodeException;
import org.n52.wps.server.RepositoryManager;
import org.n52.wps.server.RetrieveResultServlet;
import org.n52.wps.server.WPSConstants;
import org.n52.wps.server.request.ExecuteRequest;
import org.n52.wps.server.response.execute.BoundingBoxProcessOutput;
import org.n52.wps.server.response.execute.ComplexProcessOutput;
import org.n52.wps.server.response.execute.LiteralProcessOutput;
import org.n52.wps.server.response.execute.ProcessOutput;
import org.n52.wps.server.response.execute.RawData;
import org.n52.wps.server.response.execute.ReferenceProcessOutput;
import org.n52.wps.util.XMLBeansHelper;

import com.google.common.base.Strings;

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
	private final String identifier;
	private final ExecuteRequest request;
	private final ExecuteResponseDocument document;
	private final ProcessDescriptionType description;
	private final Calendar creationTime;
    private final ResponseFormType xbResponseForm;
    private final ExecuteResponseDocument.ExecuteResponse xbExecuteResponse;
    private String statusLocation;
    private RawData rawData = null;

	public ExecuteResponseBuilder(ExecuteRequest request) throws ExceptionReport{
		this.request = request;
        this.identifier = this.request.getExecute().getIdentifier().getStringValue().trim();
        this.xbResponseForm = this.request.getExecute().getResponseForm();

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
        if (this.description.getMetadataArray().length > 0) {
            xbProcess.setMetadataArray(this.description.getMetadataArray());
        }
        if (this.description.getProfileArray().length > 0) {
            xbProcess.setProfileArray(this.description.getProfileArray());
        }
        if (this.description.getWSDL() != null) {
            xbProcess.setWSDL(this.description.getWSDL());
        }
        if (this.description.getProcessVersion() != null) {
            xbProcess.setProcessVersion(this.description.getProcessVersion());
        }

        this.creationTime = Calendar.getInstance();
	}

    private void setSchemaLocation(ExecuteResponseDocument document) {
        XmlCursor c = document.newCursor();
        c.toFirstChild();
        c.toLastAttribute();
        c.setAttributeText(WPSConstants.QN_SCHEMA_LOCATION,
                           WPSConstants.NS_WPS + " " +
                           WPSConstants.SCHEMA_LOCATION_WPS_EXECUTE_RESPONSE);
        c.dispose();
    }

    private String getServiceInstanceURL() {
        return CapabilitiesConfiguration.ENDPOINT_URL + "?service=WPS&request=GetCapabilities";
    }

	public void update() throws ExceptionReport {
		if (this.xbExecuteResponse.getStatus().isSetProcessSucceeded()) {
            updateStatusSucceeded();
        } else if (this.request.isStoreResponse()) {
            this.xbExecuteResponse.setStatusLocation(getStatusLocation());
        }
	}

    private void updateStatusSucceeded() throws ExceptionReport {
        this.xbExecuteResponse.addNewProcessOutputs();
        if (this.request.isRawData()) {
            this.rawData = createRawOutput();
        } else if (this.request.hasResponseDocument()) {
            for (DocumentOutputDefinitionType output : getRequestedOutputs()) {
                OutputDescriptionType desc = getOutputDescription(
                        output.getIdentifier().getStringValue());
                update(createProcessOutput(desc, output));
            }
        } else {
            // THIS IS A WORKAROUND AND ACTUALLY NOT COMPLIANT TO THE SPEC.
            LOGGER.info("OutputDefinitions are not stated explicitly in request");
            for (OutputDescriptionType desc : getDefinedOutputs()) {
                update(createProcessOutput(desc, null));
            }
        }
    }

    private OutputDescriptionType[] getDefinedOutputs() {
        return this.description.getProcessOutputs().getOutputArray();
    }

    private DocumentOutputDefinitionType[] getRequestedOutputs() {
        return this.xbResponseForm.getResponseDocument().getOutputArray();
    }

    private OutputDefinitionType getRawDataOutput() {
        return this.request.getExecute().getResponseForm().getRawDataOutput();
    }

    private void update(ProcessOutput processOutput) throws ExceptionReport {
        ProcessOutputs xbProcessOutputs = this.xbExecuteResponse.getProcessOutputs();
        if (xbProcessOutputs == null) {
            xbProcessOutputs = this.xbExecuteResponse.addNewProcessOutputs();
        }
        processOutput.encodeTo(xbProcessOutputs.addNewOutput());
    }


    private ProcessOutput createProcessOutput(OutputDescriptionType desc,
                                              DocumentOutputDefinitionType request) throws ExceptionReport {
        OwsCodeType id = OwsCodeType.of(desc.getIdentifier());
        OwsLanguageString title = OwsLanguageString.of(desc.getTitle());
        OwsLanguageString abstrakt = OwsLanguageString.of(desc.getAbstract());
        IData data = this.request.getAttachedResult().get(id.getValue());
        switch (OutputType.of(desc)) {
            case COMPLEX:
                IComplexData complexData = (IComplexData) data;
                if (request == null) {
                     return new ComplexProcessOutput(id, title, abstrakt, complexData, Format.getDefault(desc));
                } else if (request.getAsReference()) {
                    return new ReferenceProcessOutput(id, title, abstrakt, complexData, getFormat(request));
                } else {
                    return new ComplexProcessOutput(id, title, abstrakt, complexData, getFormat(request));
                }
            case LITERAL:
                ILiteralData literalData = (ILiteralData) data;
                 DomainMetadataType dmt = desc.getLiteralOutput().getDataType();
                String dataType = null;
                if (dmt != null) {
                    dataType = dmt.getReference();
                }
                if (dataType == null) {
                    dataType = LiteralDataFactory.getTypeforBindingType(literalData.getClass());
                }
                return new LiteralProcessOutput(id, title, abstrakt, literalData, dataType);
            case BBOX:
                IBBOXData bboxData = (IBBOXData) data;
                return new BoundingBoxProcessOutput(id, title, abstrakt, bboxData);
            default:
                throw new RuntimeException("Incomplete enum switch!");
        }
    }

    private RawData createRawOutput() throws ExceptionReport {
        OutputDefinitionType rawDataOutput = this.xbResponseForm.getRawDataOutput();
        String id = rawDataOutput.getIdentifier().getStringValue();
        IData data = this.request.getAttachedResult().get(id);
        switch (OutputType.of(getOutputDescription(id))) {
            case COMPLEX:
                return RawData.of((IComplexData) data, getFormat(rawDataOutput));
            case LITERAL:
                return RawData.of((ILiteralData) data);
            case BBOX:
                return RawData.of((IBBOXData) data);
            default:
                throw new RuntimeException("Incomplete enum switch!");
        }
    }

    private OutputDescriptionType getOutputDescription(String id) throws ExceptionReport {
        OutputDescriptionType desc = XMLBeansHelper.findOutputByID(id, getDefinedOutputs());
        if (desc == null) {
            throw new InvalidParameterValueException("Could not find the output id %s", id);
        }
        return desc;
    }

    private String getStatusLocation() {
        if (this.statusLocation == null) {
            Server server = WPSConfig.getInstance().getWPSConfig().getServer();
            this.statusLocation = String.format("http://%s:%s/%s?id=%s",
                                           server.getHostname(),
                                           server.getHostport(),
                                           RetrieveResultServlet.SERVLET_PATH,
                                           this.request.getUniqueId().toString());
        }
        return this.statusLocation;
    }

    private Format getFormat(OutputDefinitionType definition) throws ExceptionReport {
        return Format.of(definition).withMimeType(getMimeType(definition));
    }

	public String getMimeType() throws ExceptionReport {
        if (this.request.isRawData()) {
            return getMimeType(getRawDataOutput());
        } else if (this.request.hasResponseDocument()) {
            return WPSConstants.MIME_TYPE_TEXT_XML;
        } else {
            return WPSConstants.MIME_TYPE_TEXT_XML;
        }
    }

	public String getMimeType(OutputDefinitionType definition) throws ExceptionReport {
        if (definition == null) {
            return getMimeType();
        }
        final String id = definition.getIdentifier().getStringValue();
        final OutputDescriptionType desc = getOutputDescription(id);
        switch (OutputType.of(desc)) {
            case BBOX:
                return WPSConstants.MIME_TYPE_TEXT_XML;
            case LITERAL:
                return WPSConstants.MIME_TYPE_TEXT_PLAIN;
            case COMPLEX:
                if (Strings.emptyToNull(definition.getMimeType()) != null) {
                    return definition.getMimeType();
                }
                String mimeType = desc.getComplexOutput().getDefault().getFormat().getMimeType();
                LOGGER.warn("Using default mime type: {} for input: {}", mimeType, id);
                return mimeType;
            default:
                throw new RuntimeException("Unknown OutputType " + desc);
        }
    }

    public InputStream getAsStream() throws ExceptionReport {
        if (this.request.isRawData() && this.rawData != null) {
            return rawData.getAsStream();
        }
        if (this.request.isStoreResponse()) {
            this.document.getExecuteResponse().setStatusLocation(getStatusLocation());
        }
        try {
            return this.document.newInputStream(XMLBeansHelper.getXmlOptions());
        } catch (Exception e) {
            throw new NoApplicableCodeException("Error generating XML stream").causedBy(e);
        }
    }

	public void setStatus(StatusType status) {
        //workaround, should be generated either at the creation of the document or when the process has been finished.
        status.setCreationTime(this.creationTime);
        this.document.getExecuteResponse().setStatus(status);
    }

    private enum OutputType {
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
}

