package concurrency.manager;

import velox.api.layer1.common.Log;
import velox.api.layer1.data.OrderDuration;
import velox.api.layer1.data.SimpleOrderSendParameters;
import velox.api.layer1.data.SimpleOrderSendParametersBuilder;
import velox.api.layer1.simplified.Api;

/**
 * The OrderPlacer class is responsible for placing orders using the provided API.
 * It takes an alias and an API instance as constructor arguments and provides a method to place orders.
 */
public class OrderPlacer {
    private final String alias;
    private final Api api;

    /**
     * Constructs an OrderPlacer object with the given alias and API.
     * @param alias the alias for the OrderPlacer object
     * @param api the API to use for placing orders
     * @throws IllegalArgumentException if either alias or api is null
     */
    public OrderPlacer(String alias, Api api) {
        if (alias == null || api == null) {
            throw new IllegalArgumentException("Arguments cannot be null");
        }
        this.alias = alias;
        this.api = api;
    }

    /**
     * Places an order with the given parameters.
     * 
     * @param isBuy    true if the order is a buy order, false if it's a sell order
     * @param price    the price of the order
     * @param quantity the quantity of the order
     * @return true if the order was successfully placed, false otherwise
     */
    public boolean placeOrder(boolean isBuy, double price, int quantity) {
        try {
            SimpleOrderSendParametersBuilder builder = new SimpleOrderSendParametersBuilder(alias, isBuy, quantity);
            builder.setDuration(OrderDuration.IOC);
            SimpleOrderSendParameters order = builder.build();
            api.sendOrder(order);
            return true;
        } catch (Exception e) {
            Log.error("Error placing order: " + e.getMessage(), e);
            return false;
        }
    }
}