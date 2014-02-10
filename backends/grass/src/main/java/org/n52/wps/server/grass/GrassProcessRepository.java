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
package org.n52.wps.server.grass;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.opengis.wps.x100.ProcessDescriptionType;

import org.apache.xmlbeans.XmlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.n52.wps.PropertyDocument.Property;
import org.n52.wps.commons.WPSConfig;
import org.n52.wps.server.IAlgorithm;
import org.n52.wps.server.IAlgorithmRepository;
import org.n52.wps.server.grass.util.GRASSWPSConfigVariables;

/**
 * @author Benjamin Pross (bpross-52n)
 *
 */
public class GrassProcessRepository implements IAlgorithmRepository {

	private static Logger LOGGER = LoggerFactory.getLogger(GrassProcessRepository.class);
	public static String tmpDir;
	public static String grassHome;
	public static String pythonHome;
	public static String pythonPath;
	public static String grassModuleStarterHome;
	public static String gisrcDir;
	public static String addonPath;
	private Map<String, ProcessDescriptionType> registeredProcesses;
	private Map<String, Boolean> processesAddonFlagMap;

	public GrassProcessRepository() {
		registeredProcesses = new HashMap<>();
		processesAddonFlagMap = new HashMap<>();
        load();

	}

    private void load() {

        if (!WPSConfig.getInstance().isRepositoryActive(getClass().getCanonicalName())) {
            LOGGER.debug("GRASS Algorithm Repository is inactive.");
            return;
        }
        LOGGER.info("Initializing Grass Repository");

        Property[] propertyArray = WPSConfig.getInstance()
                .getPropertiesForRepositoryClass(getClass().getCanonicalName());

        ArrayList<String> processList = new ArrayList<>(propertyArray.length);

        for (Property property : propertyArray) {
            if (property.getName().equalsIgnoreCase(GRASSWPSConfigVariables.TMP_Dir.toString())) {
                tmpDir = property.getStringValue();
            } else if (property.getName().equalsIgnoreCase(GRASSWPSConfigVariables.Grass_Home.toString())) {
                grassHome = property.getStringValue();
            } else if (property.getName().equalsIgnoreCase(GRASSWPSConfigVariables.ModuleStarter_Home.toString())) {
                grassModuleStarterHome = property.getStringValue();
            } else if (property.getName().equalsIgnoreCase(GRASSWPSConfigVariables.Python_Home.toString())) {
                pythonHome = property.getStringValue();
            } else if (property.getName().equalsIgnoreCase(GRASSWPSConfigVariables.GISRC_Dir.toString())) {
                gisrcDir = property.getStringValue();
            } else if (property.getName().equalsIgnoreCase(GRASSWPSConfigVariables.Addon_Dir.toString())) {
                addonPath = property.getStringValue();
            } else if (property.getName().equalsIgnoreCase(GRASSWPSConfigVariables.Python_Path.toString())) {
                pythonPath = property.getStringValue();
            } else if(property.getName().equals("Algorithm")){
                if (property.getActive()) {
                    processList.add(property.getStringValue());
                } else {
                    LOGGER.info("GRASS process {} not active.", property.getStringValue());
                }
            }
        }

        HashMap<String, String> variableMap = new HashMap<>(6);

        variableMap.put(GRASSWPSConfigVariables.TMP_Dir.toString(), tmpDir);
        variableMap.put(GRASSWPSConfigVariables.Grass_Home.toString(), grassHome);
        variableMap.put(GRASSWPSConfigVariables.ModuleStarter_Home.toString(), grassModuleStarterHome);
        variableMap.put(GRASSWPSConfigVariables.Python_Home.toString(), pythonHome);
        variableMap.put(GRASSWPSConfigVariables.GISRC_Dir.toString(), gisrcDir);
        variableMap.put(GRASSWPSConfigVariables.Python_Path.toString(), pythonPath);

        for (String variable : variableMap.keySet()) {
            if (variableMap.get(variable) == null) {
                throw new RuntimeException("Variable " + variable + " not initialized.");
            }
        }

        File tmpDirectory = new File(tmpDir);
        if (tmpDirectory.exists()) {
            deleteContents(tmpDirectory);
        }

        // initialize after properties are fetched
        GrassProcessDescriptionCreator creator = new GrassProcessDescriptionCreator();

        File processDirectory = new File(grassHome + File.separator + "bin");

        if (processDirectory.isDirectory()) {
            String[] processes = processDirectory.list();
            for (String process : processes) {
                if (process.endsWith(".exe")) {
                    process = process.replace(".exe", "");
                }
                if (processList.contains(process)) {
                    ProcessDescriptionType pDescType;
                    try {
                        pDescType = creator.createDescribeProcessType(process, false);
                        if (pDescType != null) {
                            registeredProcesses.put(process, pDescType);
                            processesAddonFlagMap.put(process, false);
                            LOGGER.info("GRASS process {} added.", process);
                        }
                    } catch (IOException | XmlException e) {
                        LOGGER.warn("Could not add Grass process: {}. Errors while creating process description", process);
                        LOGGER.error(e.getMessage(), e);
                    }

                } else {
                    LOGGER.info("Did not add GRASS process: {}. Not in Repository properties or not active.", process);
                }

            }

        }

        if(addonPath != null) {
            File addonDirectory = new File(addonPath);
            if (addonDirectory.isDirectory()) {
                String[] processes = addonDirectory.list();
                for (String process : processes) {
                    if (process.endsWith(".py")) {
                        process = process.replace(".py", "");
                    }
                    if (process.endsWith(".bat")) {
                        process = process.replace(".bat", "");
                    }
                    if (process.endsWith(".exe")) {
                        process = process.replace(".exe", "");
                    }
                    if (processList.contains(process)) {
                        ProcessDescriptionType pDescType;
                        try {
                            if(registeredProcesses.keySet().contains(process)){
                                LOGGER.info("Skipping duplicate process {}", process);
                                continue;
                            }
                            pDescType = creator.createDescribeProcessType(process, true);
                            if (pDescType != null) {
                                registeredProcesses.put(process, pDescType);
                                processesAddonFlagMap.put(process, true);
                                LOGGER.info("GRASS Addon process {} added.", process);
                            }
                        } catch (IOException | XmlException e) {
                            LOGGER.warn("Could not add Grass Addon process: {}. Errors while creating process description", process);
                            LOGGER.error(e.getMessage(), e);
                        }

                    } else {
                        LOGGER.info("Did not add GRASS Addon process: {}. Not in Repository properties or not active.", process);
                    }
                }
            }
        }
    }

