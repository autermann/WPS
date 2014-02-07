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
package org.n52.wps.io;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.n52.wps.GeneratorDocument.Generator;
import org.n52.wps.PropertyDocument.Property;
import org.n52.wps.commons.Format;
import org.n52.wps.commons.WPSConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeneratorFactory {
	
	public static final String PROPERTY_NAME_REGISTERED_GENERATORS = "registeredGenerators";
	private static GeneratorFactory factory;
	private static final Logger LOGGER = LoggerFactory.getLogger(GeneratorFactory.class);
	
	private List<IGenerator> registeredGenerators;

	/**
	 * This factory provides all available {@link AbstractXMLGenerator} to WPS.
	 * @param generators
	 */
	public static void initialize(Generator[] generators) {
		if (factory == null) {
			factory = new GeneratorFactory(generators);
		}
		else {
			LOGGER.warn("Factory already initialized");
		}
	}
	
	private GeneratorFactory(Generator[] generators) {
		loadAllGenerators(generators);

        // FvK: added Property Change Listener support
        // creates listener and register it to the wpsConfig instance.
        org.n52.wps.commons.WPSConfig.getInstance().addPropertyChangeListener(org.n52.wps.commons.WPSConfig.WPSCONFIG_PROPERTY_EVENT_NAME, new PropertyChangeListener() {
            public void propertyChange(
                    final PropertyChangeEvent propertyChangeEvent) {
                LOGGER.info(this.getClass().getName() + ": Received Property Change Event: " + propertyChangeEvent.getPropertyName());
                loadAllGenerators(org.n52.wps.commons.WPSConfig.getInstance().getActiveRegisteredGenerator());
            }
        });
	}

    private void loadAllGenerators(Generator[] generators){
        registeredGenerators = new ArrayList<>();
		for(Generator currentGenerator : generators) {

			// remove inactive properties
			Property[] activeProperties = {};
			ArrayList<Property> activeProps = new ArrayList<>();
            for (Property propertyArray : currentGenerator.getPropertyArray()) {
                if (propertyArray.getActive()) {
                    activeProps.add(propertyArray);
                }
            }
			currentGenerator.setPropertyArray(activeProps.toArray(activeProperties));
			
			IGenerator generator = null;
			String generatorClass = currentGenerator.getClassName();
			try {
				 generator = (IGenerator) this.getClass().getClassLoader().loadClass(generatorClass).newInstance();
			}
			catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
				LOGGER.error("One of the generators could not be loaded: " + generatorClass, e);
			}
			if(generator != null) {
				LOGGER.info("Generator class registered: " + generatorClass);
				registeredGenerators.add(generator);
			}
		}
    }

	public static GeneratorFactory getInstance() {
		if(factory == null){
			Generator[] generators = WPSConfig.getInstance().getActiveRegisteredGenerator();
			initialize(generators);
		}
		return factory;
	}
	
	public IGenerator getGenerator(Format format, Class<?> outputInternalClass) {
		
		for(IGenerator generator : registeredGenerators) {
            if (generator.isSupportedFormat(format) &&
                generator.isSupportedDataBinding(outputInternalClass)) {
                return generator;
            }
        }
		//TODO: try a chaining approach, by calculation all permutations and look for matches.
		return null;
	}

	public List<IGenerator> getAllGenerators() {
		return registeredGenerators;
	}

	
	
}
