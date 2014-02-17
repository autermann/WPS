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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.n52.wps.server.database.configuration.DatabaseConfiguration;
import org.n52.wps.server.database.configuration.FlatFileDatabaseConfiguration;

public class DatabaseFactory {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(DatabaseFactory.class);
    private static final DatabaseFactory INSTANCE = new DatabaseFactory();
    private IDatabase delegate;
    private DatabaseConfiguration configuration;

    private IDatabase getDelegate() {
        checkState(this.delegate != null, "Not initialized");
        return this.delegate;
    }

    @Override
    protected void finalize()
            throws Throwable {
        try {
            if (this.delegate != null) {
                this.delegate.shutdown();
                this.delegate = null;
            }
        } finally {
            super.finalize();
        }
    }

    private IDatabase createDelegate(DatabaseConfiguration configuration)
            throws DatabaseInitializationException {

        if (configuration != null && configuration.getDatabaseClassName() != null) {
            try {
                IDatabase database = (IDatabase) Class.forName(configuration.getDatabaseClassName()).newInstance();
                database.init(configuration);
                return database;
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                LOGGER.error("Database class could not be instantiated.", e);
            }
        }
        LOGGER.info("Database class name was not found in properties. FlatFileDatabase will be used.");
        IDatabase database = new FlatFileDatabase();
        database.init(new FlatFileDatabaseConfiguration());
        return database;
    }

    public void init(DatabaseConfiguration configuration)
            throws DatabaseInitializationException {
        checkNotNull(configuration);
        synchronized (this) {
            if (this.configuration == null || !this.configuration
                    .equals(configuration)) {
                this.configuration = configuration;
                if (this.delegate != null) {
                    this.delegate.shutdown();
                }
                this.delegate = createDelegate(configuration);
            }
        }
    }

    public static IDatabase getDatabase() {
        return getInstance().getDelegate();
    }

    public static DatabaseFactory getInstance() {
        return INSTANCE;
    }
}
