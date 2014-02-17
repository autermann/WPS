package org.n52.wps.server.database.configuration;

import static com.google.common.base.Strings.emptyToNull;

import org.n52.wps.commons.configuration.ConfigurationException;
import org.n52.wps.server.database.PostgresDatabase;

import com.google.common.base.Optional;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public abstract class AbstractPostgreSQLConfiguration
        extends AbstractWipingConfiguration
        implements DatabaseConfiguration.Credentials.Modifiable,
                   DatabaseConfiguration.Wiping.Modifiable,
                   DatabaseConfiguration.Driver {

    private String username;
    private String password;

    @Override
    public void setUsername(String username)
            throws ConfigurationException {
        this.username = emptyToNull(username);
        change(this);
    }

    @Override
    public Optional<String> getUsername() {
        return Optional.fromNullable(this.username);
    }

    @Override
    public void setPassword(String password)
            throws ConfigurationException {
        this.password = emptyToNull(password);
        change(this);
    }

    @Override
    public Optional<String> getPassword() {
        return Optional.fromNullable(this.password);
    }

    @Override
    public String getDatabaseClassName() {
        return PostgresDatabase.class.getName();
    }

    @Override
    public String getDriverClass() {
        return "org.postresql.Driver";
    }

}
