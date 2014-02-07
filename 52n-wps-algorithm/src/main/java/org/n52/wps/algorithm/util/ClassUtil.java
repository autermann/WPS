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
package org.n52.wps.algorithm.util;

import java.util.ArrayList;
import java.util.List;

import com.google.common.primitives.Primitives;

/**
 *
 * @author tkunicki
 */
public class ClassUtil {

    public static <T extends Enum<T>> List<T> convertStringToEnumList(Class<T> enumType, List<String> stringList) {
        List<T> enumList = new ArrayList<>();
        for (String string : stringList) {
            enumList.add(Enum.valueOf(enumType, string));
        }
        return enumList;
    }

    public static <T extends Enum<T>> List<String> convertEnumToStringList(Class<T> enumType) {
        ArrayList<String> stringList = null;
        T[] constants = enumType.getEnumConstants();
        if (constants != null && constants.length > 0) {
            stringList = new ArrayList<>(constants.length);
            for (T constant : constants) {
                stringList.add(constant.name());
            }
        }
        return stringList;
    }

    public static <T extends Enum<T>> String[] convertEnumToStringArray(Class<T> enumType) {
        List<String> stringList = convertEnumToStringList(enumType);
        return stringList == null ? null : stringList.toArray(new String[stringList.size()]);
    }

    /**
     * @deprecated use {@link Primitives#isWrapperType(java.lang.Class)}
     */
    @Deprecated
    public static boolean isWrapper(Class<?> clazz) {
        return Primitives.isWrapperType(clazz);
    }

    /**
     * @deprecated use {@link Primitives#unwrap(java.lang.Class)}
     */
    @Deprecated
    public static Class<?> unwrap(Class<?> clazz) {
        return Primitives.unwrap(clazz);
    }

    /**
     * @deprecated use {@link Primitives#wrap(java.lang.Class)}
     */
    @Deprecated
    public static Class<?> wrap(Class<?> clazz) {
        return Primitives.wrap(clazz);
    }

}
