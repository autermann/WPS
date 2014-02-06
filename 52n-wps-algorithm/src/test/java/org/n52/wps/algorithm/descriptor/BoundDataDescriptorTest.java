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
