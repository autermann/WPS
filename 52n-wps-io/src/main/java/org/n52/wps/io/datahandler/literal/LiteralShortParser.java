package org.n52.wps.io.datahandler.literal;

import org.n52.wps.io.data.ILiteralData;
import org.n52.wps.io.data.binding.literal.LiteralShortBinding;

/**
 * @author Christian Autermann
 */
public class LiteralShortParser extends AbstractLiteralNumberDataParser {

    public LiteralShortParser(String uri) {
        super(Short.class, LiteralShortBinding.class, uri);
    }

    @Override
    public ILiteralData parse1(String value) {
        return new LiteralShortBinding(Short.parseShort(value));
    }

}
