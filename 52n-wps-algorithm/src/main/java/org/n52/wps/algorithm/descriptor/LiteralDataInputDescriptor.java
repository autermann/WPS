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
