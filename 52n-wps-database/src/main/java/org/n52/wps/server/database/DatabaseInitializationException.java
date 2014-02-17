package org.n52.wps.server.database;

import org.n52.wps.commons.configuration.ConfigurationException;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class DatabaseInitializationException extends ConfigurationException {

    private static final long serialVersionUID = 1L;

    public DatabaseInitializationException() {
    }

    public DatabaseInitializationException(String message) {
        super(message);
    }

    public DatabaseInitializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public DatabaseInitializationException(Throwable cause) {
        super(cause);
    }

}
