package org.n52.wps.io.datahandler.literal;

import java.math.BigInteger;

import org.n52.wps.io.data.ILiteralData;
import org.n52.wps.io.data.binding.literal.LiteralBigIntegerBinding;

/**
 * @author Christian Autermann
 */
public class LiteralBigIntegerParser extends AbstractLiteralNumberDataParser {

    public LiteralBigIntegerParser(String uri) {
        super(BigInteger.class, LiteralBigIntegerBinding.class, uri);
    }

    @Override
    public ILiteralData parse1(String value) {
        return new LiteralBigIntegerBinding(new BigInteger(value));
    }

}
