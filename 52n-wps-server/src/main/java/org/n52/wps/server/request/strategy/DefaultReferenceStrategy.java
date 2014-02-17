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
package org.n52.wps.server.request.strategy;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;

import net.opengis.wps.x100.InputReferenceType;
import net.opengis.wps.x100.InputType;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DecompressingHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;

import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.InvalidParameterValueException;
import org.n52.wps.server.NoApplicableCodeException;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

/**
 * 
 * @author Matthias Mueller
 * 
 * Basic methods to retrieve input data using HTTP/GET, HTTP/POST or HTTP/POST with href'd body
 *
 */
public class DefaultReferenceStrategy implements IReferenceStrategy{
	
	// TODO: follow HTTP redirects with LaxRedirectStrategy
	
	//TODO: get proxy from config
	//static final HttpHost proxy = new HttpHost("127.0.0.1", 8080, "http");
	@Override
	public boolean isApplicable(InputType input) {
		return true;
	}
	
	// TODO: follow references, e..g 
	
	@Override
	public ReferenceInputStream fetchData(InputType input) throws ExceptionReport {
        DefaultHttpClient c = new DefaultHttpClient();
        DecompressingHttpClient client = new DecompressingHttpClient(c);

		String href = input.getReference().getHref();
        String inputID = input.getIdentifier().getStringValue();
		try {
            HttpUriRequest request = createRequest(input, client);
            InputReferenceType.Header[] xbHeaders = input.getReference().getHeaderArray();
            for (InputReferenceType.Header header : xbHeaders) {
                request.addHeader(new BasicHeader(header.getKey(), header.getValue()));
            }
            if (input.getReference().getMimeType() != null && request.getFirstHeader(HttpHeaders.ACCEPT) == null ) {
                request.addHeader(new BasicHeader(HttpHeaders.ACCEPT, input.getReference().getMimeType()));
            }
            return processResponse(client.execute(request));
		} catch(RuntimeException e) {
			throw new NoApplicableCodeException("Error occured while parsing XML").causedBy(e);
		} catch(MalformedURLException e) {
			throw new InvalidParameterValueException("The inputURL of the execute is wrong: inputID: %s | dataURL: %s", inputID, href).causedBy(e);
		} catch(IOException e) {
			 throw new InvalidParameterValueException("Error occured while receiving the complexReferenceURL: inputID: %s | dataURL: %s", inputID, href).causedBy(e);
		}
	}

    private HttpUriRequest createRequest(InputType input, HttpClient client)
            throws IOException {
        String body = getBody(input, client);
        if (body != null) {
            HttpPost post = new HttpPost(input.getReference().getHref());
            post.setEntity(new StringEntity(body));
            return post;
        } else if (input.getReference().isSetMethod() && input.getReference().getMethod().toString().equals("POST")) {
            return new HttpPost(input.getReference().getHref());
        } else {
            return new HttpGet(input.getReference().getHref());
        }
    }

    private String getBody(InputType input, HttpClient client) throws IOException {
        if (input.getReference().isSetBodyReference()) {
            HttpGet get = new HttpGet(input.getReference().getBodyReference().getHref());
            try (InputStream in = client.execute(get).getEntity().getContent();
                  InputStreamReader reader = new InputStreamReader(in, Charsets.UTF_8);) {
                return CharStreams.toString(reader);
            }
        } else if (input.getReference().isSetBody()) {
            return input.getReference().getBody().toString();
        } else {
            return null;
        }
    }
	
    private ReferenceInputStream processResponse(HttpResponse response) throws IOException {
        HttpEntity entity = response.getEntity();
        Header contentType = entity.getContentType();
        String mimeType = contentType == null ? null : contentType.getValue();
        Header contentEncoding = entity.getContentEncoding();
        String encoding = contentEncoding == null ? null : contentEncoding.getValue();
        return new ReferenceInputStream(entity.getContent(), mimeType, encoding);
    }
}
