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
package org.n52.wps.server;

import static org.n52.wps.algorithm.annotation.AnnotatedAlgorithmIntrospector.getInstrospector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.n52.wps.algorithm.annotation.AnnotatedAlgorithmIntrospector;
import org.n52.wps.algorithm.annotation.binding.InputBinding;
import org.n52.wps.algorithm.annotation.binding.OutputBinding;
import org.n52.wps.algorithm.descriptor.AlgorithmDescriptor;
import org.n52.wps.io.data.IData;


/**
 *
 * @author tkunicki
 */
public abstract class AbstractAnnotatedAlgorithm extends AbstractDescriptorAlgorithm {

    @Override
    protected AlgorithmDescriptor createAlgorithmDescriptor() {
        return getInstrospector(getAlgorithmClass()).getAlgorithmDescriptor();
    }

    @Override
    public Map<String, IData> run(Map<String, List<IData>> inputMap) {
        Object annotatedInstance = getAlgorithmInstance();
        
        AnnotatedAlgorithmIntrospector introspector = getInstrospector(annotatedInstance.getClass());
        
        for (Map.Entry<String, InputBinding<?>> iEntry : introspector.getInputBindingMap().entrySet()) {
            iEntry.getValue().set(annotatedInstance, inputMap.get(iEntry.getKey()));
        }
        
        getInstrospector(annotatedInstance.getClass()).getExecuteMethodBinding().execute(annotatedInstance);
        
        Map<String, IData> oMap = new HashMap<>();
        for (Map.Entry<String, OutputBinding<?>> oEntry : introspector.getOutputBindingMap().entrySet()) {
            oMap.put(oEntry.getKey(), oEntry.getValue().get(annotatedInstance));
        }
        return oMap;
    }
    
    public Object getAlgorithmInstance() {
        return this;
    }
    
    public Class<?> getAlgorithmClass() {
        return getClass();
    }
    
    public static class Proxy extends AbstractAnnotatedAlgorithm {
        
        final private Class<?> proxiedClass;
        final private Object proxiedInstance;
        
        public Proxy(Class<?> proxiedClass) {
            this.proxiedClass = proxiedClass;
            try {
                this.proxiedInstance = proxiedClass.newInstance();
            } catch (InstantiationException | IllegalAccessException ex) {
                throw new RuntimeException("unable to instantiate proxied algorithm instance", ex);
            }
        }

        @Override
        public Class<?> getAlgorithmClass() {
            return proxiedClass;
        }

        @Override
        public Object getAlgorithmInstance() {
            return proxiedInstance;
        }
    }

}