    @Override
	public boolean containsAlgorithm(String identifier) {
		if (registeredProcesses.containsKey(identifier)) {
			return true;
		}
		LOGGER.warn("Could not find Grass process {}", identifier);
		return false;
	}

    @Override
	public IAlgorithm getAlgorithm(String identifier) {
		if (!containsAlgorithm(identifier)) {
			throw new RuntimeException("Could not allocate process");
		}
		return new GrassProcessDelegator(identifier,
                                         registeredProcesses.get(identifier),
                                         processesAddonFlagMap.get(identifier));

    }

    @Override
	public Collection<String> getAlgorithmNames() {
		return registeredProcesses.keySet();
	}

    private void deleteFiles(File directory) {
        if (directory.exists()) {
            deleteContents(directory);
            directory.delete();
        }
	}

    private void deleteContents(File directory) {
        if (directory.exists()) {
            File[] filesToDelete = directory.listFiles();
            for (File file : filesToDelete) {
                try {
                    if (file.isDirectory()) {
                        deleteFiles(file);
                    } else {
                        file.delete();
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

	@Override
	public ProcessDescriptionType getProcessDescription(String processID) {
		if(!registeredProcesses.containsKey(processID)){
			registeredProcesses.put(processID, getAlgorithm(processID).getDescription());
		}
		return registeredProcesses.get(processID);
	}

	@Override
	public void shutdown() {
		// do nothing
	}
	
}
