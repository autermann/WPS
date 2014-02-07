package org.n52.wps.server;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class NoApplicableCodeException extends ExceptionReport {

    public NoApplicableCodeException(String message) {
        super(message, NO_APPLICABLE_CODE);
    }

    public NoApplicableCodeException(String message, Object... messageParam) {
        super(String.format(message, messageParam), NO_APPLICABLE_CODE);
    }

    public NoApplicableCodeException(String locator, String message,
                                     Object... messageParam) {
        super(String.format(message, messageParam), NO_APPLICABLE_CODE, locator);
    }

    public NoApplicableCodeException(Throwable e, String message,
                                     Object... messageParam) {
        super(String.format(message, messageParam), NO_APPLICABLE_CODE, e);
    }

    public NoApplicableCodeException(Throwable e, String locator, String message,
                                     Object... messageParam) {
        super(String.format(message, messageParam), NO_APPLICABLE_CODE, locator, e);
    }

}
