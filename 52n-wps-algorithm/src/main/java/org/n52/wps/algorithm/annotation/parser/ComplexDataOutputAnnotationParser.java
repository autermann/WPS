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
package org.n52.wps.algorithm.annotation.parser;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import org.n52.wps.algorithm.annotation.ComplexDataOutput;
import org.n52.wps.algorithm.annotation.binding.OutputBinding;
import org.n52.wps.algorithm.annotation.binding.OutputFieldBinding;
import org.n52.wps.algorithm.annotation.binding.OutputMethodBinding;
import org.n52.wps.algorithm.descriptor.ComplexDataOutputDescriptor;

/**
 * @author Christian Autermann
 */
public abstract class ComplexDataOutputAnnotationParser<M extends AccessibleObject & Member, B extends OutputBinding<M>>
        extends OutputAnnotationParser<ComplexDataOutput, M, B> {

    public static final ComplexDataOutputAnnotationParser<Method, OutputBinding<Method>> METHOD
            = new ComplexDataOutputAnnotationParser<Method, OutputBinding<Method>>() {

                @Override
                protected OutputBinding<Method> createBinding(Method member) {
                    return new OutputMethodBinding(member);
                }

            };

    public static final ComplexDataOutputAnnotationParser<Field, OutputBinding<Field>> FIELD
            = new ComplexDataOutputAnnotationParser<Field, OutputBinding<Field>>() {

                @Override
                protected OutputBinding<Field> createBinding(Field member) {
                    return new OutputFieldBinding(member);
                }
            };

    @Override
    protected B parse(ComplexDataOutput annotation, M member) {
        B annotatedBinding = createBinding(member);
        ComplexDataOutputDescriptor descriptor
                = ComplexDataOutputDescriptor
                .builder(annotation.identifier(), annotation.binding())
                .title(annotation.title()).abstrakt(annotation.abstrakt())
                .build();
        annotatedBinding.setDescriptor(descriptor);
        return annotatedBinding;
    }

    @Override
    public Class<? extends ComplexDataOutput> getSupportedAnnotation() {
        return ComplexDataOutput.class;
    }
}
