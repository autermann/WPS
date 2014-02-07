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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Type;

import org.n52.wps.algorithm.descriptor.OutputDescriptor;
import org.n52.wps.io.data.IData;

/**
 * TODO JavaDoc
 * @author Christian Autermann
 */
public abstract class OutputBinding<M extends AccessibleObject & Member> extends DataBinding<M, OutputDescriptor> {

    private Constructor<? extends IData> bindingConstructor;

    public OutputBinding(M member) {
        super(member);
    }

    protected boolean checkType() {
        return getConstructor() != null;
    }

    public IData bindOutputValue(Object outputValue) {
        try {
            if (isTypeEnum()) {
                outputValue = ((Enum<?>) outputValue).name();
            }
            return getConstructor().newInstance(outputValue);
        } catch (InstantiationException | SecurityException | IllegalAccessException ex) {
            throw new RuntimeException("Internal error processing outputs", ex);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            throw new RuntimeException(cause.getMessage(), cause);
        }
    }

    public abstract IData get(Object annotatedInstance);

    private synchronized Constructor<? extends IData> getConstructor() {
        if (bindingConstructor == null) {
            try {
                Class<? extends IData> bindingClass = getDescriptor().getBinding();
                Class<?> outputPayloadClass = bindingClass.getMethod("getPayload", (Class<?>[]) null).getReturnType();
                Type bindingPayloadType = getPayloadType();
                if (bindingPayloadType instanceof Class<?>) {
                    Class<?> bindingPayloadClass = (Class<?>) bindingPayloadType;
                    if (bindingPayloadClass.isAssignableFrom(outputPayloadClass)) {
                        bindingConstructor = bindingClass.getConstructor(bindingPayloadClass);
                    }
                }
            } catch (NoSuchMethodException e) {
                // error handling on fall-through
            }
        }
        return bindingConstructor;
    }

}
