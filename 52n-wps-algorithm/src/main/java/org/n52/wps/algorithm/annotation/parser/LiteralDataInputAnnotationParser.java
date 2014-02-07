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
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.n52.wps.algorithm.annotation.AlgorithmAnnotationException;
import org.n52.wps.algorithm.annotation.LiteralDataInput;
import org.n52.wps.algorithm.annotation.binding.InputBinding;
import org.n52.wps.algorithm.annotation.binding.InputFieldBinding;
import org.n52.wps.algorithm.annotation.binding.InputMethodBinding;
import org.n52.wps.algorithm.descriptor.LiteralDataInputDescriptor;
import org.n52.wps.algorithm.util.ClassUtil;
import org.n52.wps.io.BasicXMLTypeFactory;
import org.n52.wps.io.data.ILiteralData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO JavaDoc
 * @author Christian Autermann
 */
public abstract class LiteralDataInputAnnotationParser<M extends AccessibleObject & Member, B extends InputBinding<M>> extends InputAnnotationParser<LiteralDataInput, M, B> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LiteralDataInputAnnotationParser.class);

    public static final LiteralDataInputAnnotationParser<Method, InputBinding<Method>> METHOD
            = new LiteralDataInputAnnotationParser<Method, InputBinding<Method>>() {

                @Override
                protected InputBinding<Method> createBinding(Method member) {
                    return new InputMethodBinding(member);
                }

            };

    public static final LiteralDataInputAnnotationParser<Field, InputBinding<Field>> FIELD
            = new LiteralDataInputAnnotationParser<Field, InputBinding<Field>>() {

                @Override
                protected InputBinding<Field> createBinding(Field member) {
                    return new InputFieldBinding(member);
                }

            };
    @Override
    protected B parse(LiteralDataInput annotation, M member) 
            throws AlgorithmAnnotationException {
        B annotatedBinding = createBinding(member);
        // auto generate binding if it's not explicitly declared
        Type payloadType = annotatedBinding.getPayloadType();
        Class<? extends ILiteralData> binding = annotation.binding();
        if (binding == null || ILiteralData.class.equals(binding)) {
            if (payloadType instanceof Class<?>) {
                binding = BasicXMLTypeFactory.getBindingForPayloadType((Class<?>) payloadType);
                if (binding == null) {
                    throw new AlgorithmAnnotationException("Unable to locate binding class for %s; binding not found.", payloadType);
                }
            } else  if (annotatedBinding.isMemberTypeList()) {
                throw new AlgorithmAnnotationException("Unable to determine binding class for %s; List must be parameterized with a type matching a known binding payload to use auto-binding.", payloadType);
            } else {
                throw new AlgorithmAnnotationException("Unable to determine binding class for %s; type must fully resolved to use auto-binding", payloadType);
            }
        }
        String[] allowedValues = annotation.allowedValues();
        String defaultValue = annotation.defaultValue();
        int maxOccurs = annotation.maxOccurs();
        // If InputType is enum
        //  1) generate allowedValues if not explicitly declared
        //  2) validate allowedValues if explicitly declared
        //  3) validate defaultValue if declared
        //  4) check for special ENUM_COUNT maxOccurs flag
        Type inputType = annotatedBinding.getType();
        if (annotatedBinding.isTypeEnum()) {
            Class<? extends Enum> inputEnumClass = (Class<? extends Enum>) inputType;
            // validate contents of allowed values maps to enum
            if (allowedValues.length > 0) {
                List<String> invalidValues = new ArrayList<>();
                for (String value : allowedValues) {
                    try {
                        Enum.valueOf(inputEnumClass, value);
                    } catch (IllegalArgumentException e) {
                        invalidValues.add(value);
                        throw new AlgorithmAnnotationException("Invalid allowed value \"%s\" specified for for enumerated input type {}", value, inputType);
                    }
                }
                if (invalidValues.size() > 0) {
                    List<String> updatedValues
                            = new ArrayList<String>(Arrays.asList(allowedValues));
                    updatedValues.removeAll(invalidValues);
                    allowedValues = updatedValues.toArray(new String[0]);
                }
            }
            // if list is empty, populated with values from enum
            if (allowedValues.length == 0) {
                allowedValues = ClassUtil.convertEnumToStringArray(inputEnumClass);
            }
            if (defaultValue.length() > 0) {
                try {
                    Enum.valueOf(inputEnumClass, defaultValue);
                } catch (IllegalArgumentException e) {
                    throw new AlgorithmAnnotationException("Invalid default value \"%s\" specified for for enumerated input type %s", defaultValue, inputType);
                }
            }
            if (maxOccurs == LiteralDataInput.ENUM_COUNT) {
                maxOccurs = inputEnumClass.getEnumConstants().length;
            }
        } else {
            if (maxOccurs == LiteralDataInput.ENUM_COUNT) {
                maxOccurs = 1;
                LOGGER.warn("Invalid maxOccurs \"ENUM_COUNT\" specified for for input type {}, setting maxOccurs to {}", inputType, maxOccurs);
            }
        }
        LiteralDataInputDescriptor descriptor
                = LiteralDataInputDescriptor.builder(annotation.identifier(), binding)
                .title(annotation.title()).abstrakt(annotation.abstrakt())
                .minOccurs(annotation.minOccurs()).maxOccurs(maxOccurs)
                .defaultValue(defaultValue).allowedValues(allowedValues)
                .build();
        annotatedBinding.setDescriptor(descriptor);
        return annotatedBinding;
    }

    @Override
    public Class<? extends LiteralDataInput> getSupportedAnnotation() {
        return LiteralDataInput.class;
    }

}
