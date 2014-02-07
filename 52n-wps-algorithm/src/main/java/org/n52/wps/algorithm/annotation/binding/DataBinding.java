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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;
import java.lang.reflect.Type;

import org.n52.wps.algorithm.descriptor.BoundDescriptor;

import com.google.common.base.Objects;
import com.google.common.primitives.Primitives;

/**
 * TODO JavaDoc
 * @author Christian Autermann
 */
public abstract class DataBinding<M extends AccessibleObject & Member, D extends BoundDescriptor> extends AnnotationBinding<M> {

    private D descriptor;

    public DataBinding(M member) {
        super(member);
    }

    public void setDescriptor(D descriptor) {
        this.descriptor = descriptor;
    }

    public D getDescriptor() {
        return descriptor;
    }

    public abstract Type getMemberType();

    public Type getType() {
        return getMemberType();
    }

    public Type getPayloadType() {
        Type type = getType();
        if (isTypeEnum()) {
            return String.class;
        }
        if (type instanceof Class<?>) {
            Class<?> inputClass = (Class<?>) type;
            if (inputClass.isPrimitive()) {
                return Primitives.wrap(inputClass);
            }
        }
        return type;
    }

    public boolean isTypeEnum() {
        Type inputType = getType();
        return (inputType instanceof Class<?>) &&
               ((Class<?>) inputType).isEnum();
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("member", getMember())
                .add("descriptor", this.descriptor)
                .toString();
    }
}
