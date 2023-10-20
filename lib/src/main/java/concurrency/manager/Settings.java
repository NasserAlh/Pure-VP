package concurrency.manager;

import velox.api.layer1.settings.StrategySettingsVersion;

@StrategySettingsVersion(currentVersion = 1, compatibleVersions = {})
public class Settings {
    public double POC_BUFFER = 2;
    public int START_HOUR = 9;
    public int START_MINUTE = 45;
    public double STANDARD_DEVIATION_THRESHOLD = 0.5;
}