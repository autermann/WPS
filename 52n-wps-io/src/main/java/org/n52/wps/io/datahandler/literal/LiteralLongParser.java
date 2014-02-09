package org.n52.wps.io.datahandler.literal;

import org.n52.wps.io.data.ILiteralData;
import org.n52.wps.io.data.binding.literal.LiteralLongBinding;

public class LiteralLongParser extends AbstractLiteralNumberDataParser {

    public LiteralLongParser(String uri) {
        super(Long.class, LiteralLongBinding.class, uri);
    }

    @Override
    public ILiteralData parse1(String value) {
        return new LiteralLongBinding(Long.parseLong(value));
    }

}
