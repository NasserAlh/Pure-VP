package concurrency.manager;

import java.util.LinkedList;

/**
 * A class that handles the calculation of standard deviation for a given set of prices.
 */
public class StandardDeviationHandler {
    private LinkedList<Double> recentPrices = new LinkedList<>();
    private static final int PRICE_WINDOW_SIZE = 50;

    /**
     * Adds a new price to the list of recent prices.
     * If the list exceeds the maximum size, the oldest price is removed.
     * @param price the new price to add
     */
    public void addPrice(double price) {
        recentPrices.add(price);
        if (recentPrices.size() > PRICE_WINDOW_SIZE) {
            recentPrices.removeFirst();
        }
    }

    /**
     * Calculates the standard deviation of the recent prices.
     * @return the standard deviation of the recent prices
     */
    public double calculateStandardDeviation() {
        double mean = recentPrices.stream().mapToDouble(val -> val).average().orElse(0.0);
        double variance = recentPrices.stream().mapToDouble(val -> Math.pow(val - mean, 2)).sum() / recentPrices.size();
        return Math.sqrt(variance);
    }
}

