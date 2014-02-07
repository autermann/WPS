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
package org.n52.wps.algorithm.descriptor;

import java.util.Collections;
import java.util.List;

import org.n52.wps.io.data.ILiteralData;
import org.n52.wps.io.data.binding.literal.LiteralAnyURIBinding;
import org.n52.wps.io.data.binding.literal.LiteralBase64BinaryBinding;
import org.n52.wps.io.data.binding.literal.LiteralBooleanBinding;
import org.n52.wps.io.data.binding.literal.LiteralByteBinding;
import org.n52.wps.io.data.binding.literal.LiteralDateTimeBinding;
import org.n52.wps.io.data.binding.literal.LiteralDoubleBinding;
import org.n52.wps.io.data.binding.literal.LiteralFloatBinding;
import org.n52.wps.io.data.binding.literal.LiteralIntBinding;
import org.n52.wps.io.data.binding.literal.LiteralLongBinding;
import org.n52.wps.io.data.binding.literal.LiteralShortBinding;
import org.n52.wps.io.data.binding.literal.LiteralStringBinding;

import com.google.common.base.Preconditions;

/**
 *
 * @author tkunicki
 */
public class LiteralDataInputDescriptor extends InputDescriptor {

    private final String dataType;
    private final String defaultValue;
    private final List<String> allowedValues;

	protected LiteralDataInputDescriptor(LiteralDataInputDescriptorBuilder<?> builder) {
		super(builder);
        this.dataType = builder.getDataType();
		this.defaultValue = builder.getDefaultValue();
        if (builder.getAllowedValues() != null ) {
            this.allowedValues = builder.getAllowedValues();
        } else {
            this.allowedValues = Collections.emptyList();
        }
        // if allowedValues and defaultValue are set, make sure defaultValue is in set of allowedValues
        Preconditions.checkState(
                !hasAllowedValues() || !hasDefaultValue() || allowedValues.contains(defaultValue),
                "defaultValue of %s not in set of allowedValues", defaultValue);
	}

    public String getDataType() {
        return dataType;
    }

    public boolean hasDefaultValue() {
        return defaultValue != null && defaultValue.length() > 0;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public boolean hasAllowedValues() {
        return allowedValues != null && allowedValues.size() > 0;
    }

    public List<String> getAllowedValues() {
        return Collections.unmodifiableList(allowedValues);
    }

    public static LiteralDataInputDescriptorBuilder<?> builder(String identifier, Class<? extends ILiteralData> binding) {
        return new BuilderTyped(identifier, binding);
    }

    // utility functions, quite verbose...
    public static LiteralDataInputDescriptorBuilder<?> anyURIBuilder(String identifier) {
        return builder(identifier, LiteralAnyURIBinding.class);
    }

    public static LiteralDataInputDescriptorBuilder<?> base64BinaryBuilder(String identifier) {
        return builder(identifier, LiteralBase64BinaryBinding.class);
    }

    public static LiteralDataInputDescriptorBuilder<?> booleanBuilder(String identifier) {
        return builder(identifier, LiteralBooleanBinding.class);
    }

    public static LiteralDataInputDescriptorBuilder<?> byteBuilder(String identifier) {
        return builder(identifier, LiteralByteBinding.class);
    }

    public static LiteralDataInputDescriptorBuilder<?> dateTimeBuilder(String identifier) {
        return builder(identifier, LiteralDateTimeBinding.class);
    }
    
    public static LiteralDataInputDescriptorBuilder<?> doubleBuilder(String identifier) {
        return builder(identifier, LiteralDoubleBinding.class);
    }

    public static LiteralDataInputDescriptorBuilder<?> floatBuilder(String identifier) {
        return builder(identifier, LiteralFloatBinding.class);
    }

    public static LiteralDataInputDescriptorBuilder<?> intBuilder(String identifier) {
        return builder(identifier, LiteralIntBinding.class);
    }

    public static LiteralDataInputDescriptorBuilder<?> longBuilder(String identifier) {
        return builder(identifier, LiteralLongBinding.class);
    }

    public static LiteralDataInputDescriptorBuilder<?> shortBuilder(String identifier) {
        return builder(identifier, LiteralShortBinding.class);
    }

    public static LiteralDataInputDescriptorBuilder<?> stringBuilder(String identifier) {
        return builder(identifier, LiteralStringBinding.class);
    }

    private static class BuilderTyped extends LiteralDataInputDescriptorBuilder<BuilderTyped> {
        private BuilderTyped(String identifier, Class<? extends ILiteralData> binding) {
            super(identifier, binding);
        }

        @Override
        protected BuilderTyped self() {
            return this;
        }
    }
}
