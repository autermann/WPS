package org.n52.wps.commons.configuration;

import java.util.Set;

import com.google.common.collect.Sets;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class AbstractModifiableConfiguration<T extends Configuration>
        implements ModifiableConfiguration<T> {

    private final Set<ConfigurationListerner<T>> listeners = Sets
            .newConcurrentHashSet();

    @Override
    public void bind(ConfigurationListerner<T> listener) {
        listeners.add(listener);
    }

    @Override
    public void unbind(ConfigurationListerner<T> listener) {
        listeners.remove(listener);
    }

    @Override
    public void change(T change)
            throws ConfigurationException {
        synchronized (listeners) {
            for (ConfigurationListerner<T> listener : listeners) {
                listener.onChange(change);
            }
        }
    }

}
