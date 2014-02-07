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
package org.n52.wps.io;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.n52.wps.ParserDocument.Parser;
import org.n52.wps.PropertyDocument.Property;
import org.n52.wps.commons.Format;
import org.n52.wps.commons.WPSConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * XMLParserFactory. Will be initialized within each Framework.
 *
 * @author foerster
 *
 */
public class ParserFactory {

    @Deprecated
    public static String PROPERTY_NAME_REGISTERED_PARSERS = "registeredParsers";
    private static ParserFactory factory;
    private static final Logger LOGGER = LoggerFactory
            .getLogger(ParserFactory.class);

    private List<IParser> registeredParsers;

    /**
     * This factory provides all available {@link IParser} to WPS.
     *
     * @param parsers
     */
    public static void initialize(Parser[] parsers) {
        if (factory == null) {
            factory = new ParserFactory(parsers);
        } else {
            LOGGER.warn("Factory already initialized");
        }
    }

    private ParserFactory(Parser[] parsers) {
        loadAllParsers(parsers);

        // FvK: added Property Change Listener support
        // creates listener and register it to the wpsConfig instance.
        org.n52.wps.commons.WPSConfig.getInstance()
                .addPropertyChangeListener(org.n52.wps.commons.WPSConfig.WPSCONFIG_PROPERTY_EVENT_NAME, new PropertyChangeListener() {
                    public void propertyChange(
                            final PropertyChangeEvent propertyChangeEvent) {
                                LOGGER.info(this.getClass().getName() +
                                            ": Received Property Change Event: " +
                                            propertyChangeEvent
                                        .getPropertyName());
                                loadAllParsers(org.n52.wps.commons.WPSConfig
                                        .getInstance()
                                        .getActiveRegisteredParser());
                            }
                });
    }

    private void loadAllParsers(Parser[] parsers) {
        registeredParsers = new ArrayList<IParser>();
        for (Parser currentParser : parsers) {

            // remove inactive parser
            Property[] activeProperties = {};
            ArrayList<Property> activePars = new ArrayList<>();
            for (Property propertyArray : currentParser.getPropertyArray()) {
                if (propertyArray.getActive()) {
                    activePars.add(propertyArray);
                }
            }
            currentParser.setPropertyArray(activePars.toArray(activeProperties));

            String parserClass = currentParser.getClassName();
            IParser parser = null;
            try {
                parser = (IParser) this.getClass().getClassLoader()
                        .loadClass(parserClass).newInstance();

            } catch (ClassNotFoundException | IllegalAccessException |
                    InstantiationException e) {
                LOGGER.error("One of the parsers could not be loaded: " +
                             parserClass, e);
            }

            if (parser != null) {

                LOGGER.info("Parser class registered: " + parserClass);
                registeredParsers.add(parser);
            }
        }
    }

    public static ParserFactory getInstance() {
        if (factory == null) {
            Parser[] parsers = WPSConfig.getInstance()
                    .getActiveRegisteredParser();
            initialize(parsers);
        }
        return factory;
    }

    public IParser getParser(Format format, Class<?> requiredInputClass) {

        //first, look if we can find a direct way
        for (IParser parser : registeredParsers) {
            if (parser.isSupportedDataBinding(requiredInputClass) &&
                parser.isSupportedFormat(format)) {
                LOGGER.info("Matching parser found: {}", parser);
                return parser;
            }
        }

		//no parser could be found
        //try an indirect way by creating all permutations and look if one matches
        //TODO
        return null;
    }

    public List<IParser> getAllParsers() {
        return registeredParsers;
    }
}
