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
package org.n52.wps.webapp.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.n52.wps.webapp.api.AlgorithmEntry;
import org.n52.wps.webapp.api.ConfigurationModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.google.common.collect.ImmutableMap;

/**
 * An implementation for the {@link ConfigurationDAO} interface. This
 * implementation uses JDBC through Spring's
 * {@code NamedParameterJdbcTemplate}.
 */
@Repository("configurationDAO")
public class JdbcConfigurationDAO implements ConfigurationDAO {

    private static final Logger LOG = LoggerFactory
            .getLogger(JdbcConfigurationDAO.class);

    private static final String STATUS = "status";
    private static final String MODULE_CLASS_NAME = "module_class_name";
    private static final String CONFIGURATION_VALUE = "configuration_value";
    private static final String ENTRY_KEY = "entry_key";
    private static final String CONFIGURATION_MODULE = "configuration_module";
    private static final String ACTIVE = "active";
    private static final String ALGORITHM_NAME = "algorithm_name";

    private static final String INSERT_MODULE
            = "INSERT INTO configurationmodule (module_class_name, status) " +
              "VALUES(:module_class_name, :status)";
    private static final String UPDATE_MODULE
            = "UPDATE configurationmodule " +
              "SET status = :status " +
              "WHERE module_class_name = :module_class_name";
    private static final String GET_MODULE
            = "SELECT status FROM configurationmodule " +
              "WHERE module_class_name = :module_class_name";
    private static final String GET_CONFIGURATION_ENTRY_VALUE
            = "SELECT configuration_value FROM configurationentry " +
              "WHERE entry_key = :entry_key " +
              "AND configuration_module = :configuration_module";
    private static final String INSERT_CONFIGURATION_ENTRY_VALUE
            = "INSERT INTO configurationentry (entry_key, configuration_module, configuration_value) " +
              "VALUES(:entry_key, :configuration_module, :configuration_value)";
    private static final String UPDATE_CONFIGURATION_ENTRY_VALUE
            = "UPDATE configurationentry " +
              "SET configuration_value = :configuration_value " +
              "WHERE entry_key = :entry_key " +
              "AND configuration_module = :configuration_module";
    private static final String GET_ALOGIRTHM_ENTRY
            = "SELECT * FROM algorithmentry " +
              "WHERE algorithm_name = :algorithm_name " +
              "AND configuration_module = :configuration_module";
    private static final String INSERT_ALGORITHM_ENTRY
            = "INSERT INTO algorithmentry (algorithm_name, configuration_module, active)" +
              "VALUES(:algorithm_name, :configuration_module, :active)";
    private static final String UPDATE_ALGORITHM_ENTRY
            = "UPDATE algorithmentry " +
              "SET active = :active " +
              "WHERE algorithm_name = :algorithm_name " +
              "AND configuration_module = :configuration_module";

    @Autowired
    private NamedParameterJdbcTemplate template;

    @Override
    public void insertConfigurationModule(ConfigurationModule module) {
        LOG.debug("Inserting configuration module '{}' into the database.",
                  module.getClass().getName());
        template.update(INSERT_MODULE, ImmutableMap.of(
                MODULE_CLASS_NAME, module.getClass().getName(),
                STATUS, module.isActive()));
    }

    @Override
    public void updateConfigurationModuleStatus(ConfigurationModule module) {
        LOG.debug("Updating configuration module '{}' in the database.",
                  module.getClass().getName());
        template.update(UPDATE_MODULE, ImmutableMap.of(
                MODULE_CLASS_NAME, module.getClass().getName(),
                STATUS, module.isActive()));
    }

    @Override
    public Boolean getConfigurationModuleStatus(ConfigurationModule module) {
        LOG.debug("Getting configuration module '{}' status from the database.",
                  module.getClass().getName());
        List<Boolean> status = template.query(
                GET_MODULE,
                ImmutableMap.of(MODULE_CLASS_NAME,
                                module.getClass().getName()),
                ConfigurationModuleStatusRowMapper.INSTANCE);

        if (status.isEmpty()) {
            return null;
        } else if (status.size() == 1) {
            return status.get(0);
        } else {
            return null;
        }
    }
    

