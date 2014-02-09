package org.n52.wps.io.datahandler.literal;

import java.net.URI;
import java.net.URISyntaxException;

import org.n52.wps.io.data.ILiteralData;
import org.n52.wps.io.data.binding.literal.LiteralAnyURIBinding;
import org.n52.wps.server.InvalidParameterValueException;

/**
 * @author Christian Autermann
 */
public class LiteralURIParser extends AbstractLiteralDataParser {

    public LiteralURIParser(String uri) {
        super(URI.class, LiteralAnyURIBinding.class, uri);
    }

    @Override
    public ILiteralData parse(String value)
            throws InvalidParameterValueException {
        try {
            return new LiteralAnyURIBinding(new URI(value));
        } catch (URISyntaxException e) {
            throw new InvalidParameterValueException("Could not parse %s data: %s", getURI(), value)
                    .causedBy(e);
        }
    }

}
