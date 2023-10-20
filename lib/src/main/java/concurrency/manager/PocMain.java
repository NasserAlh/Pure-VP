package concurrency.manager;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import javax.swing.*;
import velox.api.layer1.annotations.*;
import velox.api.layer1.data.*;
import velox.api.layer1.simplified.*;
import velox.gui.StrategyPanel;

/**
 * This class represents a trading strategy that uses various indicators and handlers to place buy and sell orders
 * based on certain conditions. It implements CustomModule, TradeDataListener, TimeListener, OrdersListener, and
 * CustomSettingsPanelProvider interfaces to receive and process trade data, time updates, order updates, and custom
 * settings panel requests. The class is annotated with various Layer1 annotations to specify its trading strategy name,
 * attachable type, and API version.
 */
@Layer1TradingStrategy
@Layer1SimpleAttachable
@Layer1StrategyName("POC OrderId Headers")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION1)
public class PocMain implements CustomModule, TradeDataListener, TimeListener,
    OrdersListener, CustomSettingsPanelProvider {

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
    private OrderLogger orderLogger = new OrderLogger();
    private static final String LOG_PATH = "C:\\Bookmap\\Logs\\Order_log.csv";
    private BufferedWriter logWriter;


    /**
     * Initializes the PocMain object with the given alias, instrument info, API, and initial state.
     * 
     * @param alias the alias for the PocMain object
     * @param info the instrument info for the PocMain object
     * @param api the API for the PocMain object
     * @param initialState the initial state for the PocMain object
     */
    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        
        this.api = api;
        this.settings = api.getSettings(Settings.class);
        this.orderPlacer = new OrderPlacer(alias, api);

        this.indicatorInitializer = new IndicatorInitializer(api);
        indicatorInitializer.initializeIndicators();

        priceIndicator = indicatorInitializer.getPriceIndicator();
        pocIndicator = indicatorInitializer.getPocIndicator();
        stdDevIndicator = indicatorInitializer.getStdDevIndicator();
        pocTradeIndicator = indicatorInitializer.getPocTradeIndicator();

        initializeCsvFile();
    }

    /**
     * This method is called whenever a new trade is received. It updates various indicators and handlers
     * based on the trade information. If certain conditions are met, it places a buy or sell order using
     * the OrderPlacer object.
     *
     * @param price the price of the trade
     * @param size the size of the trade
     * @param tradeInfo the trade information object containing additional information about the trade
     */
    @Override
    public void onTrade(double price, int size, TradeInfo tradeInfo) {
        volumeSum += size;
        double volumeRateOfChange = calculateVolumeRateOfChange();

        ZonedDateTime currentTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(currentTimestamp / 1_000_000_000), NY_ZONE_ID);
        ZonedDateTime startTimeNY = currentTime.withHour(settings.START_HOUR).withMinute(settings.START_MINUTE).withDayOfYear
            (currentTime.getDayOfYear()).withYear(currentTime.getYear());

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

        double pointOfControlPrice = pointOfControlHandler.getPointOfControlPrice();  // Obtaining POC price from the handler
        pocIndicator.addPoint(pointOfControlPrice);
        stdDevIndicator.addPoint(standardDeviation);

        int pocRobustness = volumeProfileHandler.getVolumeProfile().getOrDefault(pointOfControlPrice, 0);

        if (currentTimestamp - startTime >= TIME_LIMIT && !Double.isNaN(previousPrice)) {
            boolean enteredUpper = previousPrice > pointOfControlPrice + settings.POC_BUFFER && price <= pointOfControlPrice + settings.POC_BUFFER;
            boolean exitedLower = previousPrice < pointOfControlPrice - settings.POC_BUFFER && price >= pointOfControlPrice - settings.POC_BUFFER;

            boolean volumeTrigger = volumeRateOfChange > VOLUME_TRIGGER_THRESHOLD;
            boolean pocStable = pocRobustness > POC_ROBUSTNESS_THRESHOLD;

            if ((enteredUpper || exitedLower) && volumeTrigger && pocStable && isHighVolatility) {
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
        
    /**
     * Creates a translucent circle image with the specified color.
     * 
     * @param isBid a boolean indicating whether the circle represents a bid or not
     * @return a BufferedImage object representing the created circle image
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
     * Calculates the rate of change of the volume sum.
     * @return the rate of change of the volume sum
     */
    private double calculateVolumeRateOfChange() {
        return prevVolumeSum == 0 ? 0 : (double) (volumeSum - prevVolumeSum) / prevVolumeSum;
    }

    /**
     * Returns an array of custom settings panels for the strategy.
     * The first panel contains a slider to adjust the ticks distance from POC.
     * The second panel contains spinners to adjust the start time settings.
     * The third panel contains a spinner to adjust the standard deviation threshold settings.
     * Each panel updates the corresponding setting in the strategy's settings object and sets it using the API.
     * @return an array of StrategyPanel objects representing the custom settings panels.
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

        StrategyPanel panel3 = new StrategyPanel("Standard Deviation Threshold Settings");
        
        JSpinner stdDevThresholdSpinner = new JSpinner(new SpinnerNumberModel(settings.STANDARD_DEVIATION_THRESHOLD,
            0.0, 10.0, 0.1));
        stdDevThresholdSpinner.addChangeListener(e -> {
            settings.STANDARD_DEVIATION_THRESHOLD = (Double) stdDevThresholdSpinner.getValue();
            api.setSettings(settings);
        });
    
        panel3.add(new JLabel("Standard Deviation Threshold:"));
        panel3.add(stdDevThresholdSpinner);
    
        return new StrategyPanel[]{panel1, panel2, panel3};
    }

    /**
     * This method is called when a new timestamp is received. It updates the current timestamp and sets the start time if it is not already set.
     * 
     * @param nanoseconds the new timestamp in nanoseconds
     */
    @Override
    public void onTimestamp(long nanoseconds) {
        currentTimestamp = nanoseconds;
        if (startTime == 0) startTime = nanoseconds;
    }

    /**
     * Stops the execution of the program and closes the log writer if it is not null.
     */
    @Override
    public void stop() {
        try {
            if (logWriter != null) {
                logWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }

    /**
     * This method is called when an order is executed and logs the execution information to a CSV file.
     * @param executionInfo the execution information of the order
     */
    @Override
    public void onOrderExecuted(ExecutionInfo executionInfo) {
        String logData = orderLogger.formatOrderExecuted(executionInfo);
        writeToCsvFile(logData);
    }

    /**
     * This method is called when an order is updated.
     * It formats the order information and writes it to a CSV file.
     * 
     * @param orderInfoUpdate the updated order information
     */
    @Override
    public void onOrderUpdated(OrderInfoUpdate orderInfoUpdate) {
        String logData = orderLogger.formatOrderUpdated(orderInfoUpdate);
        writeToCsvFile(logData);
    }

    /**
     * Initializes a CSV file by creating a BufferedWriter object and writing the header row to the file.
     * The header row contains the following columns: "Log Type", "Order ID", "IsBuy", "Status", "Price", and "Time".
     * @throws IOException if an I/O error occurs while writing to the file.
     */
    private void initializeCsvFile() {
        try {
            logWriter = new BufferedWriter(new FileWriter(LOG_PATH));
            logWriter.write("Log Type,Order ID,IsBuy,Status,Price, Time");
            logWriter.newLine();
            logWriter.flush(); 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes the given log data to a CSV file.
     * 
     * @param logData the data to be written to the CSV file
     */
    private void writeToCsvFile(String logData) {
        try {
            logWriter.write(logData);
            logWriter.newLine();
            logWriter.flush(); 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
