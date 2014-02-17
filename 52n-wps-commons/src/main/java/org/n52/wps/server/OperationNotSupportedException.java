package org.n52.wps.server;

/**
 * @author Christian Autermann
 */
public class OperationNotSupportedException extends ExceptionReport {
    private static final long serialVersionUID = -3105416816693305219L;

    public OperationNotSupportedException(String message) {
        super(message, OPERATION_NOT_SUPPORTED);
    }

    public OperationNotSupportedException(String message, Object... messageParam) {
        super(String.format(message, messageParam), OPERATION_NOT_SUPPORTED);
    }

    @Override
    public OperationNotSupportedException causedBy(Throwable t) {
        return (OperationNotSupportedException) super.causedBy(t);
    }

    @Override
    public OperationNotSupportedException locatedAt(String locator) {
        return (OperationNotSupportedException) super.locatedAt(locator);
    }
}
