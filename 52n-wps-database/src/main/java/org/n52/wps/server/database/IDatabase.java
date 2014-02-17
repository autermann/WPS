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
package org.n52.wps.server.database;

import java.io.File;
import java.io.InputStream;

import org.n52.wps.server.database.configuration.DatabaseConfiguration;

/**
 * An interface-layer to the databases.
 *
 * @note All implementing classes have to be singletons!
 * @author Janne Kovanen
 *
 */
public interface IDatabase {

    void init(DatabaseConfiguration configuration)
            throws DatabaseInitializationException;
    
    void shutdown();

    void insertRequest(String id, InputStream request, boolean xml);

    void insertResponse(String id, InputStream response);
    void updateResponse(String id, InputStream response);
    void storeResponse(String id, InputStream response);

    InputStream getRequest(String id);
    InputStream getResponse(String id);
    InputStream getRawData(String id);

    File getRequestAsFile(String id);
    File getResponseAsFile(String id);
    File getRawDataAsFile(String id);

    void insertRawData(String id, InputStream stream, String mimeType);
    void storeRawData(String id, InputStream stream, String mimeType);
    void updateRawData(String id, InputStream stream);

    String getMimeTypeForResponse(String id);
    String getMimeTypeForRequest(String id);
    String getMimeTypeForRawData(String id);

    long getContentLengthForRequest(String id);
    long getContentLengthForResponse(String id);
    long getContentLengthForRawData(String id);



}
