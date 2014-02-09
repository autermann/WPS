package org.n52.wps.io.datahandler.literal;

import org.n52.wps.io.data.ILiteralData;
import org.n52.wps.io.data.binding.literal.LiteralDoubleBinding;

/**
 * @author Christian Autermann
 */
public class LiteralDoubleParser extends AbstractLiteralNumberDataParser {

    public LiteralDoubleParser(String uri) {
        super(Double.class, LiteralDoubleBinding.class, uri);
    }

    @Override
    public ILiteralData parse1(String value) {
        return new LiteralDoubleBinding(Double.parseDouble(value));
    }

}
