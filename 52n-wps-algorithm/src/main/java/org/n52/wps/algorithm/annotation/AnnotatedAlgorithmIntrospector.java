/**
 * ﻿Copyright (C) 2007
 * by 52 North Initiative for Geospatial Open Source Software GmbH
 *
 * Contact: Andreas Wytzisk
 * 52 North Initiative for Geospatial Open Source Software GmbH
 * Martin-Luther-King-Weg 24
 * 48155 Muenster, Germany
 * info@52north.org
 *
 * This program is free software; you can redistribute and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the
 * Free Software Foundation.
 *
 * This program is distributed WITHOUT ANY WARRANTY; even without the implied
 * WARRANTY OF MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program (see gnu-gpl v2.txt). If not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA or
 * visit the Free Software Foundation web page, http://www.fsf.org.
 */
package org.n52.wps.algorithm.annotation;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.n52.wps.algorithm.annotation.binding.ExecuteMethodBinding;
import org.n52.wps.algorithm.annotation.binding.InputBinding;
import org.n52.wps.algorithm.annotation.binding.OutputBinding;
import org.n52.wps.algorithm.annotation.parser.ComplexDataInputAnnotationParser;
import org.n52.wps.algorithm.annotation.parser.ComplexDataOutputAnnotationParser;
import org.n52.wps.algorithm.annotation.parser.ExecuteAnnotationParser;
import org.n52.wps.algorithm.annotation.parser.InputAnnotationParser;
import org.n52.wps.algorithm.annotation.parser.LiteralDataInputAnnotationParser;
import org.n52.wps.algorithm.annotation.parser.LiteralDataOutputAnnotationParser;
import org.n52.wps.algorithm.annotation.parser.OutputAnnotationParser;
import org.n52.wps.algorithm.descriptor.AlgorithmDescriptor;
import org.n52.wps.algorithm.descriptor.AlgorithmDescriptorBuilder;

import com.google.common.collect.ImmutableList;

/**
 *
 * @author tkunicki
 */
public class AnnotatedAlgorithmIntrospector {

    private final static List<InputAnnotationParser<?, Field, InputBinding<Field>>> INPUT_FIELD_PARSERS
            = ImmutableList.of(LiteralDataInputAnnotationParser.FIELD,
                               ComplexDataInputAnnotationParser.FIELD);
    private final static List<InputAnnotationParser<?, Method, InputBinding<Method>>> INPUT_METHOD_PARSERS
            = ImmutableList.of(LiteralDataInputAnnotationParser.METHOD,
                               ComplexDataInputAnnotationParser.METHOD);
    private final static List<OutputAnnotationParser<?, Field, OutputBinding<Field>>> OUTPUT_FIELD_PARSERS
            = ImmutableList.of(LiteralDataOutputAnnotationParser.FIELD,
                               ComplexDataOutputAnnotationParser.FIELD);
    private final static List<OutputAnnotationParser<?, Method, OutputBinding<Method>>> OUTPUT_METHOD_PARSERS
            = ImmutableList.of(LiteralDataOutputAnnotationParser.METHOD,
                               ComplexDataOutputAnnotationParser.METHOD);
    private final static ExecuteAnnotationParser PROCESS_PARSER = new ExecuteAnnotationParser();

    private final static Map<Class<?>, AnnotatedAlgorithmIntrospector> INTROSPECTOR_MAP = new HashMap<>();
    
    public static synchronized AnnotatedAlgorithmIntrospector getInstrospector(Class<?> algorithmClass) {
        AnnotatedAlgorithmIntrospector introspector = INTROSPECTOR_MAP.get(algorithmClass);
        if (introspector == null) {
            introspector = new AnnotatedAlgorithmIntrospector(algorithmClass);
            INTROSPECTOR_MAP.put(algorithmClass, introspector);
            try {
                introspector.parseClass();
            } catch (AlgorithmAnnotationException ex) {
                throw new RuntimeException(ex);
            }
        }
        return introspector;
    }
    
    private final Class<?> algorithmClass;
    private AlgorithmDescriptor algorithmDescriptor;
    private ExecuteMethodBinding executeMethodBinding;
    private Map<String, InputBinding<?>> inputBindingMap;
    private Map<String, OutputBinding<?>> outputBindingMap;
    

    public AnnotatedAlgorithmIntrospector(Class<?> algorithmClass) {
        this.algorithmClass = algorithmClass;
        inputBindingMap = new LinkedHashMap<>();
        outputBindingMap = new LinkedHashMap<>();
    }

