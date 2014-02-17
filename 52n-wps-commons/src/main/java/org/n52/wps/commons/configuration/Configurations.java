package org.n52.wps.commons.configuration;

import org.n52.wps.commons.configuration.AbstractModifiableConfiguration;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class Configurations {

    private Configurations() {
    }

    public static <T extends Configuration> ModifiableConfiguration<T> listeners() {
        return new AbstractModifiableConfiguration<>();
    }

    public static void checkArgument(boolean expression)
            throws ConfigurationException {
        if (!expression) {
            throw new ConfigurationException();
        }
    }

    public static void checkArgument(boolean expression,
                                     String errorMessageTemplate,
                                     Object... errorMessageArgs)
            throws ConfigurationException {
        if (!expression) {
            throw new ConfigurationException(
                    String.format(errorMessageTemplate, errorMessageArgs));
        }
    }

    public static <T> T checkNotNull(T reference)
            throws ConfigurationException {
        if (reference == null) {
            throw new ConfigurationException();
        }
        return reference;
    }

    public static <T> T checkNotNull(T reference,
                                     String errorMessageTemplate,
                                     Object... errorMessageArgs)
            throws ConfigurationException {
        if (reference == null) {
            throw new ConfigurationException(
                    String.format(errorMessageTemplate, errorMessageArgs));
        }
        return reference;
    }
}
