package concurrency.manager;

import velox.api.layer1.simplified.Api;
import velox.api.layer1.simplified.Indicator;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;

import java.awt.*;
import java.util.stream.IntStream;

/**
 * This class is responsible for initializing the indicators used in the application.
 */
public class IndicatorInitializer {

    private Api api;

    private Indicator priceIndicator;
    private Indicator pocIndicator;
    private Indicator stdDevIndicator;
    private Indicator pocTradeIndicator;

    /**
     * Constructor for the IndicatorInitializer class.
     * @param api The Api object used to register the indicators.
     */
    public IndicatorInitializer(Api api) {
        this.api = api;
    }

    /**
     * Initializes the indicators used in the application.
     */
    public void initializeIndicators() {
        Indicator[] indicators = {
                priceIndicator = api.registerIndicator("Price Indicator", GraphType.PRIMARY, Double.NaN),
                pocIndicator = api.registerIndicator("POC Indicator", GraphType.PRIMARY, Double.NaN),
                stdDevIndicator = api.registerIndicator("Standard Deviation", GraphType.BOTTOM, Double.NaN)
        };

        pocTradeIndicator = api.registerIndicator("Trade Signal", GraphType.PRIMARY);

        Color[] colors = {Color.BLUE, Color.RED, Color.GREEN};

        IntStream.range(0, indicators.length)
                .forEach(i -> indicators[i].setColor(colors[i]));
    }

    /**
     * Getter method for the priceIndicator field.
     * @return The priceIndicator object.
     */
    public Indicator getPriceIndicator() {
        return priceIndicator;
    }

    /**
     * Getter method for the pocIndicator field.
     * @return The pocIndicator object.
     */
    public Indicator getPocIndicator() {
        return pocIndicator;
    }

    /**
     * Getter method for the stdDevIndicator field.
     * @return The stdDevIndicator object.
     */
    public Indicator getStdDevIndicator() {
        return stdDevIndicator;
    }

    /**
     * Getter method for the pocTradeIndicator field.
     * @return The pocTradeIndicator object.
     */
    public Indicator getPocTradeIndicator() {
        return pocTradeIndicator;
    }
}

