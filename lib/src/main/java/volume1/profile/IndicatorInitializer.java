package volume1.profile;

import velox.api.layer1.simplified.Api;
import velox.api.layer1.simplified.Indicator;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;

import java.awt.*;
import java.util.stream.IntStream;

public class IndicatorInitializer {

    private Api api;

    private Indicator priceIndicator;
    private Indicator pocIndicator;
    private Indicator stdDevIndicator;
    private Indicator pocTradeIndicator;

    public IndicatorInitializer(Api api) {
        this.api = api;
    }

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

    public Indicator getPriceIndicator() {
        return priceIndicator;
    }

    public Indicator getPocIndicator() {
        return pocIndicator;
    }

    public Indicator getStdDevIndicator() {
        return stdDevIndicator;
    }

    public Indicator getPocTradeIndicator() {
        return pocTradeIndicator;
    }
}

