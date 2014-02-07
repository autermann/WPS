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
package org.n52.wps.io.datahandler.generator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.apache.commons.codec.binary.Base64InputStream;
import org.n52.wps.commons.Format;
import org.n52.wps.io.AbstractIOHandler;
import org.n52.wps.io.IGenerator;
import org.n52.wps.io.data.IData;
import org.n52.wps.server.ExceptionReport;

/**
 * @author Matthias Mueller, TU Dresden
 *
 */
public abstract class AbstractGenerator extends AbstractIOHandler
        implements IGenerator {

    public AbstractGenerator(Set<Format> formats,
                             Set<Class<?>> dataTypes) {
        super(formats, dataTypes);
    }

    public AbstractGenerator(Set<Class<?>> dataTypes) {
        super(dataTypes);
    }

    public AbstractGenerator(Class<?>... dataTypes) {
        super(dataTypes);
    }

    @Override
    public InputStream generate(IData data, Format format)
            throws IOException, ExceptionReport {
        if (!format.hasEncoding()|| format.hasEncoding(DEFAULT_ENCODING)) {
            return generateStream(data, format);
        } else if (format.hasEncoding(ENCODING_BASE64)) {
            return new Base64InputStream(generate(data, format.withoutEncoding()), true);
        } else {
            throw new ExceptionReport("Unable to generate encoding " + format
                    .getEncoding().orNull(), ExceptionReport.NO_APPLICABLE_CODE);
        }
    }

    protected abstract InputStream generateStream(IData data, Format format)
            throws IOException, ExceptionReport;
}
