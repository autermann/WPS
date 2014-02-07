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
package org.n52.wps.algorithm.descriptor;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.google.common.base.Preconditions;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public abstract class AlgorithmDescriptorBuilder<B extends AlgorithmDescriptorBuilder<B>>
        extends DescriptorBuilder<B> {

    private String version = "1.0.0";
    private boolean storeSupported = true;
    private boolean statusSupported = true;
    private final List<InputDescriptor> inputDescriptors = new LinkedList<>();
    private final List<OutputDescriptor> outputDescriptors = new LinkedList<>();

    protected AlgorithmDescriptorBuilder(String identifier) {
        super(identifier);
        title(identifier);
    }

    public B version(String version) {
        this.version = version;
        return self();
    }

    public B storeSupported(boolean storeSupported) {
        this.storeSupported = storeSupported;
        return self();
    }

    public B statusSupported(boolean statusSupported) {
        this.statusSupported = statusSupported;
        return self();
    }

    public B addInputDescriptor(
            InputDescriptorBuilder<?> inputDescriptorBuilder) {
        return addInputDescriptor(inputDescriptorBuilder.build());
    }

    public B addInputDescriptor(InputDescriptor inputDescriptor) {
        Preconditions.checkNotNull(inputDescriptor);
        this.inputDescriptors.add(inputDescriptor);
        return self();
    }

    public B addInputDescriptors(
            List<? extends InputDescriptor> inputDescriptors) {
        for (InputDescriptor inputDescriptor : inputDescriptors) {
            addInputDescriptor(inputDescriptor);
        }
        return self();
    }

    public B addOutputDescriptor(
            OutputDescriptorBuilder<?> outputDescriptorBuilder) {
        return addOutputDescriptor(outputDescriptorBuilder.build());
    }

    public B addOutputDescriptor(OutputDescriptor outputDescriptor) {
        Preconditions.checkNotNull(outputDescriptor);
        this.outputDescriptors.add(outputDescriptor);
        return self();
    }

    public B addOutputDescriptors(
            List<? extends OutputDescriptor> outputDescriptors) {
        for (OutputDescriptor outputDescriptor : outputDescriptors) {
            addOutputDescriptor(outputDescriptor);
        }
        return self();
    }

    public AlgorithmDescriptor build() {
        return new AlgorithmDescriptor(this);
    }

    protected String getVersion() {
        return version;
    }

    protected boolean isStoreSupported() {
        return storeSupported;
    }

    protected boolean isStatusSupported() {
        return statusSupported;
    }

    protected List<InputDescriptor> getInputDescriptors() {
        return Collections.unmodifiableList(inputDescriptors);
    }

    protected List<OutputDescriptor> getOutputDescriptors() {
        return Collections.unmodifiableList(outputDescriptors);
    }

}
