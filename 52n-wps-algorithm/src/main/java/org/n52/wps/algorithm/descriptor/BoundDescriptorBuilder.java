package org.n52.wps.algorithm.descriptor;

import org.n52.wps.io.data.IData;

import com.google.common.base.Preconditions;

/**
 * @author Christian Autermann
 * @param <B> the type of this builder
 */
public abstract class BoundDescriptorBuilder<B extends BoundDescriptorBuilder<B>>
        extends DescriptorBuilder<B> {

    private final Class<? extends IData> binding;

    protected BoundDescriptorBuilder(String identifier, Class<? extends IData> binding) {
        super(identifier);
        Preconditions.checkArgument(binding != null, "binding may not be null");
        this.binding = binding;
    }

    protected Class<? extends IData> getBinding() {
        return binding;
    }

}
