/**
 * ﻿Copyright (C) 2007 - 2014 52°North Initiative for Geospatial Open Source
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
 *       • Apache License, version 2.0
 *       • Apache Software License, version 1.0
 *       • GNU Lesser General Public License, version 3
 *       • Mozilla Public License, versions 1.0, 1.1 and 2.0
 *       • Common Development and Distribution License (CDDL), version 1.0
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
package org.n52.wps.algorithm.descriptor;

import org.n52.wps.io.data.ILiteralData;

import junit.framework.TestCase;

/**
 *
 * @author tkunicki
 */
public class BoundDataDescriptorTest extends TestCase {

    public BoundDataDescriptorTest(String testName) {
        super(testName);
    }

    public void testBinding() {

        // Test fail-early, exception should be thrown if binding is 'null';
        boolean thrown = false;
        try {
            new BoundDescriptorImpl.Builder(null).build();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

        // make sure the class type we build with is the same as the one returned
        // by the constucted object
        BoundDescriptor descriptor
                = new BoundDescriptorImpl.Builder(MockNumberBinding.class).build();
        assertEquals(MockNumberBinding.class, descriptor.getBinding());

        descriptor = (new BoundDescriptorImpl.Builder(MockNumber.class)).build();
        assertEquals(MockNumber.class, descriptor.getBinding());
    }

    public static class BoundDescriptorImpl extends BoundDescriptor {
        private BoundDescriptorImpl(Builder builder) {
            super(builder);
        }

        public static class Builder extends BoundDescriptorBuilder<Builder> {
            Builder(Class<? extends ILiteralData> binding) {
                super("mock_identifier", binding);
            }

            @Override
            protected Builder self() {
                return this;
            }

            public BoundDescriptorImpl build() {
                return new BoundDescriptorImpl(this);
            }
        }
    }

    public static class MockNumberBinding implements ILiteralData {
        private final MockNumber number;

        public MockNumberBinding(MockNumber number) {
            this.number = number;
        }

        @Override
        public Object getPayload() {
            return number;
        }

        @Override
        public Class<?> getSupportedClass() {
            return MockNumber.class;
        }
    }

    public static class MockNumber extends Number implements ILiteralData {
        private static final long serialVersionUID = 1L;
        @Override
        public int intValue() {
            return Integer.MAX_VALUE;
        }

        @Override
        public long longValue() {
            return Long.MAX_VALUE;
        }

        @Override
        public float floatValue() {
            return Float.MAX_VALUE;
        }

        @Override
        public double doubleValue() {
            return Double.MAX_VALUE;
        }

        @Override
        public MockNumber getPayload() {
            return this;
        }

        @Override
        public Class<?> getSupportedClass() {
            return MockNumber.class;
        }
    }
}
