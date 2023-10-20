package concurrency.manager;

import java.util.TreeMap;

/**
 * This class represents a volume profile handler that keeps track of the volume profile for a given instrument.
 */
public class VolumeProfileHandler {
    private TreeMap<Double, Integer> volumeProfile = new TreeMap<>();

    /**
     * Adds a trade to the volume profile.
     * @param price the price of the trade
     * @param size the size of the trade
     */
    public void addTrade(double price, int size) {
        volumeProfile.merge(price, size, (oldValue, newValue) -> (int) oldValue + newValue);
    }

    /**
     * Returns the volume profile.
     * @return the volume profile
     */
    public TreeMap<Double, Integer> getVolumeProfile() {
        return volumeProfile;
    }
}

