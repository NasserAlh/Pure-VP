package volume1.profile;

public class PointOfControlHandler {
    private double pointOfControlPrice = Double.NaN;
    private int pointOfControlVolume = 0;

    public void updatePOC(double price, int volume) {
        if (volume > pointOfControlVolume) {
            pointOfControlPrice = price;
            pointOfControlVolume = volume;
        }
    }

    public double getPointOfControlPrice() {
        return pointOfControlPrice;
    }
}
