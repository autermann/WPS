package org.n52.wps.server.database.configuration;

import java.io.File;

import org.n52.wps.commons.configuration.Configuration;
import org.n52.wps.commons.configuration.ConfigurationException;
import org.n52.wps.commons.configuration.ModifiableConfiguration;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;

/**
 *
 * @author Christian Autermann
 */
public interface DatabaseConfiguration extends Configuration {
    String getDatabaseClassName();

    interface Modifiable extends DatabaseConfiguration, ModifiableConfiguration<DatabaseConfiguration> {
        String setDatabaseClassName();
    }
    interface Credentials extends DatabaseConfiguration {
        Optional<String> getUsername();
        Optional<String> getPassword();
        public static interface Modifiable extends Credentials, ModifiableConfiguration<DatabaseConfiguration> {
            void setUsername(String username) throws ConfigurationException;
            void setPassword(String password) throws ConfigurationException;
        }
    }
    public interface Driver extends DatabaseConfiguration {
        String getDriverClass();
        interface Modifiable extends Driver, ModifiableConfiguration<DatabaseConfiguration>{
            void setDriverClass(String driverClass);
        }
    }
    public interface JDBC extends Driver, Credentials {
        String createConnectionURL();
    }
    public interface JNDI extends Driver, Credentials {
        String getJNDIName();
        interface Modifiable extends JNDI {
            void setJNDIName(String name) throws ConfigurationException;
        }
    }
    public interface Named extends DatabaseConfiguration {
        String DEFAULT_DATABASE_NAME = "wps";
        String getDatabaseName();
        interface Modifiable extends Named, ModifiableConfiguration<DatabaseConfiguration> {
            void setDatabaseName(String name) throws ConfigurationException;
        }
    }
    public interface Path extends DatabaseConfiguration {
        String DEFAULT_DATABASE_PATH = Joiner.on(File.separator).join(System
                .getProperty("java.io.tmpdir", "."), "Database", "Results");
        String getPath();
        interface Modifiable extends Path, ModifiableConfiguration<DatabaseConfiguration> {
            void setPath(String path) throws ConfigurationException;
        }
    }

    public interface HostPort extends DatabaseConfiguration {
        String DEFAULT_HOST = "localhost";
        int getPort();
        String getHost();
        interface Modifiable extends HostPort, ModifiableConfiguration<DatabaseConfiguration> {
            void setPort(int port) throws ConfigurationException;
            void setHost(String host) throws ConfigurationException;
        }
    }
    public interface Wiping extends DatabaseConfiguration {
        boolean DEFAULT_WIPING_ENABLED = true;
        long DEFAULT_WIPING_THRESHOLD = 1000 * 60 * 60 * 24 * 7;
        long DEFAULT_WIPING_PERIOD = 1000 * 60 * 60;
        long getWipingThreshold();
        long getWipingPeriod();
        boolean isWipingEnabled();
        interface Modifiable extends Wiping, ModifiableConfiguration<DatabaseConfiguration> {
            void setWipingThreshold(long threshold) throws ConfigurationException;
            void setWipingPeriod(long period) throws ConfigurationException;
            void setWipingEnabled(boolean enabled) throws ConfigurationException;
        }
    }


}
