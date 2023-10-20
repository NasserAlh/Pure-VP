package concurrency.manager;

import velox.api.layer1.settings.StrategySettingsVersion;

/**
 * This class represents the settings for the concurrency manager strategy.
 */
@StrategySettingsVersion(currentVersion = 1, compatibleVersions = {})
public class Settings {
    /**
     * The buffer for the percentage of completion (POC) before a task is considered complete.
     */
    public double POC_BUFFER = 2;

    /**
     * The hour of the day when the strategy should start executing.
     */
    public int START_HOUR = 9;

    /**
     * The minute of the hour when the strategy should start executing.
     */
    public int START_MINUTE = 45;

    /**
     * The threshold for the standard deviation of the POC values before a task is considered complete.
     */
    public double STANDARD_DEVIATION_THRESHOLD = 0.5;
}