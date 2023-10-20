package pure.vp;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.stream.IntStream;
import javax.swing.*;
import velox.api.layer1.annotations.*;
import velox.api.layer1.common.Log;
import velox.api.layer1.data.*;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.api.layer1.settings.StrategySettingsVersion;
import velox.api.layer1.simplified.*;
import velox.gui.StrategyPanel;

/**
 * This class implements a trading strategy that uses the Point of Control (POC) indicator and standard deviation to identify high volatility trading opportunities.
 * The strategy listens to trade data and time events, and provides a custom settings panel for the user to adjust the POC buffer, start time, and start minute.
 * The strategy also calculates the volume profile and standard deviation of recent prices to determine if the market is experiencing high volatility.
 * If the market is experiencing high volatility and the POC is stable, the strategy will enter a trade when the price enters a certain range around the POC and the volume rate of change is above a certain threshold.
 */
@Layer1SimpleAttachable
@Layer1StrategyName("POC with Standard Deviation  ")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION1)
public class PocTrading implements CustomModule, TradeDataListener, TimeListener, CustomSettingsPanelProvider {

    /**
     * This class represents the settings for the Point of Control (POC) trading strategy.
     * It contains the POC buffer, start hour, and start minute.
     */
    @StrategySettingsVersion(currentVersion = 1, compatibleVersions = {})
    public static class Settings {
    public double POC_BUFFER = 2;
    public int START_HOUR = 9;
    public int START_MINUTE = 45;
    }

    private Indicator priceIndicator, pocIndicator, pocTradeIndicator;
    private TreeMap<Double, Integer> volumeProfile = new TreeMap<>();  
    private double previousPrice = Double.NaN;
    private long startTime = 0, currentTimestamp;
    private final long TIME_LIMIT = 120 * 1_000_000_000L;
    private volatile double POC_BUFFER = 2;
    private static final ZoneId NY_ZONE_ID = ZoneId.of("America/New_York");
    private double pointOfControlPrice = Double.NaN;
    private int pointOfControlVolume = 0;
    private static final double VOLUME_TRIGGER_THRESHOLD = 0.00;
    private static final int POC_ROBUSTNESS_THRESHOLD = 0;
    private int volumeSum = 0;
    private int prevVolumeSum = 0;
    private LinkedList<Double> recentPrices = new LinkedList<>();
    private static final int PRICE_WINDOW_SIZE = 50;  // Define your window size
    private Indicator stdDevIndicator;
    private Settings settings;
    private Api api;

    /**
     * Initializes the PocTrading class with the given alias, instrument info, API, and initial state.
     * Registers indicators and sets their colors.
     * 
     * @param alias the alias for the PocTrading class
     * @param info the instrument info
     * @param api the API to use
     * @param initialState the initial state to use
     */
    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        this.api = api;
        this.settings = api.getSettings(Settings.class);

        Indicator[] indicators = {
            priceIndicator = api.registerIndicator("Price Indicator", GraphType.PRIMARY, Double.NaN),
            pocIndicator = api.registerIndicator("POC Indicator", GraphType.PRIMARY, Double.NaN),
            stdDevIndicator = api.registerIndicator("Standard Deviation", GraphType.BOTTOM, Double.NaN)
        };

        pocTradeIndicator = api.registerIndicator("Trade Signal", GraphType.PRIMARY);
        
        Color[] colors = {Color.BLUE, Color.RED, Color.GREEN};  // Added Color.GREEN

