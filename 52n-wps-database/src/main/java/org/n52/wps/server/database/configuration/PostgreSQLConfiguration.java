package org.n52.wps.server.database.configuration;

import static com.google.common.base.Strings.emptyToNull;
import static org.n52.wps.commons.configuration.Configurations.checkArgument;
import static org.n52.wps.commons.configuration.Configurations.checkNotNull;

import org.n52.wps.commons.configuration.ConfigurationException;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class PostgreSQLConfiguration
        extends AbstractPostgreSQLConfiguration
        implements DatabaseConfiguration.JDBC,
                   DatabaseConfiguration.HostPort.Modifiable,
                   DatabaseConfiguration.Named.Modifiable {
    public static final int POSTRESQL_DEFAULT_PORT = 5432;

    private String host = DEFAULT_HOST;
    private int port = POSTRESQL_DEFAULT_PORT;
    private String name = DEFAULT_DATABASE_NAME;

    @Override
    public String createConnectionURL() {
        return String.format("jdbc:postgresql://%s:%d/%s",
                             getHost(), getPort(), getDatabaseName());
    }

    @Override
    public void setPort(int port)
            throws ConfigurationException {
        checkArgument(port > 0);
        this.port = port;
        change(this);
    }

    @Override
    public int getPort() {
        return this.port;
    }

    @Override
    public void setHost(String host)
            throws ConfigurationException {
        checkNotNull(emptyToNull(host));
        this.host = host;
        change(this);
    }

    @Override
    public String getHost() {
        return this.host;
    }

    @Override
    public void setDatabaseName(String name)
            throws ConfigurationException {
        checkNotNull(emptyToNull(name));
        this.name = name;
        change(this);
    }

    @Override
    public String getDatabaseName() {
        return this.name;
    }

}
