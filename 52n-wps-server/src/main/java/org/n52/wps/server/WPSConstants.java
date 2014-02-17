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
package org.n52.wps.server;

import javax.xml.namespace.QName;

/**
 *
 * @author Christian Autermann
 */
public interface WPSConstants {
    String NS_XS = "http://www.w3.org/TR/xmlschema-2#";
    String NS_WPS = "http://www.opengis.net/wps/1.0.0";
    String NS_XSI = "http://www.w3.org/2001/XMLSchema-instance";
    String NS_XLINK = "http://www.w3.org/1999/xlink";
    String AN_SCHEMA_LOCATION = "schemaLocation";
    String AN_SERVICE = "service";
    String AN_VERSION = "version";
    QName QN_SCHEMA_LOCATION = new QName(NS_XSI, AN_SCHEMA_LOCATION);
    String DEFAULT_LANGUAGE = "en-US";

    String EN_ACCEPT_VERSIONS = "AcceptVersions";
    String EN_DESCRIBE_PROCESS = "DescribeProcess";
    String EN_EXECUTE = "Execute";
    String EN_GET_CAPABILITIES = "GetCapabilities";
    String EN_IDENTIFIER = "identifier";

    String MIME_TYPE_TEXT_PLAIN = "text/plain";
    String MIME_TYPE_TEXT_XML = "text/xml";

    String PARAMETER_ACCEPT_VERSIONS = "AcceptVersions";
    String PARAMETER_IDENTIFIER = "identifier";
    String PARAMETER_SERVICE = "service";
    String PARAMETER_VERSION = "version";
    String PARAMETER_ATTACHMENT = "attachment";
    String PARAMETER_FILENAME = "filename";
    String PARAMETER_ID = "id";
    String PARAMETER_REQUEST = "request";
    String PARAMETER_LANGUAGE = "language";

    String WPS_SERVICE_TYPE = "WPS";
    String WPS_SERVICE_VERSION = "1.0.0";

    String RETRIEVE_RESULT_REQUEST = "RetrieveResult";
    String GET_CAPABILITIES_REQUEST = "GetCapabilities";
    String DESCRIBE_PROCESS_REQUEST = "DescribeProcess";
    String EXECUTE_REQUEST = "Execute";

    String SCHEMA_LOCATION_WPS_EXECUTE_RESPONSE
            = "http://schemas.opengis.net/wps/1.0.0/wpsExecute_response.xsd";
}
