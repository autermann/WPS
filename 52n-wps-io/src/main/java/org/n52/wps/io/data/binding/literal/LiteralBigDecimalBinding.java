package org.n52.wps.io.data.binding.literal;

import java.math.BigDecimal;


/**
 * @author Christian Autermann
 */
public class LiteralBigDecimalBinding extends AbstractLiteralDataBinding
        implements Comparable<LiteralBigDecimalBinding> {
    private final BigDecimal value;

    public LiteralBigDecimalBinding(BigDecimal value) {
        this.value = value;
    }

    @Override
    public BigDecimal getPayload() {
        return this.value;
    }

    @Override
    public Class<BigDecimal> getSupportedClass() {
        return BigDecimal.class;
    }

    @Override
    public int compareTo(LiteralBigDecimalBinding o) {
        return getPayload().compareTo(o.getPayload());
    }
}
