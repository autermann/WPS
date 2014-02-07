/**
 * ﻿Copyright (C) 2007 - 2014 52°North Initiative for Geospatial Open Source
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
 *       • Apache License, version 2.0
 *       • Apache Software License, version 1.0
 *       • GNU Lesser General Public License, version 3
 *       • Mozilla Public License, versions 1.0, 1.1 and 2.0
 *       • Common Development and Distribution License (CDDL), version 1.0
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.n52.wps.commons.MIMEUtil;
import org.n52.wps.commons.XMLUtil;
import org.n52.wps.server.database.DatabaseFactory;
import org.n52.wps.server.database.IDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.net.HttpHeaders;

public class RetrieveResultServlet extends HttpServlet {

    private final static Logger LOGGER = LoggerFactory.getLogger(RetrieveResultServlet.class);
    private static final long serialVersionUID = -268198171054599696L;
    // This is required for URL generation for response documents.
    public final static String SERVLET_PATH = "RetrieveResultServlet";
    // in future parameterize
    private final boolean indentXML = false;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// id of result to retrieve.
		String id = request.getParameter(WPSConstants.PARAMETER_ID);
		// optional alternate name for filename (rename the file when retrieving if requested)
		String alternateFilename = request.getParameter(WPSConstants.PARAMETER_FILENAME);
        // return result as attachment (instructs browser to offer user "Save" dialog)
        String attachment = request.getParameter(WPSConstants.PARAMETER_ATTACHMENT);

        if (StringUtils.isEmpty(id)) {
            errorResponse("id parameter missing", response);
        } else {

            IDatabase db = DatabaseFactory.getDatabase();
            String mimeType = db.getMimeTypeForStoreResponse(id);
            long contentLength = db.getContentLengthForStoreResponse(id);

            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                inputStream = db.lookupResponse(id);
                if (inputStream == null) {
                    errorResponse("id " + id + " is unknown to server", response);
                } else if (mimeType == null) {
                    errorResponse("Unable to determine mime-type for id " + id, response);
                } else {
                    String suffix = MIMEUtil.getSuffixFromMIMEType(mimeType).toLowerCase();
                    boolean isXML = "xml".equals(suffix);

                    // if attachment parameter unset, default to false for mime-type of 'xml' and true for everything else.
					boolean useAttachment = (StringUtils.isEmpty(attachment) && !isXML) || Boolean.parseBoolean(attachment);
					if (useAttachment) {
                        StringBuilder sb = new StringBuilder();
                        if (StringUtils.isEmpty(alternateFilename)) {
                            sb.append(id);
                        } else {
                            sb.append(alternateFilename);
                        }
						String attachmentName = sb.append('.').append(suffix).toString();
						response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachmentName + "\"");
					}

                    response.setContentType(mimeType);
                    try {
                        outputStream = response.getOutputStream();
                    } catch (IOException e) {
                        throw new IOException("Error obtaining output stream for response", e);
                    }
                    if (isXML) {
                        // need these to work around aggressive IE 8 caching.
                        response.addHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store");
                        response.addHeader(HttpHeaders.PRAGMA, "no-cache");
                        response.addHeader(HttpHeaders.EXPIRES, "-1");

                        // NOTE:  We don't set "Content-Length" header, xml may be modified
                        copyResponseAsXML(inputStream, outputStream, useAttachment || indentXML, id);
                    } else {
                        if (contentLength > -1) {
                            // Can't use response.setContentLength(...) as it accepts an int (max of 2^31 - 1) ?!
                            // response.setContentLength(contentLength);
                            response.setHeader(HttpHeaders.CONTENT_LENGTH, Long.toString(contentLength));
                        } else {
                            LOGGER.warn("Content-Length unknown for response to id {}", id);
                        }
                        copyResponseStream(inputStream, outputStream, id, contentLength);
                    }
                }
            } catch (Exception e) {
                logException(e);
            } finally {
                IOUtils.closeQuietly(inputStream);
                IOUtils.closeQuietly(outputStream);
            }
        }
    }

    protected void errorResponse(String error, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        PrintWriter writer = response.getWriter();
        writer.write("<html><title>Error</title><body>");
        writer.write(error);
        writer.write("</body></html>");
        writer.flush();
        LOGGER.warn("Error processing response: " + error);
    }

    protected void copyResponseStream(InputStream inputStream,
                                      OutputStream outputStream,
                                      String id,
                                      long contentLength) throws IOException {
        long contentWritten = 0;
        try {
            byte[] buffer = new byte[8192];
            int bufferRead;
            while ((bufferRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bufferRead);
                contentWritten += bufferRead;
            }
        } catch (IOException e) {
            if (contentLength > -1) {
                throw new IOException(String.format("Error writing response to output stream for id %s, %d of %d bytes written", id, contentWritten, contentLength), e);
            } else {
                throw new IOException(String.format("Error writing response to output stream for id %s, %d bytes written", id, contentWritten), e);
            }
        }
        LOGGER.info("{} bytes written in response to id {}", contentWritten, id);
    }

    protected void copyResponseAsXML(InputStream inputStream,
                                     OutputStream outputStream,
                                     boolean indent, String id)
            throws IOException {
        try {
            XMLUtil.copyXML(inputStream, outputStream, indent);
        } catch (IOException e) {
            throw new IOException("Error writing XML response for id " + id, e);
        }
    }

    private void logException(Exception exception) {
        StringBuilder errorBuilder = new StringBuilder(exception.getMessage());
        Throwable cause = Throwables.getRootCause(exception);
        if (cause != exception) {
            errorBuilder.append(", exception message: ").append(cause.getMessage());
        }
        LOGGER.error(errorBuilder.toString());
    }

}
