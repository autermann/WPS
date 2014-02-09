package org.n52.wps.io.datahandler.literal;

import org.apache.xmlbeans.impl.util.Base64;
import org.n52.wps.io.data.ILiteralData;
import org.n52.wps.io.data.binding.literal.LiteralBase64BinaryBinding;
import org.n52.wps.server.InvalidParameterValueException;

/**
 * @author Christian Autermann
 */
public class LiteralBase64Parser extends AbstractLiteralDataParser {

    public LiteralBase64Parser(String uri) {
        super(byte[].class, LiteralBase64BinaryBinding.class, uri);
    }

    @Override
    public ILiteralData parse(String value)
            throws
            InvalidParameterValueException {
        byte[] decoded = Base64.decode(value.getBytes());
        if (decoded == null) {
            throw new InvalidParameterValueException("Could not decode %s data: %s", getURI(), value);
        }
        return new LiteralBase64BinaryBinding(decoded);
    }

}
