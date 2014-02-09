package org.n52.wps.server;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class FileSizeExceededException extends ExceptionReport {

    public FileSizeExceededException(String message) {
        super(message, FILE_SIZE_EXCEEDED);
    }

    public FileSizeExceededException(String message, Object... messageParam) {
        super(String.format(message, messageParam), FILE_SIZE_EXCEEDED);
    }

    @Override
    public FileSizeExceededException causedBy(Throwable t) {
        return (FileSizeExceededException) super.causedBy(t);
    }

    @Override
    public FileSizeExceededException locatedAt(String locator) {
        return (FileSizeExceededException) super.locatedAt(locator);
    }
}
