package org.n52.wps.algorithm.annotation;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class AlgorithmAnnotationException extends Exception {

    private static final long serialVersionUID = 1L;

    public AlgorithmAnnotationException(String message, Object... messageParam) {
        super(String.format(message, messageParam));
    }

    public AlgorithmAnnotationException(Throwable cause, String message,
                                        Object... messageParam) {
        super(String.format(message, messageParam), cause);
    }
}
