package org.n52.wps.io.data.binding.literal;

import java.math.BigInteger;

/**
 * @author Christian Autermann
 */
public class LiteralBigIntegerBinding extends AbstractLiteralDataBinding
        implements Comparable<LiteralBigIntegerBinding> {
    private final BigInteger payload;

    public LiteralBigIntegerBinding(BigInteger payload) {
        this.payload = payload;
    }

    @Override
    public BigInteger getPayload() {
        return this.payload;
    }

    @Override
    public Class<BigInteger> getSupportedClass() {
        return BigInteger.class;
    }

    @Override
    public int compareTo(LiteralBigIntegerBinding o) {
        return getPayload().compareTo(o.getPayload());
    }

}
