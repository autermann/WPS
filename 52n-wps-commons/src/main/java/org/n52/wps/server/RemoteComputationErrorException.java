package org.n52.wps.server;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class RemoteComputationErrorException extends ExceptionReport {
    private static final long serialVersionUID = 4107487898287548641L;

    public RemoteComputationErrorException(String message) {
        super(message, REMOTE_COMPUTATION_ERROR);
    }

    public RemoteComputationErrorException(String message,
                                           Object... messageParam) {
        super(String.format(message, messageParam), REMOTE_COMPUTATION_ERROR);
    }

    @Override
    public RemoteComputationErrorException causedBy(Throwable t) {
        return (RemoteComputationErrorException) super.causedBy(t);
    }

    @Override
    public RemoteComputationErrorException locatedAt(String locator) {
        return (RemoteComputationErrorException) super.locatedAt(locator);
    }
}
