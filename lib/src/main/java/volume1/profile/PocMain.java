package volume1.profile;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import javax.swing.*;
import velox.api.layer1.annotations.*;
import velox.api.layer1.common.Log;
import velox.api.layer1.data.*;
import velox.api.layer1.simplified.*;
import velox.gui.StrategyPanel;

@Layer1TradingStrategy
@Layer1SimpleAttachable
@Layer1StrategyName("POC Ordering Functionality")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION1)
public class PocMain implements CustomModule, TradeDataListener, TimeListener, CustomSettingsPanelProvider {

    private Indicator priceIndicator, pocIndicator, pocTradeIndicator;
    private double previousPrice = Double.NaN;
    private long startTime = 0, currentTimestamp;
    private final long TIME_LIMIT = 120 * 1_000_000_000L;
    private static final ZoneId NY_ZONE_ID = ZoneId.of("America/New_York");
    private static final double VOLUME_TRIGGER_THRESHOLD = 0.00;
    private static final int POC_ROBUSTNESS_THRESHOLD = 0;
    private int volumeSum = 0;
    private int prevVolumeSum = 0;
    private Indicator stdDevIndicator;
    private Settings settings;
    private Api api;
    private VolumeProfileHandler volumeProfileHandler = new VolumeProfileHandler();
    private PointOfControlHandler pointOfControlHandler = new PointOfControlHandler();
    private StandardDeviationHandler standardDeviationHandler = new StandardDeviationHandler();
    private IndicatorInitializer indicatorInitializer;
    private OrderPlacer orderPlacer;
    private OrderStatusManager orderStatusManager;  // Added field

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        this.api = api;
        this.settings = api.getSettings(Settings.class);
        this.orderStatusManager = new OrderStatusManager();  // Instantiate OrderStatusManager
        this.orderPlacer = new OrderPlacer(alias, api, orderStatusManager);  // Pass OrderStatusManager to OrderPlacer

        this.indicatorInitializer = new IndicatorInitializer(api);
        indicatorInitializer.initializeIndicators();

        priceIndicator = indicatorInitializer.getPriceIndicator();
        pocIndicator = indicatorInitializer.getPocIndicator();
        stdDevIndicator = indicatorInitializer.getStdDevIndicator();
        pocTradeIndicator = indicatorInitializer.getPocTradeIndicator();
    }

    @Override
    public void onTrade(double price, int size, TradeInfo tradeInfo) {
        volumeSum += size;
        double volumeRateOfChange = calculateVolumeRateOfChange();
    
        ZonedDateTime currentTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(currentTimestamp / 1_000_000_000), NY_ZONE_ID);
        ZonedDateTime startTimeNY = currentTime.withHour(settings.START_HOUR).withMinute(settings.START_MINUTE).withDayOfYear(currentTime.getDayOfYear()).withYear(currentTime.getYear());
    
        if (currentTime.isBefore(startTimeNY)) {return;}
    
        priceIndicator.addPoint(price);
    
        // Updating volume profile using the VolumeProfileHandler
        volumeProfileHandler.addTrade(price, size);
        
        // Updating point of control using the PointOfControlHandler
        pointOfControlHandler.updatePOC(price, volumeProfileHandler.getVolumeProfile().get(price));
        
        // Updating recent price data and calculating standard deviation using the StandardDeviationHandler
        standardDeviationHandler.addPrice(price);
        double standardDeviation = standardDeviationHandler.calculateStandardDeviation();
    
        boolean isHighVolatility = standardDeviation > settings.STANDARD_DEVIATION_THRESHOLD;
        Log.info("Is High Volatility: " + isHighVolatility);
    
        double pointOfControlPrice = pointOfControlHandler.getPointOfControlPrice();  // Obtaining POC price from the handler
        pocIndicator.addPoint(pointOfControlPrice);
        stdDevIndicator.addPoint(standardDeviation);
    
        int pocRobustness = volumeProfileHandler.getVolumeProfile().getOrDefault(pointOfControlPrice, 0);
    
        if (currentTimestamp - startTime >= TIME_LIMIT && !Double.isNaN(previousPrice)) {
            boolean enteredUpper = previousPrice > pointOfControlPrice + settings.POC_BUFFER && price <= pointOfControlPrice + settings.POC_BUFFER;
            boolean exitedLower = previousPrice < pointOfControlPrice - settings.POC_BUFFER && price >= pointOfControlPrice - settings.POC_BUFFER;
    
            boolean volumeTrigger = volumeRateOfChange > VOLUME_TRIGGER_THRESHOLD;
            boolean pocStable = pocRobustness > POC_ROBUSTNESS_THRESHOLD;
    
            if ((enteredUpper || exitedLower) && volumeTrigger && pocStable && isHighVolatility && !orderStatusManager.isOrderOpen()) {  // Check order status
                pocTradeIndicator.addIcon(price, createTranslucentCircle(enteredUpper), 1, 1);
    
                // Assuming a quantity of 1 for simplicity, adjust as needed
                int quantity = 1;  
                if (enteredUpper) {
                    // Place a sell order if entered upper threshold
                    orderPlacer.placeOrder(false, price, quantity);
                } else if (exitedLower) {
                    // Place a buy order if exited lower threshold
                    orderPlacer.placeOrder(true, price, quantity);
                }
            }
        }
    
        previousPrice = price;
        prevVolumeSum = volumeSum;
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

        StrategyPanel panel3 = new StrategyPanel("Standard Deviation Threshold Settings");
        
        JSpinner stdDevThresholdSpinner = new JSpinner(new SpinnerNumberModel(settings.STANDARD_DEVIATION_THRESHOLD, 0.0, 10.0, 0.1));
        stdDevThresholdSpinner.addChangeListener(e -> {
            settings.STANDARD_DEVIATION_THRESHOLD = (Double) stdDevThresholdSpinner.getValue();
            api.setSettings(settings);
        });
    
        panel3.add(new JLabel("Standard Deviation Threshold:"));
        panel3.add(stdDevThresholdSpinner);
    
        return new StrategyPanel[]{panel1, panel2, panel3};
    }

    @Override
    public void onTimestamp(long nanoseconds) {
        currentTimestamp = nanoseconds;
        if (startTime == 0) startTime = nanoseconds;
    }

    @Override
    public void stop() {}
}
