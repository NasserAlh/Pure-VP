package volume.profile;

import velox.api.layer1.common.Log;
import velox.api.layer1.data.OrderDuration;
import velox.api.layer1.data.SimpleOrderSendParameters;
import velox.api.layer1.data.SimpleOrderSendParametersBuilder;
import velox.api.layer1.simplified.Api;

public class OrderPlacer {
    private final String alias;
    private final Api api;

    public OrderPlacer(String alias, Api api) {
        this.alias = alias;
        this.api = api;
    }

    public void placeOrder(boolean isBuy, double price, int quantity) {
        try {
            SimpleOrderSendParametersBuilder builder = new SimpleOrderSendParametersBuilder(alias, isBuy, quantity);
            builder.setDuration(OrderDuration.IOC);
            SimpleOrderSendParameters order = builder.build();
            api.sendOrder(order);
        } catch (Exception e) {
            Log.info("Error placing order", e);
        }
    }
}
