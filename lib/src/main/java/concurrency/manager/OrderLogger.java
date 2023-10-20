package concurrency.manager;

import velox.api.layer1.data.ExecutionInfo;
import velox.api.layer1.data.OrderInfoUpdate;

/**
 * The OrderLogger class provides methods to format order execution and update information.
 */
public class OrderLogger {

    /**
     * Formats the execution information of an order.
     * @param executionInfo The execution information of the order.
     * @return A formatted string containing the order execution information.
     */
    public String formatOrderExecuted(ExecutionInfo executionInfo) {
        return "Order Executed," + executionInfo.orderId + ",,," + executionInfo.price + "," + executionInfo.time;
    }

    /**
     * Formats the update information of an order.
     * @param orderInfoUpdate The update information of the order.
     * @return A formatted string containing the order update information.
     */
    public String formatOrderUpdated(OrderInfoUpdate orderInfoUpdate) {
        return "Order Updated," + orderInfoUpdate.orderId + "," + orderInfoUpdate.isBuy + "," + orderInfoUpdate.status + ",,";
    }
}