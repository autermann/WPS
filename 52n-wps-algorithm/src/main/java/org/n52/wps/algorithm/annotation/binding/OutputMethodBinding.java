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
package org.n52.wps.algorithm.annotation.binding;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.n52.wps.algorithm.annotation.AlgorithmAnnotationException;
import org.n52.wps.io.data.IData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christian Autermann
 */
public class OutputMethodBinding extends OutputBinding<Method> {
    private static final Logger LOGGER = LoggerFactory.getLogger(OutputMethodBinding.class);
    public OutputMethodBinding(Method method) {
        super(method);
    }

    @Override
    public Type getMemberType() {
        return getMember().getGenericReturnType();
    }

    @Override
    public void validate() throws AlgorithmAnnotationException {
        Method method = getMember();
        if (method.getParameterTypes().length != 0) {
            throw new AlgorithmAnnotationException("Method %s with output annotation can't be used, parameter count != 0", getMember());
        }
        if (!checkModifier()) {
            throw new AlgorithmAnnotationException("Method %s with output annotation can't be used, not public", getMember());
        }
        if (!checkType()) {
            throw new AlgorithmAnnotationException("Method %s with output annotation can't be used, unable to safely construct binding using method return type", getMember());
        }
    }

    @Override
    public IData get(Object annotatedInstance) {
        Object value;
        try {
            value = getMember().invoke(annotatedInstance);
        } catch (IllegalAccessException | IllegalArgumentException ex) {
            throw new RuntimeException("Internal error processing inputs", ex);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            throw new RuntimeException(cause.getMessage(), cause);
        }
        return value == null ? null : bindOutputValue(value);
    }

}
