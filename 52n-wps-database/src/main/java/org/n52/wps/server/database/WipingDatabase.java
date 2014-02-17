package org.n52.wps.server.database;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.n52.wps.server.database.configuration.DatabaseConfiguration;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public abstract class WipingDatabase implements IDatabase {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(WipingDatabase.class);
    private Timer timer;

    @Override
    public void init(DatabaseConfiguration configuration) 
            throws DatabaseInitializationException{
        if (configuration instanceof DatabaseConfiguration.Wiping) {
            DatabaseConfiguration.Wiping c = (DatabaseConfiguration.Wiping) configuration;
            if (c.isWipingEnabled()) {
                timer = new Timer(getClass().getSimpleName() + " Wiper", true);
                timer.scheduleAtFixedRate(new WipeTimerTask(c.getWipingThreshold()), 0, c.getWipingPeriod());
                LOGGER.info("Started wiper timer; period {} ms, threshold {} ms",
                            c.getWipingPeriod(), c.getWipingThreshold());
            } else {
                this.timer = null;
            }
        }
    }

    @Override
    public void shutdown() {
        if (this.timer != null) {
            this.timer.cancel();
        }
    }

    private void wipe(long threshold) {
        long currentTime = System.currentTimeMillis();
        LOGGER.info("Checking for records older than {} ms", threshold);
        List<String> oldRequests = findOldRequests(currentTime, threshold);
        if (!oldRequests.isEmpty()) {
            deleteRequests(oldRequests);
            LOGGER.info("Deleted {} old requests", oldRequests.size());
        }
        List<String> oldResponses = findOldResponses(currentTime, threshold);
        if (!oldResponses.isEmpty()) {
            deleteResponses(oldResponses);
            LOGGER.info("Deleted {} old responses", oldResponses.size());
        }
        List<String> oldComplexValues = findOldComplexValues(currentTime, threshold);
        if (!oldComplexValues.isEmpty()) {
            deleteComplexValues(oldComplexValues);
            LOGGER.info("Deleted {} old complex values", oldComplexValues.size());
        }
    }

    protected abstract List<String> findOldRequests(long currentTime, long threshold);

    protected abstract List<String> findOldResponses(long currentTime, long threshold);

    protected abstract List<String> findOldComplexValues(long currentTime, long threshold);

    protected abstract void deleteRequests(List<String> ids);

    protected abstract void deleteResponses(List<String> ids);

    protected abstract void deleteComplexValues(List<String> ids);

    private class WipeTimerTask extends TimerTask {
        private final long thresholdMillis;

        WipeTimerTask(long thresholdMillis) {
            this.thresholdMillis = thresholdMillis;
        }

        @Override
        public void run() {
            wipe(thresholdMillis);
        }
    }

}
