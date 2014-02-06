package org.n52.wps.algorithm.descriptor;

import org.n52.wps.io.data.IData;

/**
 * @author Christian Autermann
 * @param <B> the type of this builder
 */
public abstract class OutputDescriptorBuilder<B extends OutputDescriptorBuilder<B>>
        extends BoundDescriptorBuilder<B> {

    protected OutputDescriptorBuilder(String identifier, Class<? extends IData> binding) {
        super(identifier, binding);
    }

    public abstract OutputDescriptor build();

}
