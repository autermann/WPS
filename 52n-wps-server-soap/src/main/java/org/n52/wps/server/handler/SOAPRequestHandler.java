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

import java.io.OutputStream;

import org.n52.wps.commons.WPSConfig;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.WPSConstants;
import org.n52.wps.server.request.CapabilitiesRequest;
import org.n52.wps.server.request.DescribeProcessRequest;
import org.n52.wps.server.request.ExecuteRequest;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class SOAPRequestHandler extends RequestHandler {

	/**
	 * Handles requests of type SOAPMessage (currently capabilities and
	 * describeProcess). A OMElement is used to represent the client input.
	 * 
	 * @param params
	 *            The client input
	 * @param outOM
	 *            The OMElement to write the response to.
	 * @throws ExceptionReport
	 *             If the requested operation is not supported
	 */

	public SOAPRequestHandler(Document inputDoc, OutputStream os)
			throws ExceptionReport {
		this.os = os;
		// sleepingTime is 0, by default.
		/*
		 * if(WPSConfiguration.getInstance().exists(PROPERTY_NAME_COMPUTATION_TIMEOUT)) {
		 * this.sleepingTime =
		 * Integer.parseInt(WPSConfiguration.getInstance().getProperty(PROPERTY_NAME_COMPUTATION_TIMEOUT)); }
		 */
		String nodeName, localName, nodeURI, version;
		String sleepTime = WPSConfig.getInstance().getWPSConfig().getServer()
				.getComputationTimeoutMilliSeconds();
		if (sleepTime == null || sleepTime.isEmpty()) {
			sleepTime = "5";
		}
		//this.sleepingTime = new Integer(sleepTime);

	
			Node child = inputDoc.getFirstChild();
			while (child.getNodeName().compareTo("#comment") == 0) {
				child = child.getNextSibling();
			}
			nodeName = child.getNodeName();
			localName = child.getLocalName();
			nodeURI = child.getNamespaceURI();

		// get the request type
		if (nodeURI.equals(WPSConstants.NS_WPS) && localName.equals(WPSConstants.EN_EXECUTE)) {
			
			Node versionNode = child.getAttributes().getNamedItem(WPSConstants.AN_VERSION);
			if (versionNode == null) {
				throw new ExceptionReport("No version parameter supplied.",
						ExceptionReport.MISSING_PARAMETER_VALUE);
			}
			version = child.getAttributes().getNamedItem(WPSConstants.AN_VERSION)
					.getNodeValue();

			if (version == null) {
				throw new ExceptionReport("version is null: ",
						ExceptionReport.MISSING_PARAMETER_VALUE);
			}
			if (!version.equals(WPSConstants.WPS_SERVICE_VERSION)) {
				throw new ExceptionReport("version is null: ",
						ExceptionReport.INVALID_PARAMETER_VALUE);
			}

			req = new ExecuteRequest(inputDoc);
			if (req instanceof ExecuteRequest) {
				setResponseMimeType((ExecuteRequest) req);
			} else {
				this.responseMimeType = WPSConstants.MIME_TYPE_TEXT_XML;
			}
		} else if (localName.equals(WPSConstants.EN_GET_CAPABILITIES)) {
			req = new CapabilitiesRequest(inputDoc);
		} else if (localName.equals(WPSConstants.EN_DESCRIBE_PROCESS)) {
			req = new DescribeProcessRequest(inputDoc);
		} else if (!localName.equals(WPSConstants.EN_EXECUTE)) {
			throw new ExceptionReport("specified operation is not supported: "
					+ nodeName, ExceptionReport.OPERATION_NOT_SUPPORTED);
		} else if (nodeURI.equals(WPSConstants.NS_WPS)) {
			throw new ExceptionReport("specified namespace is not supported: "
					+ nodeURI, ExceptionReport.INVALID_PARAMETER_VALUE);
		}

	}
}