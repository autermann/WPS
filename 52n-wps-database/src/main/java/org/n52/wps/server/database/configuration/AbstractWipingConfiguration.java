package org.n52.wps.server.database.configuration;

import static org.n52.wps.commons.configuration.Configurations.checkArgument;

import org.n52.wps.commons.configuration.AbstractModifiableConfiguration;
import org.n52.wps.commons.configuration.ConfigurationException;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public abstract class AbstractWipingConfiguration
        extends AbstractModifiableConfiguration<DatabaseConfiguration>
        implements DatabaseConfiguration,
                   DatabaseConfiguration.Wiping.Modifiable {
    private boolean wipingEnabled = DEFAULT_WIPING_ENABLED;
    private long wipingThreshold = DEFAULT_WIPING_THRESHOLD;
    private long wipingPeriod = DEFAULT_WIPING_PERIOD;

    @Override
    public boolean isWipingEnabled() {
        return wipingEnabled;
    }

    @Override
    public void setWipingEnabled(boolean wipingEnabled)
            throws ConfigurationException {
        this.wipingEnabled = wipingEnabled;
        change(this);
    }

    @Override
    public long getWipingThreshold() {
        return wipingThreshold;
    }

    @Override
    public void setWipingThreshold(long wipingThreshold)
            throws ConfigurationException {
        checkArgument(wipingThreshold > 0);
        this.wipingThreshold = wipingThreshold;
        change(this);
    }

    @Override
    public long getWipingPeriod() {
        return wipingPeriod;
    }

    @Override
    public void setWipingPeriod(long wipingPeriod)
            throws ConfigurationException {
        checkArgument(wipingPeriod > 0);
        this.wipingPeriod = wipingPeriod;
        change(this);
    }

}
