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


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.xpath.XPathAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class IOUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(IOUtils.class);
    public static final String TEMP_DIR = System.getProperty("java.io.tmpdir");
    public static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    private IOUtils() {
    }

	/**
     * Reads the given input stream as a string and decodes that base64 string
     * into a file with the specified extension
     *
     * @param input
     *            the stream with the base64 string
     * @param extension
     *            the extension of the result file (without the '.' at the
     *            beginning)
     * @return the decoded base64 file written to disk
     * @throws IOException if an error occurs while writing the contents to disk
     */
    public static File writeBase64ToFile(InputStream input, String extension) throws IOException {

        File file = File.createTempFile(
                "file" + UUID.randomUUID(),
                "." + extension,
                new File(TEMP_DIR));

        try (OutputStream out = new FileOutputStream(file)) {
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int n = 0;
            while (-1 != (n = input.read(buffer))) {
                out.write(buffer, 0, n);
            }
            out.flush();
        }

        return file;
    }

	public static File writeStreamToFile(InputStream inputStream, String extension)
			throws IOException {
        File file = File.createTempFile("file" + UUID.randomUUID(), "." + extension);
        return writeStreamToFile(inputStream, file);
    }

    @Deprecated
    public static File writeStreamToFile(InputStream inputStream, String extension, File file)
            throws IOException {
        return writeStreamToFile(inputStream, file);
    }

    public static File writeStreamToFile(InputStream inputStream, File file)
            throws IOException {
        try (FileOutputStream out = new FileOutputStream(file);
             InputStream in = inputStream) {
            byte buf[] = new byte[DEFAULT_BUFFER_SIZE];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.flush();
        }
        return file;
    }

	public static File writeBase64XMLToFile(InputStream stream, String extension) throws SAXException, IOException, ParserConfigurationException, DOMException, TransformerException {
        
        // ToDo:  look at StAX to stream XML parsing instead of in memory DOM
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
        String binary = XPathAPI.selectSingleNode(document.getFirstChild(), "text()").getTextContent();

        try (InputStream byteStream = new ByteArrayInputStream(binary.getBytes())){
            return writeBase64ToFile(byteStream, extension);
        }
    }

	/**
     * Zip the files. Returns a zipped file and delete the specified files
     *
     * @param files
     *            files to zipped
     * @return the zipped file
     * @throws IOException
     *             if the zipping process fails.
     */
    public static File zip(File... files)
            throws IOException {
        File zip = File.createTempFile("zip" + UUID.randomUUID(), ".zip");

        try (FileOutputStream fout = new FileOutputStream(zip);
                ZipOutputStream zout = new ZipOutputStream(fout)) {
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            for (File file : files) {
                if (!file.exists()) {
                    LOGGER.debug("Could not zip {}", file.getAbsolutePath());
                    continue;
                }
                
                zout.putNextEntry(new ZipEntry(file.getName()));
                try (FileInputStream in = new FileInputStream(file)) {
                    int len;
                    while ((len = in.read(buffer)) > 0) {
                        zout.write(buffer, 0, len);
                    }
                    zout.closeEntry();
                }
            }
            
            deleteResources(files);
        }

        return zip;
    }

    /**
     * Unzip the file. Returns the unzipped file with the specified extension
     * and deletes the zipped file
     *
     * @param file
     *            the file to unzip
     * @param extension
     *            the extension to search in the content files
     * @return the file with the specified extension
     * @throws IOException
     *             if the unzipping process fails
     */
    public static List<File> unzip(File file, String extension)
            throws IOException {
        return unzip(file, extension, null);
    }

	public static List<File> unzip(File file, String extension, File directory) throws IOException {
        byte buffer[] = new byte[DEFAULT_BUFFER_SIZE];
        List<File> foundFiles = new ArrayList<>();
        try (FileInputStream fin = new FileInputStream(file);
                BufferedInputStream bin = new BufferedInputStream(fin);
                ZipInputStream zipInputStream = new ZipInputStream(bin)) {
            ZipEntry entry;
            File tempDir = directory;
            if (tempDir == null || !directory.isDirectory()) {
                tempDir = File.createTempFile("unzipped" + UUID.randomUUID(), "", new File(TEMP_DIR));
                tempDir.delete();
                tempDir.mkdir();
            }
            while ((entry = zipInputStream.getNextEntry()) != null) {
                int count;
                File entryFile = new File(tempDir, entry.getName());
                entryFile.createNewFile();
                FileOutputStream fos = new FileOutputStream(entryFile);
                try (BufferedOutputStream dest = new BufferedOutputStream(fos, DEFAULT_BUFFER_SIZE)) {
                    while ((count = zipInputStream.read(buffer, 0, DEFAULT_BUFFER_SIZE)) != -1) {
                        dest.write(buffer, 0, count);
                    }
                    dest.flush();
                }

                if (entry.getName().endsWith("." + extension)) {
                    foundFiles.add(entryFile);
                }
            }
        }

        deleteResources(file);

        return foundFiles;
    }

    public static List<File> unzipAll(File file) throws IOException {
        byte buffer[] = new byte[DEFAULT_BUFFER_SIZE];
        List<File> foundFiles = new ArrayList<>();
        try (FileInputStream fin = new FileInputStream(file);
                BufferedInputStream bin = new BufferedInputStream(fin);
                ZipInputStream zin = new ZipInputStream(bin)) {
            ZipEntry entry;
            File tempDir = File.createTempFile("unzipped" + UUID.randomUUID(), "", new File(TEMP_DIR));
            tempDir.delete();
            tempDir.mkdir();
            while ((entry = zin.getNextEntry()) != null) {
                int count;
                File entryFile = new File(tempDir, entry.getName());
                entryFile.createNewFile();
                FileOutputStream fos = new FileOutputStream(entryFile);
                try (BufferedOutputStream dest
                        = new BufferedOutputStream(fos, DEFAULT_BUFFER_SIZE)) {
                    while ((count = zin.read(buffer, 0, DEFAULT_BUFFER_SIZE)) != -1) {
                        dest.write(buffer, 0, count);
                    }
                    dest.flush();
                }
                foundFiles.add(entryFile);
            }
        }
        deleteResources(file);
        return foundFiles;
    }

    /**
     * Delete the given files and all the files with the same name but different
     * extension. If some file is <code>null</code> just doesn't process it and
     * continue to the next element of the array
     *
     * @param files
     *            the files to delete
     */
    public static void deleteResources(File... files) {
        //FIXME this will delete the files if the JVM exists. Is this the expected behaviour?
        for (File file : files) {
            if (file != null) {
                if (file.getAbsolutePath().startsWith(TEMP_DIR)) {
                    delete(file);
                    File parent = file.getAbsoluteFile().getParentFile();
                    if (parent != null && !(parent.getAbsolutePath().equals(TEMP_DIR))) {
                        parent.deleteOnExit();
                    }
                }
            }
        }
    }

    /**
     * Delete the given files and all the files with the same name but different
     * extension. If some file is <code>null</code> just doesn't process it and
     * continue to the next element of the array
     *
     * @param files
     *              the files to delete
     */
    private static void delete(File... files) {
        //FIXME this will delete the files if the JVM exists. Is this the expected behaviour?
        for (File file : files) {
            if (file == null) {
                continue;
            }
            final String baseName = getBaseName(file);
            File[] list = file.getAbsoluteFile().getParentFile().listFiles(new PrefixFileFilter(baseName));
            for (File f : list) {
                f.deleteOnExit();
            }

            file.deleteOnExit();
        }
    }

    private static String getBaseName(File file) {
        return file.getName().substring(0, file.getName().lastIndexOf('.'));
    }

    private static class PrefixFileFilter implements FileFilter {
        private final String baseName;

        PrefixFileFilter(String baseName) {
            this.baseName = baseName;
        }

        @Override
        public boolean accept(File pathname) {
            return pathname.getName().startsWith(baseName);
        }
    }
}
