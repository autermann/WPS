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

import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.n52.wps.io.data.ILiteralData;
import org.n52.wps.io.datahandler.literal.LiteralBase64Parser;
import org.n52.wps.io.datahandler.literal.LiteralBigDecimalParser;
import org.n52.wps.io.datahandler.literal.LiteralBigIntegerParser;
import org.n52.wps.io.datahandler.literal.LiteralBooleanParser;
import org.n52.wps.io.datahandler.literal.LiteralByteParser;
import org.n52.wps.io.datahandler.literal.LiteralDataParser;
import org.n52.wps.io.datahandler.literal.LiteralDateTimeParser;
import org.n52.wps.io.datahandler.literal.LiteralDoubleParser;
import org.n52.wps.io.datahandler.literal.LiteralFloatParser;
import org.n52.wps.io.datahandler.literal.LiteralIntegerParser;
import org.n52.wps.io.datahandler.literal.LiteralLongParser;
import org.n52.wps.io.datahandler.literal.LiteralShortParser;
import org.n52.wps.io.datahandler.literal.LiteralStringParser;
import org.n52.wps.io.datahandler.literal.LiteralURIParser;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.NoApplicableCodeException;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.primitives.Primitives;

/**
 * Extensible factory for {@link ILiteralData} bindings using
 * {@link LiteralDataParser}s.
 *
 * @author Christian Autermann
 */
public class LiteralDataFactory {
    /**
     * Boolean data type: {@value}
     */
    public static final String XS_BOOLEAN = "xs:boolean";
    /**
     * Byte data type: {@value}
     */
    public static final String XS_BYTE = "xs:byte";
    /**
     * Short data type: {@value}
     */
    public static final String XS_SHORT = "xs:short";
    /**
     * Integer data type: {@value}
     */
    public static final String XS_INTEGER = "xs:integer";
    /**
     * Int data type: {@value}
     */
    public static final String XS_INT = "xs:int";
    /**
     * Long data type: {@value}
     */
    public static final String XS_LONG = "xs:long";
    /**
     * Double data type: {@value}
     */
    public static final String XS_DOUBLE = "xs:double";
    /**
     * Float data type: {@value}
     */
    public static final String XS_FLOAT = "xs:float";
    /**
     * String data type: {@value}
     */
    public static final String XS_STRING = "xs:string";
    /**
     * URI data type: {@value}
     */
    public static final String XS_ANY_URI = "xs:anyURI";
    /**
     * Base64 data type: {@value}
     */
    public static final String XS_BASE64_BINARY = "xs:base64Binary";
    /**
     * Date data type: {@value}
     */
    public static final String XS_DATE = "xs:date";
    /**
     * Decimal data type: {@value}
     */
    public static final String XS_DECIMAL = "xs:decimal";
    /**
     * DateTime data type: {@value}
     */
    public static final String XS_DATE_TIME = "xs:dateTime";

    private static final Map<String, LiteralDataParser> byURI = Maps
            .newHashMap();
    private static final Map<Class<? extends ILiteralData>, LiteralDataParser> byBinding
            = Maps.newHashMap();
    private static final Map<Class<?>, LiteralDataParser> byPayload = Maps
            .newHashMap();
    private static final ReadWriteLock lock = new ReentrantReadWriteLock();

    static {
        register(new LiteralBooleanParser(XS_BOOLEAN));
        register(new LiteralByteParser(XS_BYTE));
        register(new LiteralShortParser(XS_SHORT));
        register(new LiteralBigIntegerParser(XS_INTEGER));
        register(new LiteralIntegerParser(XS_INT));
        register(new LiteralLongParser(XS_LONG));
        register(new LiteralDoubleParser(XS_DOUBLE));
        register(new LiteralFloatParser(XS_FLOAT));
        register(new LiteralStringParser(XS_STRING));
        register(new LiteralURIParser(XS_ANY_URI));
        register(new LiteralBase64Parser(XS_BASE64_BINARY));
        register(new LiteralDateTimeParser(XS_DATE));
        register(new LiteralDateTimeParser(XS_DATE_TIME));
        register(new LiteralBigDecimalParser(XS_DECIMAL));
    }

    private LiteralDataFactory() {
    }

