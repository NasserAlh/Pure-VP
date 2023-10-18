package volume1.profile;

import java.util.TreeMap;

public class VolumeProfileHandler {
    private TreeMap<Double, Integer> volumeProfile = new TreeMap<>();

    public void addTrade(double price, int size) {
        volumeProfile.merge(price, size, (oldValue, newValue) -> (int) oldValue + newValue);
    }

    public TreeMap<Double, Integer> getVolumeProfile() {
        return volumeProfile;
    }
}

