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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.n52.wps.io.LiteralDataFactory;
import org.n52.wps.io.data.ILiteralData;

/**
 * @author Christian Autermann
 * @param <B> the type of this builder
 */
public abstract class LiteralDataInputDescriptorBuilder<B extends LiteralDataInputDescriptorBuilder<B>>
        extends InputDescriptorBuilder<B> {

    private final String dataType;
    private String defaultValue;
    private List<String> allowedValues;

    protected LiteralDataInputDescriptorBuilder(String identifier, Class<? extends ILiteralData> binding) {
        super(identifier, binding);
        this.dataType = checkNotNull(LiteralDataFactory.getTypeforBindingType(binding),
                                     "Unable to resolve XML DataType for binding class %s", binding);
    }

    public B defaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return self();
    }

    public <E extends Enum<E>> B allowedValues(Class<E> allowedValues) {
        Enum[] constants = allowedValues.getEnumConstants();
        List<String> names = new ArrayList<>(constants.length);
        for (Enum<?> constant : constants) {
            names.add(constant.name());
        }
        return allowedValues(names);
    }

    public B allowedValues(String[] allowedValues) {
        return allowedValues(Arrays.asList(allowedValues));
    }

    public B allowedValues(List<String> allowedValues) {
        this.allowedValues = allowedValues;
        return self();
    }

    @Override
    public LiteralDataInputDescriptor build() {
        return new LiteralDataInputDescriptor(this);
    }

    protected String getDataType() {
        return dataType;
    }

    protected String getDefaultValue() {
        return defaultValue;
    }

    protected List<String> getAllowedValues() {
        return allowedValues;
    }

}
