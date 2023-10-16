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

@Layer1SimpleAttachable
@Layer1StrategyName("POC with Standard Deviation  ")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION1)
public class PocTrading implements CustomModule, TradeDataListener, TimeListener, CustomSettingsPanelProvider {

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

    @Override
    public synchronized void onTrade(double price, int size, TradeInfo tradeInfo) {

        volumeSum += size;
        double volumeRateOfChange = calculateVolumeRateOfChange();

        ZonedDateTime currentTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(currentTimestamp / 1_000_000_000), NY_ZONE_ID);
        ZonedDateTime startTimeNY = currentTime.withHour(settings.START_HOUR).withMinute(settings.START_MINUTE).withDayOfYear(currentTime.getDayOfYear()).withYear(currentTime.getYear());

        if (currentTime.isBefore(startTimeNY)) {
            return;
        }

        priceIndicator.addPoint(price);

        // New code to update recent price data
        recentPrices.add(price);
        if (recentPrices.size() > PRICE_WINDOW_SIZE) {
            recentPrices.removeFirst();
        }
        // New code to calculate standard deviation
        double standardDeviation = calculateStandardDeviation();

        boolean isHighVolatility = standardDeviation > 0.5;
        Log.info("Is High Volatility: " + isHighVolatility);

        volumeProfile.merge(price, size, Integer::sum);
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

    private double calculateStandardDeviation() {
        double mean = recentPrices.stream().mapToDouble(val -> val).average().orElse(0.0);
        double variance = recentPrices.stream().mapToDouble(val -> Math.pow(val - mean, 2)).sum() / recentPrices.size();
        double standardDeviation = Math.sqrt(variance);
        Log.info("Standard Deviation: " + standardDeviation);
        return standardDeviation;
    }

    private BufferedImage createTranslucentCircle(boolean isBid) {
        int diameter = 20;
        BufferedImage icon = new BufferedImage(diameter, diameter, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = icon.createGraphics();
        g.setColor(isBid ? new Color(0, 255, 0, 127) : new Color(255, 0, 0, 127));
        g.fillOval(0, 0, diameter, diameter);
        g.dispose();
        return icon;
    }

    private double calculateVolumeRateOfChange() {
        return prevVolumeSum == 0 ? 0 : (double) (volumeSum - prevVolumeSum) / prevVolumeSum;
    }

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

    @Override
    public void onTimestamp(long nanoseconds) {
        currentTimestamp = nanoseconds;
        if (startTime == 0) startTime = nanoseconds;
    }

    @Override
    public void stop() {}
}