    @Override
    public Object getConfigurationEntryValue(String moduleClassName,
                                             String entryKey) {
        LOG.debug("Getting configuration entry '{}' in configuration module '{}' from the database.",
                  entryKey, moduleClassName);
        List<Object> values = template.query(
                GET_CONFIGURATION_ENTRY_VALUE,
                ImmutableMap.of(ENTRY_KEY, entryKey,
                                CONFIGURATION_MODULE, moduleClassName), 
                ConfigurationEntryRowMapper.INSTANCE);

        if (values.isEmpty()) {
            return null;
        } else if (values.size() == 1) {
            return values.get(0);
        } else {
            return null;
        }
    }
    

    @Override
    public void insertConfigurationEntryValue(String moduleClassName,
                                              String entryKey, Object value) {
        LOG.debug("Inserting value '{}' for configuration entry '{}' in configuration module '{}' into the database.",
                       value, entryKey, moduleClassName);
        template.update(INSERT_CONFIGURATION_ENTRY_VALUE, ImmutableMap.of(
                ENTRY_KEY, entryKey,
                CONFIGURATION_MODULE, moduleClassName,
                CONFIGURATION_VALUE, value));
    }

    @Override
    public void updateConfigurationEntryValue(String moduleClassName,
                                              String entryKey, Object value) {
        LOG.debug("Updating configuration entry '{}' in configuration module '{}' to the value of '{}' in the database.",
                  entryKey, moduleClassName, value);
        template.update(UPDATE_CONFIGURATION_ENTRY_VALUE, ImmutableMap.of(
                ENTRY_KEY, entryKey,
                CONFIGURATION_MODULE, moduleClassName,
                CONFIGURATION_VALUE, value));
    }

    @Override
    public AlgorithmEntry getAlgorithmEntry(String moduleClassName,
                                            String algorithm) {
        LOG.debug("Getting algorithm entry '{}' in configuration module '{}' from the database.",
                  algorithm, moduleClassName);

        List<AlgorithmEntry> entries = template.query(
                GET_ALOGIRTHM_ENTRY,
                ImmutableMap.of(ALGORITHM_NAME, algorithm,
                                CONFIGURATION_MODULE, moduleClassName), 
                AlgorithmEntryRowMapper.INSTANCE);

        if (entries.isEmpty()) {
            return null;
        } else if (entries.size() == 1) {
            return entries.get(0);
        } else {
            return null;
        }
    }

    @Override
    public void insertAlgorithmEntry(String moduleClassName, String algorithm,
                                     boolean active) {
        LOG.debug("Inserting algorithm entry '{}' in configuration module '{}' with the status of '{}' into the database.",
                  algorithm, moduleClassName, active);

        template.update(INSERT_ALGORITHM_ENTRY, ImmutableMap.of(
                ALGORITHM_NAME, algorithm,
                CONFIGURATION_MODULE, moduleClassName,
                ACTIVE, active
        ));
    }

    @Override
    public void updateAlgorithmEntry(String moduleClassName, String algorithm,
                                     boolean active) {
        LOG.debug("Updating algorithm entry '{}' in configuration module '{}' to the status of '{}' in the database.",
                  algorithm, moduleClassName, active);
        
        template.update(UPDATE_ALGORITHM_ENTRY, ImmutableMap.of(
                ALGORITHM_NAME, algorithm,
                CONFIGURATION_MODULE, moduleClassName,
                ACTIVE, active));
    }

    private static class AlgorithmEntryRowMapper
            implements RowMapper<AlgorithmEntry> {
        public static final RowMapper<AlgorithmEntry> INSTANCE
                = new AlgorithmEntryRowMapper();

        @Override
        public AlgorithmEntry mapRow(ResultSet rs, int rowNo)
                throws SQLException {
            return new AlgorithmEntry(rs.getString(ALGORITHM_NAME),
                                      rs.getBoolean(ACTIVE));
        }
    }

    private static class ConfigurationEntryRowMapper
            implements RowMapper<Object> {
        public static final RowMapper<Object> INSTANCE
                = new ConfigurationEntryRowMapper();

        @Override
        public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
            return rs.getObject(CONFIGURATION_VALUE);
        }
    }

    private static class ConfigurationModuleStatusRowMapper
            implements RowMapper<Boolean> {
        public static final RowMapper<Boolean> INSTANCE
                = new ConfigurationModuleStatusRowMapper();

        @Override
        public Boolean mapRow(ResultSet rs, int rowNum) throws SQLException {
            return rs.getBoolean(STATUS);
        }
    }
}
