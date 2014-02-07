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

import java.math.BigInteger;

import org.n52.test.mock.MockEnum;
import org.n52.wps.io.data.IData;

import junit.framework.TestCase;

/**
 *
 * @author tkunicki
 */
public class InputDataDescriptorTest extends TestCase {
    
    public InputDataDescriptorTest(String testName) {
        super(testName);
    }

    public void testMinOccurs() {
        InputDescriptor inputDescriptor = null;
        
        // test default minOccurs is 1
        inputDescriptor = (new InputDescriptorImpl.Builder()).build();
        assertEquals(BigInteger.valueOf(1), inputDescriptor.getMinOccurs());
        
        // test default minOccurs is 1, that we set it again doesn't matter
        inputDescriptor = (new InputDescriptorImpl.Builder()).minOccurs(1).build();
        assertEquals(BigInteger.valueOf(1), inputDescriptor.getMinOccurs());
        // the other API
        inputDescriptor = (new InputDescriptorImpl.Builder()).minOccurs(BigInteger.valueOf(1)).build();
        assertEquals(BigInteger.valueOf(1), inputDescriptor.getMinOccurs());
        
        // test that 0 is OK
        inputDescriptor = (new InputDescriptorImpl.Builder()).minOccurs(0).build();
        assertEquals(BigInteger.valueOf(0), inputDescriptor.getMinOccurs());
        // the other API
        inputDescriptor = (new InputDescriptorImpl.Builder()).minOccurs(BigInteger.valueOf(0)).build();
        assertEquals(BigInteger.valueOf(0), inputDescriptor.getMinOccurs());
        
        // test fail early on < 0
        boolean thrown = false;
        try {
            (new InputDescriptorImpl.Builder()).minOccurs(-1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);
        // The other API
        thrown = false;
        try {
            (new InputDescriptorImpl.Builder()).minOccurs(BigInteger.valueOf(-1));
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);
        
        // test that minOccurs can't be > maxOccurs
        thrown = false;
        try {
            (new InputDescriptorImpl.Builder()).minOccurs(2).build();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            thrown = true;
        }
        assertTrue(thrown);
        // The other API
        thrown = false;
        try {
            (new InputDescriptorImpl.Builder()).minOccurs(BigInteger.valueOf(2)).build();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            thrown = true;
        }
        assertTrue(thrown);
    }

    public void testMaxOccurs() {
        
        InputDescriptor inputDescriptor = null;
        
        // test default maxOccurs is 1
        inputDescriptor = (new InputDescriptorImpl.Builder()).build();
        assertEquals(BigInteger.valueOf(1), inputDescriptor.getMaxOccurs());
        
        // test default maxOccurs is 1, that we set it again doesn't matter
        inputDescriptor = (new InputDescriptorImpl.Builder()).maxOccurs(1).build();
        assertEquals(BigInteger.valueOf(1), inputDescriptor.getMaxOccurs());
        // the other API
        inputDescriptor = (new InputDescriptorImpl.Builder()).maxOccurs(BigInteger.valueOf(1)).build();
        assertEquals(BigInteger.valueOf(1), inputDescriptor.getMaxOccurs());
        
        // test that we can set maxOccurs value > 1
        inputDescriptor = (new InputDescriptorImpl.Builder()).maxOccurs(2).build();
        assertEquals(BigInteger.valueOf(2), inputDescriptor.getMaxOccurs());
        // the other API
        inputDescriptor = (new InputDescriptorImpl.Builder()).maxOccurs(BigInteger.valueOf(2)).build();
        assertEquals(BigInteger.valueOf(2), inputDescriptor.getMaxOccurs());
        
        // test that we set maxOccurs to number of enum constants
        inputDescriptor = (new InputDescriptorImpl.Builder()).maxOccurs(MockEnum.class).build();
        assertEquals(BigInteger.valueOf(MockEnum.values().length), inputDescriptor.getMaxOccurs());
        
        // test fail-early for maxOccurs < 1;
        boolean thrown = false;
        try {
            (new InputDescriptorImpl.Builder()).maxOccurs(0);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);
        // the other API
        thrown = false;
        try {
            (new InputDescriptorImpl.Builder()).maxOccurs(BigInteger.valueOf(0));
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);
        
        // test maxOccurs can be < minOccurs even if both are non-default
        thrown = false;
        try {
            (new InputDescriptorImpl.Builder()).
                    minOccurs(3).
                    maxOccurs(2).
                    build();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            thrown = true;
        }
        assertTrue(thrown);
        // the other API
        thrown = false;
        try {
            (new InputDescriptorImpl.Builder()).
                    minOccurs(BigInteger.valueOf(3)).
                    maxOccurs(BigInteger.valueOf(2)).
                    build();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            thrown = true;
        }
        assertTrue(thrown);
    }

    public static class InputDescriptorImpl extends InputDescriptor {
        private InputDescriptorImpl(Builder builder) { super(builder); }

        public static class Builder extends InputDescriptorBuilder<Builder> {
            Builder() {
                super("mock_identifier", IData.class);
            }
            @Override protected Builder self() { return this;  }
            @Override public InputDescriptorImpl build() { return new InputDescriptorImpl(this); }
        }
    }
}
