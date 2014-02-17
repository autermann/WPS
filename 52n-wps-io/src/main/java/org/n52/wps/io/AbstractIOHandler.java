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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.n52.wps.FormatDocument;
import org.n52.wps.commons.Format;
import org.n52.wps.commons.FormatPermutation;
import org.n52.wps.commons.WPSConfig;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Extending subclasses of AbstractGenerator shall provide functionality to
 * generate serviceable output data for the processes offered by the 52N WPS
 * framework.
 *
 * @author Matthias Mueller
 *
 */
public abstract class AbstractIOHandler implements IOHandler {
    private final Set<Format> formats;
    private final Set<Class<?>> dataTypes;

    /**
     * A list of files that shall be deleted by destructor.
     * Convenience mechanism to delete temporary files that had
     * to be written during the generation procedure.
     */
    private final List<File> finalizeFiles = Lists.newLinkedList();

    public AbstractIOHandler(Set<Format> formats,
                             Set<Class<?>> dataTypes) {
        this.formats = checkNotNull(formats);
        this.dataTypes = checkNotNull(dataTypes);
    }

    public AbstractIOHandler(Set<Class<?>> dataTypes) {
        this.dataTypes = checkNotNull(dataTypes);
        this.formats = getFormatsForHandler();
    }

    public AbstractIOHandler(Class<?>... dataTypes) {
        this(Sets.newHashSet(dataTypes));
    }

    @Override
    public Set<Class<?>> getSupportedDataBindings() {
        return Collections.unmodifiableSet(dataTypes);
    }

    @Override
    public boolean isSupportedFormat(Format format) {
        return this.formats.contains(format);
    }

    @Override
    public Set<Format> getSupportedFormats() {
        return Collections.unmodifiableSet(this.formats);
    }

    @Override
    public boolean isSupportedDataBinding(Class<?> clazz) {
        return this.dataTypes.contains(clazz);
    }

    protected File registerTempFile(File file) {
        this.finalizeFiles.add(file);
        return file;
    }

    protected File registerTempFile() throws IOException {
        Path path = Files.createTempFile(getClass().getName(), ".tmp");
        return path.toFile();
    }

    protected <T extends Collection<File>> T registerTempFiles(T files) {
        this.finalizeFiles.addAll(files);
        return files;
    }

    /**
     * Destructor deletes generated temporary files.
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            for (File currentFile : finalizeFiles) {
                currentFile.delete();
            }
        } finally {
            super.finalize();
        }
    }

    private Set<Format> getFormatsForHandler() {
        // FIXME CONFIGURATION API
        if (this instanceof IGenerator) {
            return parseFormats(WPSConfig.getInstance()
                    .getFormatsForGeneratorClass(getClass().getName()));
        } else if (this instanceof IParser) {
            return parseFormats(WPSConfig.getInstance()
                    .getFormatsForParserClass(getClass().getName()));
        } else {
            return Collections.emptySet();
        }
    }

    private static FormatPermutation parseFormats(
            FormatDocument.Format[] formats) {
        List<String> mimeTypes = Lists.newLinkedList();
        List<String> encodings = Lists.newLinkedList();
        List<String> schemas = Lists.newLinkedList();

        for (FormatDocument.Format format : formats) {
            if (format.getMimetype() != null &&
                !format.getMimetype().isEmpty()) {
                mimeTypes.add(format.getMimetype());
            }
            if (format.getSchema() != null &&
                !format.getSchema().isEmpty()) {
                schemas.add(format.getSchema());
            }
            if (format.getEncoding() != null &&
                !format.getEncoding().isEmpty()) {
                encodings.add(format.getEncoding());
            } else {
                encodings.add(IOHandler.DEFAULT_ENCODING);
            }
        }
        return new FormatPermutation(mimeTypes, encodings, schemas);
    }

}
