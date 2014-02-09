package org.n52.wps.io.datahandler.literal;

import java.math.BigDecimal;

import org.n52.wps.io.data.ILiteralData;
import org.n52.wps.io.data.binding.literal.LiteralBigDecimalBinding;

/**
 * @author Christian Autermann
 */
public class LiteralBigDecimalParser extends AbstractLiteralNumberDataParser {

    public LiteralBigDecimalParser(String uri) {
        super(BigDecimal.class, LiteralBigDecimalBinding.class, uri);
    }

    @Override
    protected ILiteralData parse1(String value) throws NumberFormatException {
        return new LiteralBigDecimalBinding(new BigDecimal(value));
    }

}