    /**
     * Registers a new {@link LiteralDataParser} to be used during encoding and
     * decoding.
     *
     * @param parser the parser
     */
    public static void register(LiteralDataParser parser) {
        Preconditions.checkNotNull(parser);
        lock.writeLock().lock();
        try {
            byURI.put(parser.getURI().toLowerCase(), parser);
            byBinding.put(parser.getBindingType(), parser);
            byPayload.put(parser.getPayloadType(), parser);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Gets a parser from the specified map using this classes lock.
     *
     * @param <T> the key type
     * @param map the map
     * @param key the key
     *
     * @return the parser, or {@code null} if there is no parser for the
     *         supplied key
     */
    private static <T> LiteralDataParser getParser(
            Map<T, LiteralDataParser> map, T key) {
        lock.readLock().lock();
        try {
            return map.get(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the {@link LiteralDataParser} for the specified type.
     *
     * @param type the type
     *
     * @return the parser, or {@code null} if there is no parser for the
     *         supplied type
     */
    private static LiteralDataParser getParserForType(String type) {
        return getParser(byURI, type.toLowerCase());
    }

    /**
     * Gets the {@link LiteralDataParser} for the specified {@link ILiteralData}
     * class.
     *
     * @param bindingType the binding class
     *
     * @return the parser, or {@code null} if there is no parser for the
     *         supplied binding
     */
    private static LiteralDataParser getParserForBindingType(
            Class<? extends ILiteralData> bindingType) {
        return getParser(byBinding, bindingType);
    }

    /**
     * Gets the {@link LiteralDataParser} for the specified payload class.
     *
     * @param payloadType the payload class
     *
     * @return the parser, or {@code null} if there is no parser for the
     *         supplied payload class
     */
    private static LiteralDataParser getParserForPayloadType(
            Class<?> payloadType) {
        return getParser(byPayload, Primitives.wrap(payloadType));
    }

    /**
     * Transforms the supplied binding into a string value. Currently equivalent
     * to
     * <pre>
     * String.valueOf(obj.getPayload());
     * </pre>
     *
     * @param xmlDataTypeURI the type
     * @param obj            the data binding
     *
     * @return the string representing the data
     */
    public static String toString(String xmlDataTypeURI, ILiteralData obj) {
        return String.valueOf(obj.getPayload());
    }

    /**
     * Gets the {@link ILiteralData} binding class for the specified payload.
     *
     * @param payloadType the payload class
     *
     * @return the binding class
     */
    public static Class<? extends ILiteralData> getBindingForPayloadType(
            Class<?> payloadType) {
        return getBindingType(getParserForPayloadType(payloadType));
    }

    /**
     * Gets the {@link ILiteralData} binding class for the specified type.
     *
     * @param type the type
     *
     * @return the binding class
     */
    public static Class<? extends ILiteralData> getBindingForType(String type) {
        return getBindingType(getParserForType(type));
    }

    /**
     * Gets the payload type for the specified {@link ILiteralData} binding
     * class.
     *
     * @param bindingType the binding class
     *
     * @return the payload type
     */
    public static Class<?> getPayloadTypeForBindingType(
            Class<? extends ILiteralData> bindingType) {
        return getPayloadType(getParserForBindingType(bindingType));
    }

    /**
     * Gets the payload type for the specified data type.
     *
     * @param type the type
     *
     * @return the payload class
     */
    public static Class<?> getPayloadTypeForType(String type) {
        return getPayloadType(getParserForType(type));
    }

    /**
     * Gets the type for the specified {@link ILiteralData} binding class.
     *
     * @param bindingType the binding class
     *
     * @return the type
     */
    public static String getTypeforBindingType(
            Class<? extends ILiteralData> bindingType) {
        return getURI(getParserForBindingType(bindingType));
    }

    /**
     * Gets the type for the specified payload type.
     *
     * @param payloadType the payload class
     *
     * @return the type
     */
    public static String getTypeForPayloadType(Class<?> payloadType) {
        return getURI(getParserForPayloadType(payloadType));
    }

    /**
     * Get the binding type of the parser.
     *
     * @param p the parser
     *
     * @return the binding type, or {@code null} if the parser is {@code null}
     */
    private static Class<? extends ILiteralData> getBindingType(
            LiteralDataParser p) {
        return p == null ? null : p.getBindingType();
    }

    /**
     * Get the payload type of the parser.
     *
     * @param p the parser
     *
     * @return the payload type, or {@code null} if the parser is {@code null}
     */
    private static Class<?> getPayloadType(LiteralDataParser p) {
        return p == null ? null : p.getPayloadType();
    }

    /**
     * Get the URI of the parser.
     *
     * @param p the parser
     *
     * @return the URI, or {@code null} if the parser is {@code null}
     */
    private static String getURI(LiteralDataParser p) {
        return p == null ? null : p.getURI();
    }

    /**
     * Parses the supplied literal string value into a matching
     * {@link ILiteralData} binding.
     *
     * @param type  the data type (if it is {@code null}, {@code xs:string} is
     *              assumed
     * @param value the XML object String
     *
     * @return the parsed data
     *
     * @throws ExceptionReport if the input could not be parsed
     */
    public static ILiteralData create(String type, String value)
            throws ExceptionReport {
        LiteralDataParser p = getParserForType(type == null ? XS_STRING : type);
        if (p != null) {
            return p.parse(value.replace('\n', ' ').replace('\t', ' ').trim());
        } else {
            throw new NoApplicableCodeException("Can not parse literal data of type %s", type);
        }
    }

}
