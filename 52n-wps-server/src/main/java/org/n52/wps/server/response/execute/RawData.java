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
package org.n52.wps.server.response.execute;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.n52.wps.commons.Format;
import org.n52.wps.io.GeneratorFactory;
import org.n52.wps.io.IGenerator;
import org.n52.wps.io.data.IBBOXData;
import org.n52.wps.io.data.IComplexData;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.ILiteralData;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.NoApplicableCodeException;

import com.google.common.base.Joiner;
import com.google.common.primitives.Doubles;

/*
 * @author foerster
 *
 */
public abstract class RawData {
    private static final Logger LOGGER = LoggerFactory.getLogger(RawData.class);
    private static final String DEFAULT_ENCODING = "UTF-8";

    private final IData data;
    private final Format format;

    public RawData(IData data, Format format) {
        this.data = checkNotNull(data);
        this.format = checkNotNull(format);
    }

    public Format getFormat() {
        return format;
    }

    public IData getPayload() {
        return data;
    }

    public InputStream getAsStream() throws ExceptionReport {
        try {
            return encode();
        } catch (IOException ex) {
            throw new NoApplicableCodeException("Error while generating raw data out of the process result").causedBy(ex);
        }
    }

    public abstract InputStream encode()
            throws ExceptionReport, IOException;

    public static RawData of(ILiteralData data) {
        return new LiteralRawData(data);
    }

    public static RawData of(IBBOXData data) {
        return new BoundingBoxRawData(data);
    }

    public static RawData of(IComplexData data, Format format) {
        return new ComplexRawData(data, format);

    }

    private static class ComplexRawData extends RawData {
        ComplexRawData(IData data, Format format) {
            super(data, format);
        }

        private IGenerator getGenerator() throws ExceptionReport {
            LOGGER.debug("Looking for matching Generator ... {}", getFormat());
            IGenerator generator = GeneratorFactory.getInstance()
                    .getGenerator(getFormat(), getPayload().getClass());
            if (generator == null) {
                throw new NoApplicableCodeException("Could not find an generator for format %s and binding %s", getFormat(), getPayload()
                        .getClass());
            }
            LOGGER.info("Using generator {} for format: {}",
                        generator.getClass().getName(), getFormat());
            return generator;

        }

        @Override
        public InputStream encode() throws IOException, ExceptionReport {
            return getGenerator().generate(getPayload(), getFormat());
        }
    }

    private static class BoundingBoxRawData extends RawData {
        private static final Joiner JOINER = Joiner.on(" ");
        BoundingBoxRawData(IData data) {
            super(data, new Format("text/xml", DEFAULT_ENCODING));
        }

        @Override
        public InputStream encode() throws IOException {

//        DataType dt = DataType.Factory.newInstance();
//        BoundingBoxType bbd = dt.addNewBoundingBoxData();
            IBBOXData data = (IBBOXData) getPayload();

            StringBuilder builder = new StringBuilder();
            builder.append("<wps:BoundingBoxData xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\"");
            if (data.getCRS() != null && !data.getCRS().isEmpty()) {
                builder.append(" crs=\"").append(data.getCRS()).append('"');
            }
            builder.append(" dimensions=\"").append(data.getDimension()).append('"');
            builder.append(">");
            builder.append("<ows:LowerCorner>");
            JOINER.appendTo(builder, Doubles.asList(data.getLowerCorner()));
            builder.append("</ows:LowerCorner>");
            builder.append("<ows:UpperCorner>");
            JOINER.appendTo(builder, Doubles.asList(data.getUpperCorner()));
            builder.append("</ows:UpperCorner>");
            builder.append("</wps:BoundingBoxData>");
            return new ByteArrayInputStream(builder.toString().getBytes(DEFAULT_ENCODING));
        }

    }

    private static class LiteralRawData extends RawData {
        LiteralRawData(IData data) {
            super(data, new Format("text/string", DEFAULT_ENCODING));
        }

        @Override
        public InputStream encode() throws IOException {
            String result = String.valueOf(getPayload().getPayload());
            String encoding = getFormat().getEncoding().or(DEFAULT_ENCODING);
            return new ByteArrayInputStream(result.getBytes(encoding));
        }
    }

}
