/**
 * ﻿Copyright (C) 2007
 * by 52 North Initiative for Geospatial Open Source Software GmbH
 *
 * Contact: Andreas Wytzisk
 * 52 North Initiative for Geospatial Open Source Software GmbH
 * Martin-Luther-King-Weg 24
 * 48155 Muenster, Germany
 * info@52north.org
 *
 * This program is free software; you can redistribute and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the
 * Free Software Foundation.
 *
 * This program is distributed WITHOUT ANY WARRANTY; even without the implied
 * WARRANTY OF MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program (see gnu-gpl v2.txt). If not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA or
 * visit the Free Software Foundation web page, http://www.fsf.org.
 */
package org.n52.wps.io;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.n52.wps.FormatDocument;
import org.n52.wps.commons.Format;
import org.n52.wps.commons.FormatPermutation;
import org.n52.wps.commons.WPSConfig;

import com.google.common.base.Preconditions;
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
    protected final List<File> finalizeFiles = Lists.newLinkedList();

    public AbstractIOHandler(Set<Format> formats,
                             Set<Class<?>> dataTypes) {
        this.formats = Preconditions.checkNotNull(formats);
        this.dataTypes = Preconditions.checkNotNull(dataTypes);
    }

    public AbstractIOHandler(Set<Class<?>> dataTypes) {
        this.dataTypes = Preconditions.checkNotNull(dataTypes);
        this.formats = getFormatsForHandler();
    }

    public AbstractIOHandler(Class<?>... dataTypes) {
        this(Sets.newHashSet(dataTypes));
    }

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

    protected void registerTempFile(File file) {
        this.finalizeFiles.add(file);
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
