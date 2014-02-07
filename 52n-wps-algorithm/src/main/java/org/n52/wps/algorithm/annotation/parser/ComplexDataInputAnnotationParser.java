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
package org.n52.wps.algorithm.annotation.parser;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import org.n52.wps.algorithm.annotation.ComplexDataInput;
import org.n52.wps.algorithm.annotation.binding.InputBinding;
import org.n52.wps.algorithm.annotation.binding.InputFieldBinding;
import org.n52.wps.algorithm.annotation.binding.InputMethodBinding;
import org.n52.wps.algorithm.descriptor.ComplexDataInputDescriptor;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 * @param <M>
 * @param <B>
 */
public abstract class ComplexDataInputAnnotationParser<M extends AccessibleObject & Member, B extends InputBinding<M>>
        extends InputAnnotationParser<ComplexDataInput, M, B> {

    public static final ComplexDataInputAnnotationParser<Field, InputBinding<Field>> FIELD
            = new ComplexDataInputAnnotationParser<Field, InputBinding<Field>>() {

                @Override
                protected InputBinding<Field> createBinding(Field member) {
                    return new InputFieldBinding(member);
                }

            };
    public static final ComplexDataInputAnnotationParser<Method, InputBinding<Method>> METHOD
            = new ComplexDataInputAnnotationParser<Method, InputBinding<Method>>() {

                @Override
                protected InputBinding<Method> createBinding(Method member) {
                    return new InputMethodBinding(member);
                }

            };

    @Override
    protected B parse(ComplexDataInput annotation, M member) {
        B annotatedBinding = createBinding(member);
        ComplexDataInputDescriptor descriptor
                = ComplexDataInputDescriptor
                .builder(annotation.identifier(), annotation.binding())
                .title(annotation.title()).abstrakt(annotation.abstrakt())
                .minOccurs(annotation.minOccurs())
                .maxOccurs(annotation.maxOccurs())
                .maximumMegaBytes(annotation.maximumMegaBytes()).build();
        annotatedBinding.setDescriptor(descriptor);
        return annotatedBinding;
    }

    @Override
    public Class<? extends ComplexDataInput> getSupportedAnnotation() {
        return ComplexDataInput.class;
    }

}
