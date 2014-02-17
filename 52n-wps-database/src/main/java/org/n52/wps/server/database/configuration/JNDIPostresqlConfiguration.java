package org.n52.wps.server.database.configuration;

import static com.google.common.base.Strings.emptyToNull;
import static org.n52.wps.commons.configuration.Configurations.checkNotNull;

import org.n52.wps.commons.configuration.ConfigurationException;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class JNDIPostresqlConfiguration
        extends AbstractPostgreSQLConfiguration
        implements DatabaseConfiguration.JNDI.Modifiable {
    private String jndiName;

    @Override
    public void setJNDIName(String name)
            throws ConfigurationException {
        checkNotNull(emptyToNull(name));
        this.jndiName = name;
        change(this);
    }

    @Override
    public String getJNDIName() {
        return this.jndiName;
    }

}