        IntStream.range(0, indicators.length)
            .forEach(i -> indicators[i].setColor(colors[i]));
    }

    /**
     * This method is called whenever a trade occurs. It updates various indicators and checks if the trading conditions are met.
     * If the conditions are met, it adds an icon to the chart indicating a trade should be made.
     *
     * @param price The price of the trade.
     * @param size The size of the trade.
     * @param tradeInfo Information about the trade.
     */
    @Override
    public synchronized void onTrade(double price, int size, TradeInfo tradeInfo) {

        volumeSum += size;
        
        /**
         * Calculates the volume rate of change.
         *
         * @return the volume rate of change.
         */
        double volumeRateOfChange = calculateVolumeRateOfChange();

        /**
         * Converts the current timestamp to a ZonedDateTime object in the New York time zone.
         * 
         * @param currentTimestamp the current timestamp in nanoseconds
         * @return the current time in the New York time zone
         */
        ZonedDateTime currentTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(currentTimestamp / 1_000_000_000), NY_ZONE_ID);
        
        /**
         * Sets the start time for trading in New York based on the current time and the specified settings.
         * 
         * @param currentTime the current time
         * @param settings the trading settings
         * @return the start time for trading in New York
         */
        ZonedDateTime startTimeNY = currentTime.withHour(settings.START_HOUR).withMinute(settings.START_MINUTE).withDayOfYear(currentTime.getDayOfYear()).withYear(currentTime.getYear());

        if (currentTime.isBefore(startTimeNY)) {
            return;
        }

        priceIndicator.addPoint(price);

        
        recentPrices.add(price);
        if (recentPrices.size() > PRICE_WINDOW_SIZE) {
            recentPrices.removeFirst();
        }
        // New code to calculate standard deviation
        double standardDeviation = calculateStandardDeviation();

        boolean isHighVolatility = standardDeviation > 0.5;
        Log.info("Is High Volatility: " + isHighVolatility);

        volumeProfile.merge(price, size, (a, b) -> a + b);
        int currentPriceVolume = volumeProfile.get(price);
        if (currentPriceVolume > pointOfControlVolume) {
            pointOfControlPrice = price;
            pointOfControlVolume = currentPriceVolume;
        }
        pocIndicator.addPoint(pointOfControlPrice);  // Updated Line
        stdDevIndicator.addPoint(standardDeviation);

        int pocRobustness = volumeProfile.getOrDefault(pointOfControlPrice, 0);  // Updated Line

        if (currentTimestamp - startTime >= TIME_LIMIT && !Double.isNaN(previousPrice)) {
            boolean enteredUpper = previousPrice > pointOfControlPrice + POC_BUFFER && price <= pointOfControlPrice + POC_BUFFER;  // Updated Line
            boolean exitedLower = previousPrice < pointOfControlPrice - POC_BUFFER && price >= pointOfControlPrice - POC_BUFFER;  // Updated Line

            boolean volumeTrigger = volumeRateOfChange > VOLUME_TRIGGER_THRESHOLD; // A x% increase in volume
            boolean pocStable = pocRobustness > POC_ROBUSTNESS_THRESHOLD; // POC has been the same for more than x trades

            // Updated trading logic to include volatility check
        if ((enteredUpper || exitedLower) && volumeTrigger && pocStable && isHighVolatility) {
            pocTradeIndicator.addIcon(price, createTranslucentCircle(enteredUpper), 1, 1);
            }
        }

        previousPrice = price;
        prevVolumeSum = volumeSum;
    }

    /**
     * Calculates the standard deviation of the recent prices.
     *
     * @return the standard deviation of the recent prices
     */
    private double calculateStandardDeviation() {
        double mean = recentPrices.stream().mapToDouble(val -> val).average().orElse(0.0);
        double variance = recentPrices.stream().mapToDouble(val -> Math.pow(val - mean, 2)).sum() / recentPrices.size();
        double standardDeviation = Math.sqrt(variance);
        Log.info("Standard Deviation: " + standardDeviation);
        return standardDeviation;
    }

    /**
     * Creates a translucent circle icon with a specified color based on the given boolean value.
     * 
     * @param isBid a boolean value indicating whether the icon represents a bid or not
     * @return a BufferedImage object representing the created icon
     */
    private BufferedImage createTranslucentCircle(boolean isBid) {
        int diameter = 20;
        BufferedImage icon = new BufferedImage(diameter, diameter, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = icon.createGraphics();
        g.setColor(isBid ? new Color(0, 255, 0, 127) : new Color(255, 0, 0, 127));
        g.fillOval(0, 0, diameter, diameter);
        g.dispose();
        return icon;
    }

    /**
     * Calculates the volume rate of change.
     * 
     * @return the volume rate of change
     */
    private double calculateVolumeRateOfChange() {
        return prevVolumeSum == 0 ? 0 : (double) (volumeSum - prevVolumeSum) / prevVolumeSum;
    }

    /**
     * Returns an array of custom settings panels for the strategy.
     * The first panel contains a slider for adjusting the ticks distance from POC.
     * The second panel contains spinners for setting the start time of the strategy.
     * Changes made to the settings in the panels are reflected in the strategy's settings and saved using the API.
     *
     * @return an array of {@link StrategyPanel} objects representing the custom settings panels for the strategy
     */
    @Override
    public StrategyPanel[] getCustomSettingsPanels() {
        StrategyPanel panel1 = new StrategyPanel("Settings Ticks distance from POC");
        
        JSlider pocBufferSlider = new JSlider(0, 10, (int) settings.POC_BUFFER);
        pocBufferSlider.setMajorTickSpacing(1);
        pocBufferSlider.setPaintTicks(true);
        pocBufferSlider.setPaintLabels(true);
        pocBufferSlider.addChangeListener(e -> {
            settings.POC_BUFFER = ((JSlider) e.getSource()).getValue();
            api.setSettings(settings);
        });
        panel1.add(pocBufferSlider);
        
        StrategyPanel panel2 = new StrategyPanel("Start Time Settings");
        
        JSpinner hourSpinner = new JSpinner(new SpinnerNumberModel(settings.START_HOUR, 0, 23, 1));
        hourSpinner.addChangeListener(e -> {
            settings.START_HOUR = (Integer) hourSpinner.getValue();
            api.setSettings(settings);
        });
        
        JSpinner minuteSpinner = new JSpinner(new SpinnerNumberModel(settings.START_MINUTE, 0, 59, 1));
        minuteSpinner.addChangeListener(e -> {
            settings.START_MINUTE = (Integer) minuteSpinner.getValue();
            api.setSettings(settings);
        });
        
        panel2.add(hourSpinner);
        panel2.add(minuteSpinner);
        
        return new StrategyPanel[]{panel1, panel2};
    }

    /**
     * This method is called by the trading engine to provide the current timestamp in nanoseconds.
     * It updates the current timestamp and sets the start time if it has not been set yet.
     *
     * @param nanoseconds the current timestamp in nanoseconds
     */
    @Override
    public void onTimestamp(long nanoseconds) {
        currentTimestamp = nanoseconds;
        if (startTime == 0) startTime = nanoseconds;
    }

    /**
     * Stops the trading process.
     */
    @Override
    public void stop() {}
}
