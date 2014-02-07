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

package org.n52.wps.server.handler;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.apache.commons.io.IOUtils;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.WPSConstants;
import org.n52.wps.server.request.CapabilitiesRequest;
import org.n52.wps.server.request.DescribeProcessRequest;
import org.n52.wps.server.request.ExecuteRequest;
import org.n52.wps.server.request.Request;
import org.n52.wps.server.request.RetrieveResultRequest;
import org.n52.wps.server.response.ExecuteResponse;
import org.n52.wps.server.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * This class accepts client requests, determines its type and then schedules
 * the {@link ExecuteRequest}'s for execution. The request is executed for a
 * short time, within the client will be served with an immediate result. If the
 * time runs out, the client will be served with a reference to the future
 * result. The client can come back later to retrieve the result. Uses
 * "computation_timeout_seconds" from wps.properties
 * 
 * @author Timon ter Braak
 */
public class RequestHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestHandler.class);
	protected static RequestExecutor pool = new RequestExecutor();
	protected OutputStream os;
	protected String responseMimeType;
	protected Request req;
	
	// Empty constructor due to classes which extend the RequestHandler
	protected RequestHandler() {
	}

	/**
	 * Handles requests of type HTTP_GET (currently capabilities and
	 * describeProcess). A Map is used to represent the client input.
	 * 
	 * @param params
	 *            The client input
	 * @param os
	 *            The OutputStream to write the response to.
	 * @throws ExceptionReport
	 *             If the requested operation is not supported
	 */
	public RequestHandler(Map<String, String[]> params, OutputStream os)
			throws ExceptionReport {
		this.os = os;
		//sleepingTime is 0, by default.
		/*if(WPSConfiguration.getInstance().exists(PROPERTY_NAME_COMPUTATION_TIMEOUT)) {
			this.sleepingTime = Integer.parseInt(WPSConfiguration.getInstance().getProperty(PROPERTY_NAME_COMPUTATION_TIMEOUT));
		}
		String sleepTime = WPSConfig.getInstance().getWPSConfig().getServer().getComputationTimeoutMilliSeconds();
		*/
		
		
		CaseInsensitiveMap ciMap = new CaseInsensitiveMap(params);
		
		/*
		 * check if service parameter is present and equals "WPS"
		 * otherwise an ExceptionReport will be thrown
		 */
		String serviceType = Request.getMapValue(WPSConstants.PARAMETER_SERVICE, ciMap, true);
		
		if(!serviceType.equalsIgnoreCase(WPSConstants.WPS_SERVICE_TYPE)){
			throw new ExceptionReport("Parameter <service> is not correct, expected: WPS, got: " + serviceType, 
					ExceptionReport.INVALID_PARAMETER_VALUE, WPSConstants.PARAMETER_SERVICE);
		}

		/*
		 * check language. if not supported, return ExceptionReport
		 * Fix for https://bugzilla.52north.org/show_bug.cgi?id=905
		 */
		String language = Request.getMapValue(WPSConstants.PARAMETER_LANGUAGE, ciMap, false);
		
		if(language != null){
			Request.checkLanguageSupported(language);
		}

		// get the request type
		String requestType = Request.getMapValue(WPSConstants.PARAMETER_REQUEST, ciMap, true);
		
		if (requestType.equalsIgnoreCase(WPSConstants.GET_CAPABILITIES_REQUEST)) {
			this.req = new CapabilitiesRequest(ciMap);
		}  else if (requestType.equalsIgnoreCase(WPSConstants.DESCRIBE_PROCESS_REQUEST)) {
			this.req = new DescribeProcessRequest(ciMap);
		} else if (requestType.equalsIgnoreCase(WPSConstants.EXECUTE_REQUEST)) {
			this.req = new ExecuteRequest(ciMap);
			setResponseMimeType((ExecuteRequest)req);
		}  else if (requestType.equalsIgnoreCase(WPSConstants.RETRIEVE_RESULT_REQUEST)) {
			this.req = new RetrieveResultRequest(ciMap);
		}  else {
			throw new ExceptionReport(
					"The requested Operation is not supported or not applicable to the specification: "
							+ requestType,
					ExceptionReport.OPERATION_NOT_SUPPORTED, requestType);
		}
	}
   

	/**
	 * Handles requests of type HTTP_POST (currently executeProcess). A Document
	 * is used to represent the client input. This Document must first be parsed
	 * from an InputStream.
	 * 
	 * @param is
	 *            The client input
	 * @param os
	 *            The OutputStream to write the response to.
	 * @throws ExceptionReport
	 */
	public RequestHandler(InputStream is, OutputStream os)
			throws ExceptionReport {
		String nodeName, localName, nodeURI, version = null;
		Document doc;
		this.os = os;
		
		boolean isCapabilitiesNode = false;
		
		try {
			System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
                               "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
		
			DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
			fac.setNamespaceAware(true);

			// parse the InputStream to create a Document
			doc = fac.newDocumentBuilder().parse(is);
			
			// Get the first non-comment child.
			Node child = doc.getFirstChild();
			while(child.getNodeName().compareTo("#comment")==0) {
				child = child.getNextSibling();
			}
			nodeName = child.getNodeName();
			localName = child.getLocalName();
			nodeURI = child.getNamespaceURI();
			Node versionNode = child.getAttributes().getNamedItem(WPSConstants.AN_VERSION);
			
			/*
			 * check for service parameter. this has to be present for all requests
			 */
			Node serviceNode = child.getAttributes().getNamedItem(WPSConstants.AN_SERVICE);
			
			if(serviceNode == null){
				throw new ExceptionReport("Parameter <service> not specified.", ExceptionReport.MISSING_PARAMETER_VALUE, WPSConstants.PARAMETER_SERVICE);
			}else{
				if(!serviceNode.getNodeValue().equalsIgnoreCase(WPSConstants.WPS_SERVICE_TYPE)){
					throw new ExceptionReport("Parameter <service> not specified.", ExceptionReport.INVALID_PARAMETER_VALUE, WPSConstants.PARAMETER_SERVICE);
				}
			}
			
            isCapabilitiesNode = nodeName.toLowerCase().contains("capabilities");
			if(versionNode == null && !isCapabilitiesNode) {
				throw new ExceptionReport("Parameter <version> not specified.", ExceptionReport.MISSING_PARAMETER_VALUE, WPSConstants.PARAMETER_VERSION);
			}
			//TODO: I think this can be removed, as capabilities requests do not have a version parameter (BenjaminPross)
			if(!isCapabilitiesNode){
//				version = child.getFirstChild().getTextContent();//.getNextSibling().getFirstChild().getNextSibling().getFirstChild().getNodeValue();
				version = child.getAttributes().getNamedItem(WPSConstants.PARAMETER_VERSION).getNodeValue();
			}
			/*
			 * check language, if not supported, return ExceptionReport
			 * Fix for https://bugzilla.52north.org/show_bug.cgi?id=905
			 */
			Node languageNode = child.getAttributes().getNamedItem(WPSConstants.PARAMETER_LANGUAGE);
			if(languageNode != null){
				String language = languageNode.getNodeValue();
				Request.checkLanguageSupported(language);
			}
		} catch (SAXException e) {
			throw new ExceptionReport(
					"There went something wrong with parsing the POST data: "
							+ e.getMessage(),
					ExceptionReport.NO_APPLICABLE_CODE, e);
		} catch (IOException e) {
			throw new ExceptionReport(
					"There went something wrong with the network connection.",
					ExceptionReport.NO_APPLICABLE_CODE, e);
		} catch (ParserConfigurationException e) {
			throw new ExceptionReport(
					"There is a internal parser configuration error",
					ExceptionReport.NO_APPLICABLE_CODE, e);
		}
		//Fix for Bug 904 https://bugzilla.52north.org/show_bug.cgi?id=904
		if(!isCapabilitiesNode && version == null) {
			throw new ExceptionReport("Parameter <version> not specified." , ExceptionReport.MISSING_PARAMETER_VALUE, WPSConstants.PARAMETER_VERSION);
		}
		if(!isCapabilitiesNode && !version.equals(WPSConstants.WPS_SERVICE_VERSION)) {
			throw new ExceptionReport("Version not supported." , ExceptionReport.INVALID_PARAMETER_VALUE, WPSConstants.PARAMETER_VERSION);
		}
		// get the request type
		if (nodeURI.equals(WPSConstants.NS_WPS) && localName.equals(WPSConstants.EXECUTE_REQUEST)) {
			req = new ExecuteRequest(doc);
			setResponseMimeType((ExecuteRequest)req);
		}else if (nodeURI.equals(WPSConstants.NS_WPS) && localName.equals(WPSConstants.GET_CAPABILITIES_REQUEST)){
			req = new CapabilitiesRequest(doc);
			this.responseMimeType = WPSConstants.MIME_TYPE_TEXT_XML;
		} else if (nodeURI.equals(WPSConstants.NS_WPS) && localName.equals(WPSConstants.DESCRIBE_PROCESS_REQUEST)) {
			req = new DescribeProcessRequest(doc);
			this.responseMimeType = WPSConstants.MIME_TYPE_TEXT_XML;
			
		}  else if(!localName.equals(WPSConstants.EXECUTE_REQUEST)){
			throw new ExceptionReport("The requested Operation not supported or not applicable to the specification: "
					+ nodeName, ExceptionReport.OPERATION_NOT_SUPPORTED, localName);
		}
		else if(nodeURI.equals(WPSConstants.NS_WPS)) {
			throw new ExceptionReport("specified namespace is not supported: "
					+ nodeURI, ExceptionReport.INVALID_PARAMETER_VALUE);
		}
	}

	/**
	 * Handle a request after its type is determined. The request is scheduled
	 * for execution. If the server has enough free resources, the client will
	 * be served immediately. If time runs out, the client will be asked to come
	 * back later with a reference to the result.
	 * 
	 * @throws ExceptionReport
	 */
	public void handle() throws ExceptionReport {
		Response resp = null;
		if(req ==null){
			throw new ExceptionReport("Internal Error","");
		}
		if (req instanceof ExecuteRequest) {
			// cast the request to an executerequest
			ExecuteRequest execReq = (ExecuteRequest) req;
			
			execReq.updateStatusAccepted();
			
			ExceptionReport exceptionReport = null;
			try {
				if (execReq.isStoreResponse()) {
					resp = new ExecuteResponse(execReq);
					InputStream is = resp.getAsStream();
					IOUtils.copy(is, os);
					is.close();
                    pool.submit(execReq);
					return;
				}
				try {
					// retrieve status with timeout enabled
					try {
						resp = pool.submit(execReq).get();
					}
					catch (ExecutionException ee) {
						LOGGER.warn("exception while handling ExecuteRequest.");
						// the computation threw an error
						// probably the client input is not valid
						if (ee.getCause() instanceof ExceptionReport) {
							exceptionReport = (ExceptionReport) ee
									.getCause();
						} else {
							exceptionReport = new ExceptionReport(
									"An error occurred in the computation: "
											+ ee.getMessage(),
									ExceptionReport.NO_APPLICABLE_CODE);
						}
					} catch (InterruptedException ie) {
						LOGGER.warn("interrupted while handling ExecuteRequest.");
						// interrupted while waiting in the queue
						exceptionReport = new ExceptionReport(
								"The computation in the process was interrupted.",
								ExceptionReport.NO_APPLICABLE_CODE);
					}
				} finally {
					if (exceptionReport != null) {
						LOGGER.debug("ExceptionReport not null: " + exceptionReport.getMessage());
						// NOT SURE, if this exceptionReport is also written to the DB, if required... test please!
						throw exceptionReport;
					}
					// send the result to the outputstream of the client.
				/*	if(((ExecuteRequest) req).isQuickStatus()) {
						resp = new ExecuteResponse(execReq);
					}*/
					else if(resp == null) {
						LOGGER.warn("null response handling ExecuteRequest.");
						throw new ExceptionReport("Problem with handling threads in RequestHandler", ExceptionReport.NO_APPLICABLE_CODE);
					}
					if(!execReq.isStoreResponse()) {
						InputStream is = resp.getAsStream();
						IOUtils.copy(is, os);
						is.close();
						LOGGER.info("Served ExecuteRequest.");
					}
				}
			} catch (RejectedExecutionException ree) {
                LOGGER.warn("exception handling ExecuteRequest.", ree);
				// server too busy?
				throw new ExceptionReport(
						"The requested process was rejected. Maybe the server is flooded with requests.",
						ExceptionReport.SERVER_BUSY);
			} catch (Exception e) {
                LOGGER.error("exception handling ExecuteRequest.", e);
                if (e instanceof ExceptionReport) {
                    throw (ExceptionReport)e;
                }
                throw new ExceptionReport("Could not read from response stream.", ExceptionReport.NO_APPLICABLE_CODE);
			}
		} else {
			// for GetCapabilities and DescribeProcess:
			resp = req.call();
			try {
				InputStream is = resp.getAsStream();
				IOUtils.copy(is, os);
				is.close();
			} catch (IOException e) {
				throw new ExceptionReport("Could not read from response stream.", ExceptionReport.NO_APPLICABLE_CODE);
			}
			
		}
	}
	
	protected void setResponseMimeType(ExecuteRequest req) {
		if(req.isRawData()){
			responseMimeType = req.getExecuteResponseBuilder().getMimeType();
		}else{
			responseMimeType = WPSConstants.MIME_TYPE_TEXT_XML;
		}
		
		
	}

	public String getResponseMimeType(){
		if(responseMimeType == null){
			return WPSConstants.MIME_TYPE_TEXT_XML;
		}
		return responseMimeType.toLowerCase();
	}
	
	
}



