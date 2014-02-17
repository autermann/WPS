package org.n52.wps.commons.configuration;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public interface ModifiableConfiguration<T extends Configuration>
        extends Configuration {

    void bind(ConfigurationListerner<T> listener);

    void unbind(ConfigurationListerner<T> listener);

    void change(T configuration)
            throws ConfigurationException;

}
