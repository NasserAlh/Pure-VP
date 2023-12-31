package pure.vp;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.swing.JSlider;

import velox.api.layer1.annotations.*;
import velox.api.layer1.data.*;
import velox.api.layer1.messages.indicators.
Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.api.layer1.simplified.*;
import velox.gui.StrategyPanel;

@Layer1SimpleAttachable
@Layer1StrategyName("POC from Docker and VSCode")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION1)
public class PocTrading implements CustomModule, TradeDataListener, TimeListener, CustomSettingsPanelProvider  {

    private Indicator priceIndicator, volumeIndicator, pocIndicator, PocTrade;
    private ConcurrentSkipListMap<Double, Integer> volumeProfile = new ConcurrentSkipListMap<>();
    private double previousPrice = Double.NaN, pointOfControl = Double.NaN;
    private long startTime = 0, currentTimestamp;
    private final long TIME_LIMIT = 120 * 1_000_000_000L;
    private volatile double POC_BUFFER = 2; 


    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        priceIndicator = api.registerIndicator("Price Indicator", GraphType.PRIMARY, Double.NaN);
        volumeIndicator = api.registerIndicator("Volume Indicator", GraphType.BOTTOM, Double.NaN);
        pocIndicator = api.registerIndicator("POC Indicator", GraphType.PRIMARY, Double.NaN);
        PocTrade = api.registerIndicator("Trade Signal", GraphType.PRIMARY);

        Color[] colors = {Color.BLUE, Color.YELLOW, Color.RED};
        Indicator[] indicators = {priceIndicator, volumeIndicator, pocIndicator};
        for (int i = 0; i < indicators.length; i++) {
            indicators[i].setColor(colors[i]);
        }
    }

    @Override
    public synchronized void onTrade(double price, int size, TradeInfo tradeInfo) {
        // Convert the currentTimestamp to New York time
        ZonedDateTime currentTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(currentTimestamp / 1_000_000_000), ZoneId.of("America/New_York"));
        ZonedDateTime startTimeNY = ZonedDateTime.ofInstant(Instant.ofEpochSecond(startTime / 1_000_000_000), ZoneId.of("America/New_York")).withHour(9).withMinute(45);
        
        // Exit the method early if before 9:45 am New York time
        if (currentTime.isBefore(startTimeNY)) {
            return;
        }
    
        priceIndicator.addPoint(price);
        volumeProfile.merge(price, size, (a, b) -> a + b);
        volumeIndicator.addPoint(volumeProfile.get(price));
        pointOfControl = volumeProfile.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(price);
        pocIndicator.addPoint(pointOfControl);
    
        if (currentTimestamp - startTime >= TIME_LIMIT && !Double.isNaN(previousPrice)) {
            boolean enteredUpper = previousPrice > pointOfControl + POC_BUFFER && price <= pointOfControl + POC_BUFFER;
            boolean exitedLower = previousPrice < pointOfControl - POC_BUFFER && price >= pointOfControl - POC_BUFFER;
            if (enteredUpper || exitedLower) {
                PocTrade.addIcon(price, createTranslucentCircle(enteredUpper), 1, 1);
            }
        }
    
        previousPrice = price;
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

    @Override
    public StrategyPanel[] getCustomSettingsPanels() {
        StrategyPanel panel = new StrategyPanel("Settings Ticks distance from POC");
        
        JSlider pocBufferSlider = new JSlider(0, 10, (int) POC_BUFFER); 
        pocBufferSlider.setMajorTickSpacing(1);
        pocBufferSlider.setPaintTicks(true);
        pocBufferSlider.setPaintLabels(true);
        pocBufferSlider.addChangeListener(e -> POC_BUFFER = ((JSlider) e.getSource()).getValue());
        panel.add(pocBufferSlider);
        return new StrategyPanel[]{panel};
    }

    @Override
    public void onTimestamp(long nanoseconds) {
        currentTimestamp = nanoseconds;
        if (startTime == 0) startTime = nanoseconds;
    }

    @Override
    public void stop() {}
}
