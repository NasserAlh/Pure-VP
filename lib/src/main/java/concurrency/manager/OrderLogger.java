package concurrency.manager;

import velox.api.layer1.data.ExecutionInfo;
import velox.api.layer1.data.OrderInfoUpdate;

public class OrderLogger {

    public String formatOrderExecuted(ExecutionInfo executionInfo) {
        return "Order Executed," + executionInfo.orderId + ",,," + executionInfo.price + "," + executionInfo.time;
    }

    public String formatOrderUpdated(OrderInfoUpdate orderInfoUpdate) {
        return "Order Updated," + orderInfoUpdate.orderId + "," + orderInfoUpdate.isBuy + "," + orderInfoUpdate.status + ",,";
    }
}