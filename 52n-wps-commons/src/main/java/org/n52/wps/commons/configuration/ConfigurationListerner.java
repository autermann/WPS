package org.n52.wps.commons.configuration;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public interface ConfigurationListerner<T> {

    void onChange(T change)
            throws ConfigurationException;

}
