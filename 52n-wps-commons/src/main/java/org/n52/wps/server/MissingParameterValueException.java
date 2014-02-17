package org.n52.wps.server;

/**
 * @author Christian Autermann
 */
public class MissingParameterValueException extends ExceptionReport {
    private static final long serialVersionUID = -1973085242173931784L;

    public MissingParameterValueException(String message) {
        super(message, MISSING_PARAMETER_VALUE);
    }

    public MissingParameterValueException(String message, Object... messageParam) {
        super(String.format(message, messageParam), MISSING_PARAMETER_VALUE);
    }

    @Override
    public MissingParameterValueException causedBy(Throwable t) {
        return (MissingParameterValueException) super.causedBy(t);
    }

    @Override
    public MissingParameterValueException locatedAt(String locator) {
        return (MissingParameterValueException) super.locatedAt(locator);
    }
}
