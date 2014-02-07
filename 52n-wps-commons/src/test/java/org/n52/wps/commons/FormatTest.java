/**
 * Copyright (C) 2006 - 2014 52°North Initiative for Geospatial Open Source
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
package org.n52.wps.commons;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class FormatTest {
    private final String mimeType = "mimeType";
    private final String encoding = "encoding";
    private final String schema = "schema";
    private final String altMimeType = mimeType + "2";
    private final String altSchema = schema + "2";
    private final String altEncoding = encoding + "2";
//
//    private final Format defaultFormat = new Format("default" + mimeType,
//                                                    "default" + encoding,
//                                                    "default" + schema);
//
//    private final Format expected                   = new Format(mimeType, encoding, schema);
//    private final Format withoutMimeType            = new Format(null,     encoding, schema);
//    private final Format withoutEncoding            = new Format(mimeType, null,     schema);
//    private final Format withoutSchema              = new Format(mimeType, encoding, null  );
//    private final Format withoutEncodingAndSchema   = new Format(mimeType, null,     null  );
//    private final Format withoutMimeTypeAndEncoding = new Format(null,     null,     schema);
//    private final Format withoutMimeTypeAndSchema   = new Format(null,     encoding, null  );
//    private final Format empty                      = new Format(null,     null,     null  );
//
    @Rule
    public final ErrorCollector errors = new ErrorCollector();
//
//    @Test
//    public void testSelectWithAltMimeTypeAndSchema() throws ExceptionReport {
//        Set<Format> supported = Sets.newHashSet(
//                defaultFormat,
//                expected,
//                expected.withMimeType(altMimeType).withSchema(altSchema)
//        );
//
//        check(expected, defaultFormat, supported, expected);
//        check(withoutMimeType, defaultFormat, supported, expected);
//        check(withoutEncoding, defaultFormat, supported, expected);
//        check(withoutSchema, defaultFormat, supported, expected);
//        check(withoutEncodingAndSchema, defaultFormat, supported, expected);
//        check(withoutMimeTypeAndEncoding, defaultFormat, supported, expected);
//        check(withoutMimeTypeAndSchema, defaultFormat, supported, null);
//        check(empty, defaultFormat, supported, defaultFormat);
//
//    }
//
//    @Test
//    public void testSelectWithAltEncodingAndSchema() {
//        Set<Format> supported = Sets.newHashSet(
//                defaultFormat,
//                expected,
//                expected.withEncoding(altEncoding).withSchema(altSchema)
//        );
//
//        check(expected, defaultFormat, supported, expected);
//        check(withoutMimeType, defaultFormat, supported, expected);
//        check(withoutEncoding, defaultFormat, supported, expected);
//        check(withoutSchema, defaultFormat, supported, expected);
//        check(withoutEncodingAndSchema, defaultFormat, supported, null);
//        check(withoutMimeTypeAndEncoding, defaultFormat, supported, expected);
//        check(withoutMimeTypeAndSchema, defaultFormat, supported, expected);
//        check(empty, defaultFormat, supported, defaultFormat);
//    }
//
//    @Test
//    public void testSelectWithAltMimeTypeAndEncoding() {
//        Set<Format> supported = Sets.newHashSet(
//                defaultFormat,
//                expected,
//                expected.withMimeType(altMimeType).withEncoding(altEncoding)
//        );
//
//        check(expected, defaultFormat, supported, expected);
//        check(withoutMimeType, defaultFormat, supported, expected);
//        check(withoutEncoding, defaultFormat, supported, expected);
//        check(withoutSchema, defaultFormat, supported, expected);
//        check(withoutEncodingAndSchema, defaultFormat, supported, expected);
//        check(withoutMimeTypeAndEncoding, defaultFormat, supported, null);
//        check(withoutMimeTypeAndSchema, defaultFormat, supported, expected);
//        check(empty, defaultFormat, supported, defaultFormat);
//    }
//
//    @Test
//    public void testSelectWithAltSchema() {
//        Set<Format> supported = Sets.newHashSet(
//                defaultFormat,
//                expected,
//                expected.withSchema(altSchema)
//        );
//
//        check(expected, defaultFormat, supported, expected);
//        check(withoutMimeType, defaultFormat, supported, expected);
//        check(withoutEncoding, defaultFormat, supported, expected);
//        check(withoutSchema, defaultFormat, supported, null);
//        check(withoutEncodingAndSchema, defaultFormat, supported, null);
//        check(withoutMimeTypeAndEncoding, defaultFormat, supported, expected);
//        check(withoutMimeTypeAndSchema, defaultFormat, supported, null);
//        check(empty, defaultFormat, supported, defaultFormat);
//    }
//
//    @Test
//    public void testSelectWithAltEncoding() {
//        Set<Format> supported = Sets.newHashSet(
//                defaultFormat,
//                expected,
//                expected.withEncoding(altEncoding)
//        );
//
//        check(expected, defaultFormat, supported, expected);
//        check(withoutMimeType, defaultFormat, supported, expected);
//        check(withoutEncoding, defaultFormat, supported, null);
//        check(withoutSchema, defaultFormat, supported, expected);
//        check(withoutEncodingAndSchema, defaultFormat, supported, null);
//        check(withoutMimeTypeAndEncoding, defaultFormat, supported, null);
//        check(withoutMimeTypeAndSchema, defaultFormat, supported, expected);
//        check(empty, defaultFormat, supported, defaultFormat);
//    }
//
//    @Test
//    public void testSelectWithAltMimeType() {
//        Set<Format> supported = Sets.newHashSet(
//                defaultFormat,
//                expected,
//                expected.withMimeType(altMimeType)
//        );
//
//        check(expected, defaultFormat, supported, expected);
//        check(withoutMimeType, defaultFormat, supported, null);
//        check(withoutEncoding, defaultFormat, supported, expected);
//        check(withoutSchema, defaultFormat, supported, expected);
//        check(withoutEncodingAndSchema, defaultFormat, supported, expected);
//        check(withoutMimeTypeAndEncoding, defaultFormat, supported, null);
//        check(withoutMimeTypeAndSchema, defaultFormat, supported, null);
//        check(empty, defaultFormat, supported, defaultFormat);
//    }
//
//    @Test
//    public void testSelectWithExpected() {
//        Set<Format> supported = Sets.newHashSet(
//                defaultFormat,
//                expected
//        );
//
//        check(expected, defaultFormat, supported, expected);
//        check(withoutMimeType, defaultFormat, supported, expected);
//        check(withoutEncoding, defaultFormat, supported, expected);
//        check(withoutSchema, defaultFormat, supported, expected);
//        check(withoutEncodingAndSchema, defaultFormat, supported, expected);
//        check(withoutMimeTypeAndEncoding, defaultFormat, supported, expected);
//        check(withoutMimeTypeAndSchema, defaultFormat, supported, expected);
//        check(empty, defaultFormat, supported, defaultFormat);
//    }
//
//    @Test
//    public void testSelectOnlyMimeType() {
//        Format wildcard = new Format(mimeType);
//        Set<Format> supported = Sets.newHashSet(
//                defaultFormat,
//                expected,
//                wildcard
//        );
//
//        check(expected, defaultFormat, supported, expected);
//        check(withoutMimeType, defaultFormat, supported, expected);
//        check(withoutEncoding, defaultFormat, supported, expected);
//        check(withoutSchema, defaultFormat, supported, expected);
//        check(withoutEncodingAndSchema, defaultFormat, supported, null);
//        check(withoutMimeTypeAndEncoding, defaultFormat, supported, expected);
//        check(withoutMimeTypeAndSchema, defaultFormat, supported, expected);
//        check(empty, defaultFormat, supported, defaultFormat);
//    }
//
//    @Test
//    public void testSelectOnlyMimeType2() {
//        Format wildcard = new Format(altMimeType);
//        Set<Format> supported = Sets.newHashSet(
//                defaultFormat,
//                expected,
//                wildcard
//        );
//
//        check(expected.withMimeType(altMimeType), defaultFormat, supported, wildcard);
//        check(withoutMimeType, defaultFormat, supported, expected);
//        check(withoutEncoding.withMimeType(altMimeType), defaultFormat, supported, wildcard);
//        check(withoutSchema.withMimeType(altMimeType), defaultFormat, supported, wildcard);
//        check(withoutEncodingAndSchema.withMimeType(altMimeType), defaultFormat, supported, wildcard);
//        check(withoutMimeTypeAndEncoding, defaultFormat, supported, expected);
//        check(withoutMimeTypeAndSchema, defaultFormat, supported, expected);
//        check(empty, defaultFormat, supported, defaultFormat);
//    }
//
//    @Test
//    public void testSelectWithPermutation() {
//        Set<Format> supported = new FormatPermutation(
//                Lists.newArrayList(mimeType, altMimeType),
//                Lists.newArrayList(encoding, altEncoding),
//                Lists.newArrayList(schema, altSchema)
//        );
//
//        check(expected, defaultFormat, supported, expected);
//        check(withoutMimeType, defaultFormat, supported, null);
//        check(withoutEncoding, defaultFormat, supported, null);
//        check(withoutSchema, defaultFormat, supported, null);
//        check(withoutEncodingAndSchema, defaultFormat, supported, null);
//        check(withoutMimeTypeAndEncoding, defaultFormat, supported, null);
//        check(withoutMimeTypeAndSchema, defaultFormat, supported, null);
//        check(empty, defaultFormat, supported, defaultFormat);
//    }
//
//    public void check(Format test,
//                      Format defaultFormat, Set<Format> supported,
//                      Format expected) {
//        try {
//            Format result = test.select(defaultFormat, supported);
//            errors.checkThat(result, is(notNullValue()));
//            errors.checkThat(result, is(expected));
//        } catch (ExceptionReport ex) {
//            if (expected != null) {
//                errors.addError(ex);
//            }
//        }
//    }

    @Test
    public void testWithoutEncoding() {
        Format format = new Format(mimeType, encoding, schema);
        errors.checkThat(format.withoutEncoding(), is(equalTo(new Format(mimeType, null, schema))));
        errors.checkThat(format.withoutEncoding().hasEncoding(), is(false));
        errors.checkThat(format.withoutEncoding().getEncoding().isPresent(), is(false));
    }

    @Test
    public void testWithoutSchema() {
        Format format = new Format(mimeType, encoding, schema);
        errors.checkThat(format.withoutSchema(), is(equalTo(new Format(mimeType, encoding, null))));
        errors.checkThat(format.withoutSchema().hasSchema(), is(false));
        errors.checkThat(format.withoutSchema().getSchema().isPresent(), is(false));
    }

    @Test
    public void testWithoutMimeType() {
        Format format = new Format(mimeType, encoding, schema);
        errors.checkThat(format.withoutMimeType(), is(equalTo(new Format(null, encoding, schema))));
        errors.checkThat(format.withoutMimeType().hasMimeType(), is(false));
        errors.checkThat(format.withoutMimeType().getMimeType().isPresent(), is(false));
    }

    @Test
    public void testNullValues() {
        Format format = new Format(null, null, null);
        assertThat(format.getMimeType(), is(notNullValue()));
        assertThat(format.getEncoding(), is(notNullValue()));
        assertThat(format.getSchema(), is(notNullValue()));
        errors.checkThat(format.hasMimeType(), is(false));
        errors.checkThat(format.getMimeType().isPresent(), is(false));
        errors.checkThat(format.hasEncoding(), is(false));
        errors.checkThat(format.getEncoding().isPresent(), is(false));
        errors.checkThat(format.hasSchema(), is(false));
        errors.checkThat(format.getSchema().isPresent(), is(false));
    }

    @Test
    public void testCaseInsensitivity() {
        Format lowercase = new Format(mimeType.toLowerCase(), encoding.toLowerCase(), schema.toLowerCase());
        errors.checkThat(lowercase.hasMimeType(mimeType.toUpperCase()), is(true));
        errors.checkThat(lowercase.hasEncoding(encoding.toUpperCase()), is(true));
        errors.checkThat(lowercase.hasSchema(schema.toUpperCase()), is(true));
        Format uppercase = new Format(mimeType.toUpperCase(), encoding.toUpperCase(), schema.toUpperCase());
        errors.checkThat(uppercase.hasMimeType(mimeType.toLowerCase()), is(true));
        errors.checkThat(uppercase.hasEncoding(encoding.toLowerCase()), is(true));
        errors.checkThat(uppercase.hasSchema(schema.toLowerCase()), is(true));
    }

    @Test
    public void testWithMethods() {
        Format format = new Format(mimeType, encoding, schema);
        errors.checkThat(format.withEncoding(altEncoding), is(new Format(mimeType, altEncoding, schema)));
        errors.checkThat(format.withSchema(altSchema), is(new Format(mimeType, encoding, altSchema)));
        errors.checkThat(format.withMimeType(altMimeType), is(new Format(altMimeType, encoding, schema)));
    }
}
