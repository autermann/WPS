package org.n52.wps.server;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class ServerBusyException extends ExceptionReport {
    private static final long serialVersionUID = -5171184694946945666L;

    public ServerBusyException(String message) {
        super(message, SERVER_BUSY);
    }

    public ServerBusyException(String message, Object... messageParam) {
        super(String.format(message, messageParam), SERVER_BUSY);
    }

    @Override
    public ServerBusyException causedBy(Throwable t) {
        return (ServerBusyException) super.causedBy(t);
    }

    @Override
    public ServerBusyException locatedAt(String locator) {
        return (ServerBusyException) super.locatedAt(locator);
    }
}
