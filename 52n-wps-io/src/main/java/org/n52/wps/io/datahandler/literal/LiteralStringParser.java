package org.n52.wps.io.datahandler.literal;

import org.n52.wps.io.data.ILiteralData;
import org.n52.wps.io.data.binding.literal.LiteralStringBinding;

/**
 * @author Christian Autermann
 */
public class LiteralStringParser extends AbstractLiteralDataParser {

    public LiteralStringParser(String uri) {
        super(String.class, LiteralStringBinding.class, uri);
    }

    @Override
    public ILiteralData parse(String value) {
        return new LiteralStringBinding(value);
    }

}
