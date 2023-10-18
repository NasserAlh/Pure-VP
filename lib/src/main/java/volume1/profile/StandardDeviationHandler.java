package volume1.profile;

import java.util.LinkedList;

public class StandardDeviationHandler {
    private LinkedList<Double> recentPrices = new LinkedList<>();
    private static final int PRICE_WINDOW_SIZE = 50;

    public void addPrice(double price) {
        recentPrices.add(price);
        if (recentPrices.size() > PRICE_WINDOW_SIZE) {
            recentPrices.removeFirst();
        }
    }

    public double calculateStandardDeviation() {
        double mean = recentPrices.stream().mapToDouble(val -> val).average().orElse(0.0);
        double variance = recentPrices.stream().mapToDouble(val -> Math.pow(val - mean, 2)).sum() / recentPrices.size();
        return Math.sqrt(variance);
    }
}

