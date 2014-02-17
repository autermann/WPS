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
package org.n52.wps.io.datahandler.generator;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.HttpHeaders;

public class GeoServerUploader {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeoServerUploader.class);
    public static final AuthScope ANY_AUTH_SCOPE
            = new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM, AuthScope.ANY_SCHEME);
    private final UsernamePasswordCredentials creds;
    private final String baseUrl;
    private final AuthScope authScope;

	public GeoServerUploader(String username, String password, String host,
			String port) {
		this(username, password, host, Integer.valueOf(port));
	}

    public GeoServerUploader(String username, String password, String host, int port) {
        this(username, password,
             String.format("http://%s:%d/geoserver", host, port),
             new AuthScope(host, port));
    }

    public GeoServerUploader(String username, String password, String url) {
        this(username, password, url, ANY_AUTH_SCOPE);
    }

    private GeoServerUploader(String username, String password, String url, AuthScope authScope) {
        this.baseUrl = url;
        this.authScope = authScope;
        this.creds = new UsernamePasswordCredentials(username, password);
    }

	public String uploadGeotiff(File file, String storeName)
			throws HttpException, IOException {
		String target = baseUrl + "/rest/workspaces/N52/coveragestores/" + storeName
                        + "/external.geotiff?configure=first&coverageName=" + storeName;
		String request;
		if (file.getAbsolutePath().startsWith("/")) { // tried with
														// request.replaceAll("//","/");
														// but didn't seem to
														// work...
			request = "file:" + file.getAbsolutePath();
		} else {
			request = "file:/" + file.getAbsolutePath();
		}
		return sendRasterRequest(target, request, "PUT");
	}

	public String uploadShp(File file, String storeName) throws HttpException,
			IOException {
		String target = baseUrl + "/rest/workspaces/N52/datastores/" + storeName + "/file.shp";
		InputStream request = new BufferedInputStream(new FileInputStream(file));
		return sendShpRequest(target, request, "PUT");

	}

	public String createWorkspace() throws HttpException, IOException {
		String target = baseUrl + "/rest/workspaces";
		String request = "<workspace><name>N52</name></workspace>";
		return sendRasterRequest(target, request, "POST");
	}

	private String sendRasterRequest(String target, String request, String method)
			throws HttpException, IOException {
		EntityEnclosingMethod requestMethod = null;
		if (method.equalsIgnoreCase("POST")) {
			requestMethod = new PostMethod(target);
            requestMethod.setRequestEntity(new StringRequestEntity(request, "application/xml", null));
			requestMethod.setRequestHeader(HttpHeaders.CONTENT_TYPE, "application/xml");
		}
		if (method.equalsIgnoreCase("PUT")) {
			requestMethod = new PutMethod(target);
            requestMethod.setRequestEntity(new StringRequestEntity(request, "text/plain", null));
			requestMethod.setRequestHeader(HttpHeaders.CONTENT_TYPE, "text/plain");

		}
        return execute(requestMethod);
	}

	private String sendShpRequest(String target, InputStream request, String method)
			throws HttpException, IOException {

		EntityEnclosingMethod requestMethod = null;
		if (method.equalsIgnoreCase("POST")) {
			requestMethod = new PostMethod(target);
            requestMethod.setRequestEntity(new InputStreamRequestEntity(request, "text/xml"));
			requestMethod.setRequestHeader(HttpHeaders.CONTENT_TYPE, "text/xml");
		}
		if (method.equalsIgnoreCase("PUT")) {
			requestMethod = new PutMethod(target);
            requestMethod.setRequestEntity(new InputStreamRequestEntity(request, "application/zip"));
			requestMethod.setRequestHeader(HttpHeaders.CONTENT_TYPE, "application/zip");
		}
		return execute(requestMethod);
	}

    private String execute(EntityEnclosingMethod requestMethod)
            throws IOException {
        HttpClient client = new HttpClient();
        client.getState().setCredentials(authScope, creds);

        int statusCode = client.executeMethod(requestMethod);

        if (!((statusCode == HttpStatus.SC_OK) || (statusCode == HttpStatus.SC_CREATED))) {
            LOGGER.error("Method failed: {}", requestMethod.getStatusLine());
        }

        // Read the response body.
        byte[] responseBody = requestMethod.getResponseBody();
        return new String(responseBody);
    }
}
