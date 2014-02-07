package org.n52.wps.server;

import javax.xml.namespace.QName;

/**
 *
 * @author Christian Autermann
 */
public interface WPSConstants {
    public static final String NS_XS = "http://www.w3.org/TR/xmlschema-2#";
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
}
