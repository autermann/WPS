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
package org.n52.wps.algorithm.annotation.binding;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

import org.n52.wps.algorithm.annotation.AlgorithmAnnotationException;
import org.n52.wps.io.data.IData;

/**
 * TODO JavaDoc
 * @author Christian Autermann
 */
public class OutputFieldBinding extends OutputBinding<Field> {
    public OutputFieldBinding(Field field) {
        super(field);
    }

    @Override
    public Type getMemberType() {
        return getMember().getGenericType();
    }

    @Override
    public void validate() throws AlgorithmAnnotationException {
        if (!checkModifier()) {
            throw new AlgorithmAnnotationException("Field %s with output annotation can't be used, not public.", getMember());
        }
        if (!checkType()) {
            throw new AlgorithmAnnotationException("Field %s with output annotation can't be used, unable to safely construct binding using field type", getMember());
        }
    }

    @Override
    public IData get(Object annotatedInstance) {
        Object value;
        try {
            value = getMember().get(annotatedInstance);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            throw new RuntimeException("Internal error processing inputs", ex);
        }
        return value == null ? null : bindOutputValue(value);
    }

}
