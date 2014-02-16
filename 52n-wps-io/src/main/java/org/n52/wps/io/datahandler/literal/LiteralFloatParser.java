package org.n52.wps.io.datahandler.literal;

import org.n52.wps.io.data.ILiteralData;
import org.n52.wps.io.data.binding.literal.LiteralFloatBinding;

/**
 * @author Christian Autermann
 */
public class LiteralFloatParser extends AbstractLiteralNumberDataParser {

    public LiteralFloatParser(String uri) {
        super(Float.class, LiteralFloatBinding.class, uri);
    }

    @Override
    public ILiteralData parse1(String value) {
        return new LiteralFloatBinding(Float.parseFloat(value));
    }

}
