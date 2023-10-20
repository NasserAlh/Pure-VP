package concurrency.manager;

/**
 * The PointOfControlHandler class represents a handler for the point of control price and volume.
 */
public class PointOfControlHandler {
    private double pointOfControlPrice = Double.NaN;
    private int pointOfControlVolume = 0;

    /**
     * Updates the point of control price and volume if the given volume is greater than the current volume.
     * @param price The new price of the point of control.
     * @param volume The new volume of the point of control.
     */
    public void updatePOC(double price, int volume) {
        if (volume > pointOfControlVolume) {
            pointOfControlPrice = price;
            pointOfControlVolume = volume;
        }
    }

    /**
     * Returns the current point of control price.
     * @return The current point of control price.
     */
    public double getPointOfControlPrice() {
        return pointOfControlPrice;
    }
}
