package org.n52.wps.algorithm.descriptor;

import java.math.BigInteger;

import org.n52.wps.io.data.IComplexData;

import com.google.common.base.Preconditions;

/**
 * @author Christian Autermann
 * @param <B> the type of this builder
 */
public abstract class ComplexDataInputDescriptorBuilder<B extends ComplexDataInputDescriptorBuilder<B>>
        extends InputDescriptorBuilder<B> {

    private BigInteger maximumMegaBytes;

    protected ComplexDataInputDescriptorBuilder(String identifier, Class<? extends IComplexData> binding) {
        super(identifier, binding);
    }

    public B maximumMegaBytes(int maximumMegaBytes) {
        return maximumMegaBytes(BigInteger.valueOf(maximumMegaBytes));
    }

    public B maximumMegaBytes(BigInteger maximumMegaBytes) {
        Preconditions.checkArgument(maximumMegaBytes.longValue() >= 0,
                                    "maximumMegabytes must be >= 0");
        this.maximumMegaBytes = maximumMegaBytes;
        return self();
    }

    @Override
    public ComplexDataInputDescriptor build() {
        return new ComplexDataInputDescriptor(this);
    }

    protected BigInteger getMaximumMegaBytes() {
        return maximumMegaBytes;
    }

}
