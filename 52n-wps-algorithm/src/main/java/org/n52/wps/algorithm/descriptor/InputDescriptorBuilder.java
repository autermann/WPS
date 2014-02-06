package org.n52.wps.algorithm.descriptor;

import static com.google.common.base.Preconditions.checkArgument;

import java.math.BigInteger;

import org.n52.wps.io.data.IData;

/**
 * @author Christian Autermann
 * @param <B> the type of this builder
 */
public abstract class InputDescriptorBuilder<B extends InputDescriptorBuilder<B>>
        extends BoundDescriptorBuilder<B> {

    private BigInteger minOccurs = BigInteger.ONE;
    private BigInteger maxOccurs = BigInteger.ONE;

    protected InputDescriptorBuilder(String identifier, Class<? extends IData> binding) {
        super(identifier, binding);
    }

    public B minOccurs(int minOccurs) {
        return minOccurs(BigInteger.valueOf(minOccurs));
    }

    public B minOccurs(BigInteger minOccurs) {
        checkArgument(minOccurs.longValue() >= 0, "minOccurs must be >= 0");
        this.minOccurs = minOccurs;
        return self();
    }

    public B maxOccurs(int maxOccurs) {
        return maxOccurs(BigInteger.valueOf(maxOccurs));
    }

    public B maxOccurs(BigInteger maxOccurs) {
        checkArgument(maxOccurs.longValue() > 0, "maxOccurs must be > 0");
        this.maxOccurs = maxOccurs;
        return self();
    }

    public <E extends Enum<E>> B maxOccurs(Class<E> enumType) {
        return maxOccurs(enumType.getEnumConstants().length);
    }

    public abstract InputDescriptor build();

    protected BigInteger getMinOccurs() {
        return minOccurs;
    }

    protected BigInteger getMaxOccurs() {
        return maxOccurs;
    }

}
