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


package org.n52.wps.server.request;

import java.util.List;

import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.WPSConstants;
import org.n52.wps.server.response.CapabilitiesResponse;
import org.n52.wps.server.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.collect.Lists;

/**
 * Handles a CapabilitesRequest
 */
public class CapabilitiesRequest extends Request {
    private static final Logger LOGGER = LoggerFactory.getLogger(CapabilitiesRequest.class);
    private static final Object REQUEST_DOC = "document";

    /**
     * Creates a CapabilitesRequest based on a Map (HTTP_GET)
     * 
     * @param ciMap
     *        The client input
     * @throws ExceptionReport
     */
        public CapabilitiesRequest(CaseInsensitiveMap ciMap) throws ExceptionReport {
            super(ciMap);
            //Fix for https://bugzilla.52north.org/show_bug.cgi?id=907
            String providedAcceptVersionsString = Request.getMapValue(WPSConstants.PARAMETER_ACCEPT_VERSIONS, ciMap, false);
            
            if (providedAcceptVersionsString != null) {

                String[] providedAcceptVersions = providedAcceptVersionsString.split(",");
                
                if (providedAcceptVersions != null) {
                    map.put(WPSConstants.PARAMETER_VERSION, providedAcceptVersions);
                }
            }
        }

        public CapabilitiesRequest(Document doc) throws ExceptionReport {
            super(doc);
            this.map = new CaseInsensitiveMap();

            Node fc = this.doc.getFirstChild();
            String name = fc.getNodeName();
            this.map.put(REQUEST_DOC, name);
            
            Node serviceItem = fc.getAttributes().getNamedItem(WPSConstants.AN_SERVICE);
            if (serviceItem != null) {
                String service = serviceItem.getNodeValue();
                String[] serviceArray = {service};
                
                this.map.put(WPSConstants.PARAMETER_SERVICE, serviceArray);
            }
            
            NodeList nList = doc.getFirstChild().getChildNodes();
            List<String> versionList = Lists.newLinkedList();
            
            for (int i = 0; i < nList.getLength(); i++) {
                Node n = nList.item(i);
                if (n.getLocalName() != null) {
                    if (n.getLocalName().equalsIgnoreCase(WPSConstants.EN_ACCEPT_VERSIONS)) {
                        
                        NodeList nList2 = n.getChildNodes();

                        for (int j = 0; j < nList2.getLength(); j++) {
                            Node n2 = nList2.item(j);

                            if (n2.getLocalName() != null
                            && n2.getLocalName().equalsIgnoreCase(WPSConstants.AN_VERSION)) {
                                versionList.add(n2.getTextContent());
                            }
                        }
                        break;
                    }
                }
            }

            if ( !versionList.isEmpty()) {
                this.map.put(WPSConstants.PARAMETER_VERSION, versionList.toArray(new String[versionList.size()]));
            }

        }

    /**
     * Validates the client input
     * 
     * @throws ExceptionReport
     * @return True if the input is valid, False otherwise
     */
    @Override
    public boolean validate() throws ExceptionReport {
        String serviceType = getMapValue(WPSConstants.PARAMETER_SERVICE, true);
        if ( !serviceType.equalsIgnoreCase(WPSConstants.WPS_SERVICE_TYPE)) {
            throw new ExceptionReport("Parameter <service> is not correct, expected: WPS , got: " + serviceType,
                    ExceptionReport.INVALID_PARAMETER_VALUE, WPSConstants.PARAMETER_SERVICE);
        }

        String[] versions = getMapArray(WPSConstants.PARAMETER_VERSION, false);
        if ( !requireVersion(WPSConstants.WPS_SERVICE_VERSION, false)) {
            throw new ExceptionReport("Requested versions are not supported, you requested: "
                                      + Request.accumulateString(versions), ExceptionReport.VERSION_NEGOTIATION_FAILED, WPSConstants.PARAMETER_VERSION);
        }

        return true;
    }

    /**
     * Actually serves the Request.
     * 
     * @throws ExceptionReport
     * @return Response The result of the computation
     */
    @Override
    public Response call() throws ExceptionReport {
        validate();
        LOGGER.info("Handled GetCapabilitiesRequest successfully!");
        return new CapabilitiesResponse(this);
    }

    /**
     * Not used in this class. Returns null;
     */
    @Override
    public Object getAttachedResult() {
        return null;
    }
}
