package org.n52.wps.io.datahandler.literal;

import org.n52.wps.io.data.ILiteralData;
import org.n52.wps.io.data.binding.literal.LiteralIntBinding;

/**
 * @author Christian Autermann
 */
public class LiteralIntegerParser extends AbstractLiteralNumberDataParser {

    public LiteralIntegerParser(String uri) {
        super(Integer.class, LiteralIntBinding.class, uri);
    }

    @Override
    public ILiteralData parse1(String value) {
        return new LiteralIntBinding(Integer.parseInt(value));
    }

}
