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
package org.n52.wps.algorithm.descriptor;

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;

/**
 *
 * @author tkunicki
 */
public class AlgorithmDescriptor extends Descriptor {

    private final String version;
    private final boolean storeSupported;
    private final boolean statusSupported;
    private final Map<String, InputDescriptor> inputDescriptorMap;
    private final Map<String, OutputDescriptor> outputDescriptorMap;
    private final ArrayList<String> inputIdentifiers;
    private final ArrayList<String> outputIdentifiers;

	protected AlgorithmDescriptor(AlgorithmDescriptorBuilder<?> builder) {
        super(builder);

        this.version = builder.getVersion();
        this.storeSupported = builder.isStoreSupported();
        this.statusSupported = builder.isStatusSupported();

        checkState(builder.getOutputDescriptors().size() > 0,
                   "Need at minimum 1 output for algorithm.");
        
        inputDescriptorMap = new LinkedHashMap<>(builder.getInputDescriptors().size());
        for (InputDescriptor iDescriptor : builder.getInputDescriptors()) {
            inputDescriptorMap.put(iDescriptor.getIdentifier(), iDescriptor);
        }
        this.inputIdentifiers = new ArrayList<>(inputDescriptorMap.keySet());

        outputDescriptorMap = new LinkedHashMap<>(builder.getOutputDescriptors().size());
        for (OutputDescriptor oDescriptor : builder.getOutputDescriptors()) {
            outputDescriptorMap.put(oDescriptor.getIdentifier(), oDescriptor);
        }
        this.outputIdentifiers = new ArrayList<>(outputDescriptorMap.keySet());
    }

    public String getVersion() {
        return version;
    }

    public boolean getStoreSupported() {
        return storeSupported;
    }

    public boolean getStatusSupported() {
        return statusSupported;
    }

    public List<String> getInputIdentifiers() {
        return Collections.unmodifiableList(inputIdentifiers);
    }

    public InputDescriptor getInputDescriptor(String identifier) {
        return inputDescriptorMap.get(identifier);
    }

    public Collection<InputDescriptor> getInputDescriptors() {
        return Collections.unmodifiableCollection(inputDescriptorMap.values());
    }

    public List<String> getOutputIdentifiers() {
        return Collections.unmodifiableList(outputIdentifiers);
    }

    public OutputDescriptor getOutputDescriptor(String identifier) {
        return outputDescriptorMap.get(identifier);
    }

    public Collection<OutputDescriptor> getOutputDescriptors() {
        return Collections.unmodifiableCollection(outputDescriptorMap.values());
    }

    public static AlgorithmDescriptorBuilder<?> builder(String identifier) {
        return new BuilderTyped(identifier);
    }

    public static AlgorithmDescriptorBuilder<?> builder(Class<?> clazz) {
        Preconditions.checkNotNull(clazz, "clazz may not be null");
        return new BuilderTyped(clazz.getCanonicalName());
    }

    private static class BuilderTyped extends AlgorithmDescriptorBuilder<BuilderTyped> {
        private BuilderTyped(String identifier) {
            super(identifier);
        }
        @Override
        protected BuilderTyped self() {
            return this;
        }
    }
}
