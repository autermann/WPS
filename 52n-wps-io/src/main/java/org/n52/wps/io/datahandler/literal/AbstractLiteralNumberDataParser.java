package org.n52.wps.io.datahandler.literal;

import org.n52.wps.io.data.ILiteralData;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.InvalidParameterValueException;

/**
 * @author Christian Autermann
 */
public abstract class AbstractLiteralNumberDataParser extends AbstractLiteralDataParser {

    public AbstractLiteralNumberDataParser(
            Class<? extends Number> payloadType,
            Class<? extends ILiteralData> bindingType,
            String uri) {
        super(payloadType, bindingType, uri);
    }

    @Override
    public ILiteralData parse(String value) throws ExceptionReport {
        try {
            return parse1(value);
        } catch (NumberFormatException e) {
            throw new InvalidParameterValueException("Could not parse literal %s data", getURI())
                    .causedBy(e);
        }
    }

    protected abstract ILiteralData parse1(String value) throws
            NumberFormatException;

}