    private void parseClass() throws AlgorithmAnnotationException {
        
        if (!algorithmClass.isAnnotationPresent(Algorithm.class)) {
            throw new RuntimeException("Class isn't annotated with an Algorithm annotation");
        }
        
        boolean validContructor = false;
        try {
            Constructor<?> defaultConstructor = algorithmClass.getConstructor();
            validContructor = (defaultConstructor.getModifiers() & Modifier.PUBLIC) == Modifier.PUBLIC;
        } catch (NoSuchMethodException ex) {
            // inherit error message on fall through...
        } catch (SecurityException ex) {
            throw new RuntimeException("Current security policy limits use of reflection, error introspecting " + algorithmClass.getName());
        }
        if (!validContructor) {
             throw new RuntimeException("Classes with Algorithm annotation require public no-arg constructor, error introspecting " + algorithmClass.getName());
        }
        
        
        AlgorithmDescriptorBuilder<?> algorithmBuilder;
        Algorithm algorithm = algorithmClass.getAnnotation(Algorithm.class);
        algorithmBuilder = AlgorithmDescriptor.builder(
                algorithm.identifier().length() > 0 ?
                    algorithm.identifier() :
                    algorithmClass.getCanonicalName());
        
        algorithmBuilder.
                title(algorithm.title()).
                abstrakt(algorithm.abstrakt()).
                version(algorithm.version()).
                storeSupported(algorithm.storeSupported()).
                statusSupported(algorithm.statusSupported());
        
        parseElements(algorithmClass.getDeclaredMethods(), INPUT_METHOD_PARSERS, OUTPUT_METHOD_PARSERS);
        parseElements(algorithmClass.getDeclaredFields(), INPUT_FIELD_PARSERS, OUTPUT_FIELD_PARSERS);

        
        for (Method method : algorithmClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(PROCESS_PARSER.getSupportedAnnotation())) {
                ExecuteMethodBinding binding = PROCESS_PARSER.parse(method);
                if (binding != null) {
                    if (this.executeMethodBinding != null) {
                        // we need to error out here because ordering of getDeclaredMethods() or
                        // getMethods() is not guarenteed to be consistent, if it were consistent
                        // maybe we could ignore this state,  but having an algorithm behave
                        // differently betweeen runtimes would be bad...
                        throw new RuntimeException("Multiple execute method bindings encountered for class " + getClass().getCanonicalName());
                    }
                    this.executeMethodBinding = binding;
                }
            }
        }
        
        for (InputBinding<?> inputBinding : inputBindingMap.values()) {
            algorithmBuilder.addInputDescriptor(inputBinding.getDescriptor());
        }
        for (OutputBinding<?> outputBinding : outputBindingMap.values()) {
            algorithmBuilder.addOutputDescriptor(outputBinding.getDescriptor());
        }
        algorithmDescriptor = algorithmBuilder.build();
    }

    public AlgorithmDescriptor getAlgorithmDescriptor() {
        return algorithmDescriptor;
    }

    public ExecuteMethodBinding getExecuteMethodBinding() {
        return executeMethodBinding;
    }

    public Map<String, InputBinding<?>> getInputBindingMap() {
        return Collections.unmodifiableMap(inputBindingMap);
    }

    public Map<String, OutputBinding<?>> getOutputBindingMap() {
        return Collections.unmodifiableMap(outputBindingMap);
    }

    public <M extends AccessibleObject & Member> void parseElements(M members[],
            List<InputAnnotationParser<?, M, InputBinding<M>>> inputParser,
            List<OutputAnnotationParser<?, M, OutputBinding<M>>> outputParser) 
            throws AlgorithmAnnotationException {
        for (M member : members) {
            for (OutputAnnotationParser<?, M, OutputBinding<M>> parser: outputParser) {
                if (member.isAnnotationPresent(parser.getSupportedAnnotation())) {
                    OutputBinding<M> binding = parser.parse(member);
                    if (binding != null) {
                        outputBindingMap.put(binding.getDescriptor().getIdentifier(), binding);
                    }
                }
            }
            for (InputAnnotationParser<?, M, InputBinding<M>> parser: inputParser) {
                if (member.isAnnotationPresent(parser.getSupportedAnnotation())) {
                    InputBinding<M> binding = parser.parse(member);
                    if (binding != null) {
                        inputBindingMap.put(binding.getDescriptor().getIdentifier(), binding);
                    }
                }
            } 
        }
    }
         
}
