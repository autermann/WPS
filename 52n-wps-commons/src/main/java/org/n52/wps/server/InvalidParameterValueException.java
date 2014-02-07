package org.n52.wps.server;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class InvalidParameterValueException extends ExceptionReport {
    private static final long serialVersionUID = 1L;

    public InvalidParameterValueException(String message) {
        super(message, INVALID_PARAMETER_VALUE);
    }

    public InvalidParameterValueException(String message, Object... messageParam) {
        super(String.format(message, messageParam), INVALID_PARAMETER_VALUE);
    }

    public InvalidParameterValueException(String locator, String message,
                                          Object... messageParam) {
        super(String.format(message, messageParam), INVALID_PARAMETER_VALUE, locator);
    }

    public InvalidParameterValueException(Throwable e, String message,
                                          Object... messageParam) {
        super(String.format(message, messageParam), INVALID_PARAMETER_VALUE, e);
    }

    public InvalidParameterValueException(Throwable e, String locator,
                                          String message, Object... messageParam) {
        super(String.format(message, messageParam), INVALID_PARAMETER_VALUE, locator, e);
    }

}
