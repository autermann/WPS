package org.n52.wps.server.database.configuration;

import static com.google.common.base.Strings.emptyToNull;
import static org.n52.wps.commons.configuration.Configurations.checkArgument;

import org.n52.wps.commons.configuration.ConfigurationException;
import org.n52.wps.server.database.FlatFileDatabase;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class FlatFileDatabaseConfiguration
        extends AbstractWipingConfiguration
        implements DatabaseConfiguration,
                   DatabaseConfiguration.Wiping.Modifiable,
                   DatabaseConfiguration.Path.Modifiable {
    public static final boolean DEFAULT_GZIP_COMPLEX_VALUES = true;
    private String path = DEFAULT_DATABASE_PATH;
    private boolean gzip = DEFAULT_GZIP_COMPLEX_VALUES;

    @Override
    public String getDatabaseClassName() {
        return FlatFileDatabase.class.getName();
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

    public boolean isGZIP() {
        return this.gzip;
    }

    public void setGZIP(boolean gzip)
            throws ConfigurationException {
        this.gzip = gzip;
        change(this);
    }

 
}
