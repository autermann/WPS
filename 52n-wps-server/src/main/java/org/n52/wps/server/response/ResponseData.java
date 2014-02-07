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
package org.n52.wps.server.response;

import net.opengis.wps.x100.OutputDescriptionType;
import net.opengis.wps.x100.ProcessDescriptionType;

import org.n52.wps.commons.Format;
import org.n52.wps.io.GeneratorFactory;
import org.n52.wps.io.IGenerator;
import org.n52.wps.io.data.IData;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.NoApplicableCodeException;
import org.n52.wps.server.RepositoryManager;
import org.n52.wps.server.request.FormatHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/*
 * @author foerster
 * This and the inheriting classes in charge of populating the ExecuteResponseDocument.
 */
public abstract class ResponseData {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ResponseData.class);

    private final IData obj;
    private final String id;
    private Format format;
    private IGenerator generator;
    private final String algorithmIdentifier;
    private final ProcessDescriptionType description;

    public ResponseData(IData obj, String id, Format format,
                        ProcessDescriptionType description) throws
            ExceptionReport {

        this.obj = obj;
        this.id = id;
        this.description = description;
        this.algorithmIdentifier = description.getIdentifier().getStringValue();

        OutputDescriptionType outputType = null;

        OutputDescriptionType[] describeProcessOutput = description
                .getProcessOutputs().getOutputArray();
        for (OutputDescriptionType tempOutputType : describeProcessOutput) {
            if (tempOutputType.getIdentifier().getStringValue()
                    .equalsIgnoreCase(id)) {
                outputType = tempOutputType;
            }
        }

        if (outputType == null) {
            throw new NullPointerException();
        }

        this.format = format == null ? null : new FormatHandler(outputType)
                .select(format);
    }

    protected void prepareGenerator() throws ExceptionReport {
        Class<?> algorithmOutput = RepositoryManager.getInstance()
                .getOutputDataTypeForAlgorithm(this.algorithmIdentifier, id);

        LOGGER.debug("Looking for matching Generator ... {}", format);

        this.generator = GeneratorFactory.getInstance()
                .getGenerator(this.format, algorithmOutput);

        if (this.generator != null) {
            LOGGER.info("Using generator {} for Schema: {}", generator
                    .getClass().getName(), format.getSchema().orNull());
        }
        if (this.generator == null) {
            throw new NoApplicableCodeException("Could not find an appropriate " +
                                                "generator based on given mimetype/schema/encoding for output");
        }
    }

    public Format getFormat() {
        return format;
    }

    protected void setFormat(Format format) {
        this.format = Preconditions.checkNotNull(format);
    }

    public IData getPayload() {
        return obj;
    }

    public String getId() {
        return this.id;
    }

    protected IGenerator getGenerator() {
        return this.generator;
    }

    public ProcessDescriptionType getDescription() {
        return description;
    }

}
