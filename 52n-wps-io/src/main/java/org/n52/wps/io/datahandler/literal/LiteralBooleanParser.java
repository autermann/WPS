package org.n52.wps.io.datahandler.literal;

import org.n52.wps.io.data.ILiteralData;
import org.n52.wps.io.data.binding.literal.LiteralBooleanBinding;
import org.n52.wps.server.ExceptionReport;

/**
 * @author Christian Autermann
 */
public class LiteralBooleanParser extends AbstractLiteralDataParser {

    public LiteralBooleanParser(String uri) {
        super(Boolean.class, LiteralBooleanBinding.class, uri);
    }

    @Override
    public ILiteralData parse(String value) throws ExceptionReport {
        return new LiteralBooleanBinding(Boolean.parseBoolean(value));
    }

}
