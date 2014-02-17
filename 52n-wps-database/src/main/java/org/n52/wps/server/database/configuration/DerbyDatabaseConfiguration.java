package org.n52.wps.server.database.configuration;

import static com.google.common.base.Strings.emptyToNull;
import static org.n52.wps.commons.configuration.Configurations.checkArgument;

import java.io.File;

import org.n52.wps.commons.configuration.ConfigurationException;
import org.n52.wps.server.database.DerbyDatabase;

import com.google.common.base.Optional;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class DerbyDatabaseConfiguration
        extends AbstractWipingConfiguration
        implements DatabaseConfiguration,
                   DatabaseConfiguration.JDBC,
                   DatabaseConfiguration.Wiping.Modifiable,
                   DatabaseConfiguration.Path.Modifiable,
                   DatabaseConfiguration.Named.Modifiable,
                   DatabaseConfiguration.Credentials.Modifiable {

    private String username;
    private String password;
    private String path = DEFAULT_DATABASE_PATH;
    private String databaseName = DEFAULT_DATABASE_NAME;

    @Override
    public String getDriverClass() {
        return "org.apache.derby.jdbc.EmbeddedDriver";
    }

    @Override
    public String getDatabaseClassName() {
        return DerbyDatabase.class.getName();
    }

    @Override
    public String createConnectionURL() {
        return String.format("jdbc:derby:directory:%s%s%s;create=true",
                             getPath(), File.separator, getDatabaseName());
    }

    @Override
    public Optional<String> getUsername() {
        return Optional.fromNullable(this.username);
    }

    @Override
    public void setUsername(String username)
            throws ConfigurationException {
        this.username = emptyToNull(username);
        change(this);
    }

    @Override
    public Optional<String> getPassword() {
        return Optional.fromNullable(this.password);
    }

    @Override
    public void setPassword(String password)
            throws ConfigurationException {
        this.password = emptyToNull(password);
        change(this);
    }

    @Override
    public String getPath() {
        return this.path;
    }

    @Override
    public void setPath(String path)
            throws ConfigurationException {
        checkArgument(emptyToNull(path) != null);
        this.path = path;
        change(this);
    }

    @Override
    public String getDatabaseName() {
        return this.databaseName;
    }

    @Override
    public void setDatabaseName(String databaseName)
            throws ConfigurationException {
        checkArgument(emptyToNull(databaseName) != null);
        this.databaseName = databaseName;
        change(this);
    }
}
