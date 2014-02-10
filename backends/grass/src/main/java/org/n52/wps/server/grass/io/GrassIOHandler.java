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
package org.n52.wps.server.grass.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.n52.wps.commons.Format;
import org.n52.wps.commons.WPSConfig;
import org.n52.wps.io.data.GenericFileData;
import org.n52.wps.io.data.GenericFileDataConstants;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GenericFileDataBinding;
import org.n52.wps.server.WebProcessingService;
import org.n52.wps.server.grass.GrassProcessRepository;
import org.n52.wps.server.grass.util.ConfigFileBuilder;
import org.n52.wps.server.grass.util.FileBuilder;
import org.n52.wps.server.grass.util.JavaProcessStreamReader;
import org.n52.wps.server.grass.util.PathBuilder;


/**
 * @author Benjamin Pross (bpross-52n)
 *
 */
public class GrassIOHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrassIOHandler.class);
    public static final String OS_NAME = System.getProperty("os.name");
    private static final String TEXT_PLAIN = "text/plain";
    private static final String APPLICATION_SHP = "application/shp";
    private static final String UTF8 = "UTF-8";
    private static final String TEXT_XML = "TEXT/XML";
    private static final String IMAGE_PNG = "IMAGE/PNG";
    private static final String IMAGE_JPEG = "IMAGE/JPEG";
    private static final String IMAGE_GIF = "IMAGE/GIF";
    private static final String IMAGE_GEOTIFF = "IMAGE/GEOTIFF";
    private static final String APPLICATION_HDF4_IMAGE = "APPLICATION/HDF4Image";
    private static final String APPLICATION_X_ERDAS_HFA = "APPLICATION/X-ERDAS-HFA";
    private static final String APPLICATION_DGN = "APPLICATION/DGN";
    private static final String APPLICATION_NETCDF = "APPLICATION/NETCDF";

    private static final String LOG_FILE_EXTENSION = ".log";
    private static final String STD_ERR_FILE_NAME = "_stderr.log";
    private static final String STD_OUT_FILE_NAME = "_stdout.log ";

    private final String grassHome;
    private String pythonHome;
    private final String pythonPath;
    private final String grassModuleStarterHome;
    private final String tmpDir;
    private String inputTxtFilename;
    private String command;
    private String uuid;
    private final String pythonName;
    private final String addonPath;
    private Environment envp;
    private boolean isAddon;
    private final String lineSeparator = System.getProperty("line.separator");
    private final String appDataDir = System.getenv(Environment.APPDATA);

    public GrassIOHandler() {

        if (!OS_NAME.startsWith("Windows")) {
            pythonName = "python";
        } else {
            pythonName = "python.exe";
        }

        grassHome = GrassProcessRepository.grassHome;
        grassModuleStarterHome = GrassProcessRepository.grassModuleStarterHome;
        tmpDir = GrassProcessRepository.tmpDir;
        pythonHome = GrassProcessRepository.pythonHome;
        pythonPath = GrassProcessRepository.pythonPath;
        addonPath = GrassProcessRepository.addonPath;

        File tmpDirectory = new File(tmpDir);

        if (!tmpDirectory.exists()) {
            tmpDirectory.mkdir();
        }
    }

    /**
     * Method to execute a GRASS GIS process.
     *
     * @param processID        the name of the process
     * @param complexInputData complex input data for the process
     * @param literalInputData literal input data for the process
     * @param outputID         the ID of the output
     * @param outputMimeType   the mime type of the output
     * @param outputSchema     the schema of the output
     * @param isAddon          if the process is a add on
     *
     * @return a GenericFileDataBinding containing the generated output
     */
    public IData executeGrassProcess(String processID,
                                     Map<String, List<IData>> complexInputData,
                                     Map<String, List<IData>> literalInputData,
                                     String outputID, String outputMimeType,
                                     String outputSchema, boolean isAddon) {

        String outputFileName;

        this.isAddon = isAddon;
        String extension = GenericFileDataConstants.mimeTypeFileTypeLUT().get(outputMimeType);
        String name = "out" + UUID.randomUUID().toString().substring(0, 5);
        outputFileName = FileBuilder.create(tmpDir).file(name, extension).build();

        boolean success = createInputTxt(processID, complexInputData, literalInputData, outputID, outputFileName, outputMimeType, outputSchema);

        if (!success) {
            inputTxtFilename = null;
            return null;
        }

        //start grassmodulestarter.py
        executeGrassModuleStarter();

        File outputFile = new File(outputFileName);

        if (!outputFile.exists()) {
            inputTxtFilename = null;
            return null;
        }

        //give back genericfiledatabinding with the outputfile created by grass
        try {
            GenericFileData outputFileData = new GenericFileData(outputFile, outputMimeType);
            GenericFileDataBinding outputData = new GenericFileDataBinding(outputFileData);
            return outputData;
        } catch (IOException e) {
            LOGGER.error("Error creating GenericFileData", e);
            return null;
        } finally {
            inputTxtFilename = null;
        }
    }

    private String getCommand() {

        if (command == null) {
            StringBuilder builder = new StringBuilder();
            uuid = UUID.randomUUID().toString().substring(0, 7);
            builder.append(getPythonHome()).append(File.separator).append(pythonName);
            builder.append(" ");
            builder.append(grassModuleStarterHome).append(File.separator).append("GrassModuleStarter.py");
            builder.append(" -f ").append(getInputTxtFilename());
            builder.append(" -l ").append(tmpDir).append(File.separator).append(uuid).append(LOG_FILE_EXTENSION);
            builder.append(" -o ").append(tmpDir).append(File.separator).append(STD_OUT_FILE_NAME);
            builder.append(" -e ").append(tmpDir).append(File.separator).append(STD_ERR_FILE_NAME);
            command = builder.toString();
        }

        return command;
    }

    private Environment getEnvp() {
        if (envp == null) {
            if (OS_NAME.startsWith("Windows")) {
                envp = createWindowsEnvp();
            } else {
                envp = createEnvp();
            }
        }
        return envp;
    }

    private Environment createEnvp() {
        return new Environment()
                .add(Environment.GDAL_DATA, FileBuilder.create(grassHome).dir("etc").dir("ogr_csv"))
                .add(Environment.PATH, PathBuilder.create()
                        .add(FileBuilder.create(grassHome).dir("lib"))
                        .add(FileBuilder.create(grassHome).dir("bin"))
                        .add(FileBuilder.create(grassHome).dir("scripts"))
                        .add(pythonHome)
                        .add(FileBuilder.create(grassHome).dir("extralib"))
                        .add(FileBuilder.create(grassHome).dir("extrabin")))
                .add(Environment.LD_LIBRARY_PATH, FileBuilder.create(grassHome).dir("lib"))
                .add(Environment.PWD, grassHome)
                .add(Environment.PYTHONHOME, pythonHome)
                .add(Environment.PYTHONPATH, PathBuilder.create()
                        .add(FileBuilder.create(grassHome).dir("etc").dir("python"))
                        .add(pythonPath))
                .add(Environment.GRASS_CONFIG_DIR, ".grass7")
                .add(Environment.GRASS_GNUPLOT, "gnuplot -persist")
                .add(Environment.GRASS_PAGER,"less")
                .add(Environment.GRASS_PYTHON, "python")
                .add(Environment.GRASS_SH, "/bin/sh")
                .add(Environment.GRASS_VERSION, "7.0.svn");
    }

    private Environment createWindowsEnvp() {
        return new Environment()
                .add(Environment.APPDATA, appDataDir)
                .add(Environment.GDAL_DATA, FileBuilder.create(grassHome).dir("etc").dir("ogr_csv"))
                .add(Environment.PWD, grassHome)
                .add(Environment.PYTHONHOME, pythonHome)
                .add(Environment.GRASS_CONFIG_DIR, "grass7")
                .add(Environment.GRASS_GNUPLOT, "gnuplot -persist")
                .add(Environment.GRASS_PAGER, "less")
                .add(Environment.GRASS_PYTHON, "python")
                .add(Environment.GRASS_SH, "/bin/sh")
                .add(Environment.GRASS_VERSION, "7.0.svn")
                .add(Environment.SYSTEM_ROOT, System.getenv("SystemRoot"))
                .add(Environment.WINGISBASE, grassHome);

    }

    private String getInputTxtFilename() {
        if (inputTxtFilename == null) {
            String txtID = UUID.randomUUID().toString();
            txtID = txtID.substring(0, 5);
            inputTxtFilename = tmpDir + File.separator + txtID + ".txt";
        }
        return inputTxtFilename;
    }

    private String getPythonHome() {
        if (pythonHome == null) {
            pythonHome = FileBuilder.create(grassHome).dir("extrabin").build();
        }
        return pythonHome;
    }

    /**
     * Creates the txt-file required by the GrassModuleStarter.py
     *
     * @param processID
     *                         ID of the process
     * @param complexInputData
     *                         complexinputdata for the process
     * @param literalInputData
     *                         literalinputdata for the process
     * @param outputID
     *                         ID of the outputdata
     * @param outputFileName
     *                         name and path to the result of the GRASS-process
     * @param outputMimeType
     *                         suggested mimetype of the result of the GRASS-process
     *
     * @return true, if everything worked, otherwise false
     */
    private boolean createInputTxt(String processID,
                                   Map<String, List<IData>> complexInputData,
                                   Map<String, List<IData>> literalInputData,
                                   String outputID, String outputFileName,
                                   String outputMimeType, String outputSchema) {

            LOGGER.info("Creating input.txt.");

            ConfigFileBuilder b = new ConfigFileBuilder();;
            b.addBlock("System")
                    .add("WorkDir", tmpDir)
                    .add("OutputDir", tmpDir);
            
            b.addBlock("GRASS")
                    .add("GISBASE", grassHome)
                    .add("GRASS_ADDON_PATH", isAddon ? addonPath : "")
                    .add("GRASS_VERSION", "7.0.svn")
                    .add("Module", processID)
                    .add("LOCATION")
                    .add("LinkInput","FALSE")
                    .add("IgnoreProjection", "FALSE")
                    .add("UseXYLocation","FALSE");

            for (String key : complexInputData.keySet()) {
                for (IData data : complexInputData.get(key)) {
                    createComplexInputDataBlock(b, key, data);
                }
            }

            for (String key : literalInputData.keySet()) {
                for (IData data :  literalInputData.get(key)) {
                    createLiteralInputDataBlock(b, key, data);
                }
            }

            createComplexOutputBlock(b, outputID, outputMimeType, outputFileName);

        File file = new File(getInputTxtFilename());
        try (FileWriter fout = new FileWriter(file);
             BufferedWriter bout = new BufferedWriter(fout)) {
            b.appendTo(bout);
            bout.flush();
            return true;
        } catch (IOException e) {
            LOGGER.error("Error writing input block", e);
            return false;
        }
    }

    private void createComplexOutputBlock(ConfigFileBuilder b, String id,
                                          String mimeType,
                                          String fileName) {
        final Format outputFormat;
        switch (mimeType) {
            case GenericFileDataConstants.MIME_TYPE_PLAIN_TEXT:
                outputFormat = new Format(TEXT_PLAIN);
                break;
            case GenericFileDataConstants.MIME_TYPE_ZIPPED_SHP:
                outputFormat = new Format(APPLICATION_SHP);
                break;
            case GenericFileDataConstants.MIME_TYPE_TIFF:
            case GenericFileDataConstants.MIME_TYPE_GEOTIFF:
                outputFormat = new Format(IMAGE_GEOTIFF);
                break;
            case GenericFileDataConstants.MIME_TYPE_TEXT_XML:
                //TODO change to gml2.1.2?!
                outputFormat = new Format(TEXT_XML, UTF8,
                        "http://schemas.opengis.net/gml/3.1.0/polygon.xsd");
                break;
            case GenericFileDataConstants.MIME_TYPE_KML:
                outputFormat = new Format(TEXT_XML, UTF8, "KML");
                break;
            case GenericFileDataConstants.MIME_TYPE_IMAGE_PNG:
                outputFormat = new Format(IMAGE_PNG);
                break;
            case GenericFileDataConstants.MIME_TYPE_IMAGE_JPEG:
                outputFormat = new Format(IMAGE_JPEG);
                break;
            case GenericFileDataConstants.MIME_TYPE_IMAGE_GIF:
                outputFormat = new Format(IMAGE_GIF);
                break;
            case GenericFileDataConstants.MIME_TYPE_IMAGE_GEOTIFF:
                outputFormat = new Format(IMAGE_GEOTIFF);
                break;
            case GenericFileDataConstants.MIME_TYPE_HDF:
                outputFormat = new Format(APPLICATION_HDF4_IMAGE);
                break;
            case GenericFileDataConstants.MIME_TYPE_X_ERDAS_HFA:
                outputFormat = new Format(APPLICATION_X_ERDAS_HFA);
                break;
            case GenericFileDataConstants.MIME_TYPE_X_NETCDF:
            case GenericFileDataConstants.MIME_TYPE_NETCDF:
                outputFormat = new Format(APPLICATION_NETCDF);
                break;
            case GenericFileDataConstants.MIME_TYPE_DGN:
                outputFormat = new Format(APPLICATION_DGN);
                break;
            default:
                outputFormat = new Format(null, null, null);
        }

        b.addBlock("ComplexOutput")
                .add("Identifier", id)
                .add("PathToFile", fileName)
                .add("MimeType", outputFormat.getMimeType().or(""))
                .add("Encoding", outputFormat.getEncoding().or(""))
                .add("Schema", outputFormat.getSchema().or(""));
    }

    private void createComplexInputDataBlock(ConfigFileBuilder b, String key, IData data) {
        if (!(data instanceof GenericFileDataBinding)) {
            return;
        }
        Format inputFormat;
        GenericFileDataBinding fileData = (GenericFileDataBinding) data;

        String mimetype = fileData.getPayload().getMimeType();
        switch (mimetype) {
            case GenericFileDataConstants.MIME_TYPE_TIFF:
                inputFormat = new Format(mimetype);
                break;
            case GenericFileDataConstants.MIME_TYPE_ZIPPED_SHP:
                inputFormat = new Format(APPLICATION_SHP);
                break;
            case GenericFileDataConstants.MIME_TYPE_PLAIN_TEXT:
                inputFormat = new Format(GenericFileDataConstants.MIME_TYPE_PLAIN_TEXT, UTF8);
                break;
            case GenericFileDataConstants.MIME_TYPE_TEXT_XML:
                inputFormat = new Format(TEXT_XML, UTF8, "GML");
                break;
            case GenericFileDataConstants.MIME_TYPE_KML:
                inputFormat = new Format(TEXT_XML, UTF8, "KML");
                break;
            case GenericFileDataConstants.MIME_TYPE_IMAGE_PNG:
                inputFormat = new Format(IMAGE_PNG);
                break;
            case GenericFileDataConstants.MIME_TYPE_IMAGE_JPEG:
                inputFormat = new Format(IMAGE_JPEG);
                break;
            case GenericFileDataConstants.MIME_TYPE_IMAGE_GIF:
                inputFormat = new Format(IMAGE_GIF);
                break;
            case GenericFileDataConstants.MIME_TYPE_IMAGE_GEOTIFF:
                inputFormat = new Format(IMAGE_GEOTIFF);
                break;
            case GenericFileDataConstants.MIME_TYPE_HDF:
                inputFormat = new Format(APPLICATION_HDF4_IMAGE);
                break;
            case GenericFileDataConstants.MIME_TYPE_X_ERDAS_HFA:
                inputFormat = new Format(APPLICATION_X_ERDAS_HFA);
                break;
            case GenericFileDataConstants.MIME_TYPE_NETCDF:
                inputFormat = new Format(APPLICATION_NETCDF);
                break;
            case GenericFileDataConstants.MIME_TYPE_DGN:
                inputFormat = new Format(APPLICATION_DGN);
                break;
            default:
                inputFormat = new Format(null, null, null);
        }
        String filename = fileData
                .getPayload().getBaseFile(true).getAbsolutePath();

        b.addBlock("ComplexData")
                .add("Identifier", key)
                .add("MaxOccurs", "1")
                .add("PathToFile", filename)
                .add("MimeType", inputFormat.getMimeType().or(""))
                .add("Encoding", inputFormat.getEncoding().or(""))
                .add("Schema", inputFormat.getSchema().or(""));
    }

    private void createLiteralInputDataBlock(ConfigFileBuilder b, String key, IData data) {
        final String dataType;
        Class<?> supportedClass = data.getSupportedClass();
        if (supportedClass.equals(Float.class)) {
            dataType = "float";
        } else if (supportedClass.equals(Double.class)) {
            dataType = "double";
        } else if (supportedClass.equals(Integer.class)) {
            dataType = "integer";
        } else if (supportedClass.equals(Long.class)) {
            dataType = "integer";
        } else if (supportedClass.equals(String.class)) {
            dataType = "string";
        } else if (supportedClass.equals(Boolean.class)) {
            dataType = "boolean";
        } else {
            dataType = "";
        }
        b.addBlock("LiteralData")
                .add("Identifier", key)
                .add("DataType", dataType)
                .add("Value", String.valueOf(data.getPayload()));
    }

    private void executeGrassModuleStarter() {
        try {
            LOGGER.info("Executing GRASS module starter.");
            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec(getCommand(), getEnvp().get());
            PipedOutputStream pipedOut = new PipedOutputStream();
            PipedInputStream pipedIn = new PipedInputStream(pipedOut);

            // attach STDOUT/STDERR stream reader
            JavaProcessStreamReader errorStreamReader
                    = new JavaProcessStreamReader(proc.getErrorStream(), pipedOut);
            JavaProcessStreamReader outputStreamReader
                    = new JavaProcessStreamReader(proc.getInputStream(), "OUTPUT");

            // start them
            errorStreamReader.start();
            outputStreamReader.start();

            //fetch errors if there are any
            BufferedReader errorReader
                    = new BufferedReader(new InputStreamReader(pipedIn));

            String line = errorReader.readLine();

            String errors = "";

            while (line != null) {
                errors = errors.concat(line + lineSeparator);
                line = errorReader.readLine();
            }

            try {
                proc.waitFor();
            } catch (InterruptedException e1) {
                LOGGER.error("Java proces was interrupted.", e1);
            } finally {
                proc.destroy();
            }

            if (!errors.isEmpty()) {
                File baseDir = FileBuilder.create(WebProcessingService.BASE_DIR).dir("GRASS_LOGS").buildFile();
                if (!baseDir.exists()) {
                    baseDir.mkdir();
                }
                String host = WPSConfig.getInstance().getWPSConfig().getServer().getHostname();
                if (host == null) {
                    host = InetAddress.getLocalHost().getCanonicalHostName();
                }
                String hostPort = WPSConfig.getInstance().getWPSConfig()
                        .getServer().getHostport();
                File tmpLog = FileBuilder.create(tmpDir).file(uuid, LOG_FILE_EXTENSION).buildFile();
                File serverLog =  FileBuilder.create(baseDir).file(uuid, LOG_FILE_EXTENSION).buildFile();

                if (tmpLog.exists()) {
                    try (FileInputStream fis = new FileInputStream(tmpLog);
                         FileOutputStream fos = new FileOutputStream(serverLog)) {
                        byte[] buf = new byte[1024];
                        int i;
                        while ((i = fis.read(buf)) != -1) {
                            fos.write(buf, 0, i);
                        }
                    } catch (IOException e) {
                        LOGGER.error("Error copying log file", e);
                    }

                } else {
                    try (FileWriter fwrite = new FileWriter(serverLog);
                         BufferedWriter bufWrite = new BufferedWriter(fwrite)) {
                        bufWrite.write(errors);
                        bufWrite.flush();
                    }
                }
                LOGGER
                        .error("An error occured while executing the GRASS GIS process.");
                throw new RuntimeException("An error occured while executing the GRASS GIS process. See the log under " +
                                           "http://" + host + ":" + hostPort +
                                           "/" +
                                           WebProcessingService.WEBAPP_PATH +
                                           "/GRASS_LOGS/" + uuid + LOG_FILE_EXTENSION +
                                           " for more details.");
            }

        } catch (IOException e) {
            LOGGER.error("An error occured while executing the GRASS GIS process.", e);
            throw new RuntimeException(e);
        }
    }

    private static class Environment {
        public static final String WINGISBASE = "WINGISBASE";
        public static final String GRASS_SH = "GRASS_SH";
        public static final String GRASS_GNUPLOT = "GRASS_GNUPLOT";
        public static final String PATH = "PATH";
        public static final String SYSTEM_ROOT = "SystemRoot";
        public static final String GRASS_VERSION = "GRASS_VERSION";
        public static final String GRASS_PAGER = "GRASS_PAGER";
        public static final String PYTHONHOME = "PYTHONHOME";
        public static final String PWD = "PWD";
        public static final String LD_LIBRARY_PATH = "LD_LIBRARY_PATH";
        public static final String GDAL_DATA = "GDAL_DATA";
        public static final String PYTHONPATH = "PYTHONPATH";
        public static final String GRASS_PYTHON = "GRASS_PYTHON";
        public static final String APPDATA = "APPDATA";
        public static final String GRASS_CONFIG_DIR = "GRASS_CONFIG_DIR";
        private final Map<String, String> variables = new HashMap<>();

        public Environment add(String key, String value) {
            variables.put(key, value);
            return this;
        }

        public Environment add(String key, Object value) {
            return add(key, String.valueOf(value));
        }

        public String[] get() {
            ArrayList<String> a = new ArrayList<>(variables.size());
            for (Entry<String, String> var : variables.entrySet()) {
                a.add(var.getKey() + "=" + var.getValue());
            }
            return a.toArray(new String[variables.size()]);
        }
    }

}
