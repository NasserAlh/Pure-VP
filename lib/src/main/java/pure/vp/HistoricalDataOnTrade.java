package pure.vp;

import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ConcurrentSkipListMap;
import velox.api.layer1.annotations.*;
import velox.api.layer1.data.*;
import velox.api.layer1.simplified.*;

import velox.api.layer1.data.TradeInfo;


@Layer1SimpleAttachable
@Layer1StrategyName("Point of Control Logger")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION1)

public class HistoricalDataOnTrade implements CustomModuleAdapter, TradeDataListener, TimeAdapter {
    private FileWriter csvWriter;
    private ConcurrentSkipListMap<Double, Integer> volumeProfile = new ConcurrentSkipListMap<>();
    private long timestamp;

    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        try {
            csvWriter = new FileWriter("poc_data.csv");
            csvWriter.append("Timestamp,Price,Volume,PointOfControl,TotalVolumeAtPOC\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onTimestamp(long nanoseconds) {
        timestamp = nanoseconds;
    }

    @Override
    public void onTrade(double price, int size, TradeInfo tradeInfo) {
        volumeProfile.merge(price, size, (a, b) -> a + b);
        double pointOfControl = volumeProfile.entrySet().stream().max(
            java.util.Map.Entry.comparingByValue()).map(java.util.Map.Entry::getKey).orElse(price);
        int totalVolumeAtPOC = volumeProfile.getOrDefault(pointOfControl, 0);
        
        try {
            csvWriter.append(String.format("%d,%f,%d,%f,%d\n",
                timestamp, price, size, pointOfControl, totalVolumeAtPOC));
            csvWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        try {
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

