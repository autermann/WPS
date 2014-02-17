package org.n52.wps.server;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class VersionNegotiationFailedException extends ExceptionReport {
    private static final long serialVersionUID = -5648657980448876875L;

    public VersionNegotiationFailedException(String message) {
        super(message, VERSION_NEGOTIATION_FAILED);
    }

    public VersionNegotiationFailedException(String message,
                                             Object... messageParam) {
        super(String.format(message, messageParam), VERSION_NEGOTIATION_FAILED);
    }

    @Override
    public VersionNegotiationFailedException causedBy(Throwable t) {
        return (VersionNegotiationFailedException) super.causedBy(t);
    }

    @Override
    public VersionNegotiationFailedException locatedAt(String locator) {
        return (VersionNegotiationFailedException) super.locatedAt(locator);
    }
}
