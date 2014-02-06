package org.n52.wps.algorithm.descriptor;

import org.n52.wps.io.data.IComplexData;

/**
 * @author Christian Autermann
 * @param <B> the type of this builder
 */
public abstract class ComplexDataOutputDescriptorBuilder<B extends ComplexDataOutputDescriptorBuilder<B>>
        extends OutputDescriptorBuilder<B> {

    protected ComplexDataOutputDescriptorBuilder(String identifier, Class<? extends IComplexData> binding) {
        super(identifier, binding);
    }

    @Override
    public ComplexDataOutputDescriptor build() {
        return new ComplexDataOutputDescriptor(this);
    }

}
