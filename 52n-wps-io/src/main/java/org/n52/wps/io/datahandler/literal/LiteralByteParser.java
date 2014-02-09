package org.n52.wps.io.datahandler.literal;

import org.n52.wps.io.data.ILiteralData;
import org.n52.wps.io.data.binding.literal.LiteralByteBinding;

/**
 * @author Christian Autermann
 */
public class LiteralByteParser extends AbstractLiteralNumberDataParser {

    public LiteralByteParser(String uri) {
        super(Byte.class, LiteralByteBinding.class, uri);
    }

    @Override
    public ILiteralData parse1(String value) {
        return new LiteralByteBinding(Byte.parseByte(value));
    }

}
