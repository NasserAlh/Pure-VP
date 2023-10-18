package volume1.profile;

import velox.api.layer1.common.Log;
import velox.api.layer1.data.ExecutionInfo;
import velox.api.layer1.data.OrderInfoUpdate;
import velox.api.layer1.data.OrderStatus;  // Import the OrderStatus enum

public class OrderStatusManager {

    private boolean isOrderOpen = false;
    private String openOrderId = null;

    public synchronized void onOrderUpdated(OrderInfoUpdate orderInfoUpdate) {
        // Corrected the comparison to OrderStatus.FILLED
        if (orderInfoUpdate != null && orderInfoUpdate.orderId != null 
                && orderInfoUpdate.status == OrderStatus.FILLED 
                && orderInfoUpdate.orderId.equals(openOrderId)) {
            isOrderOpen = false;
            openOrderId = null;
            Log.info("Order " + orderInfoUpdate.orderId + " is filled.");
        }
    }

    public synchronized void onOrderExecuted(ExecutionInfo executionInfo) {
        if (executionInfo != null && executionInfo.orderId != null 
                && executionInfo.orderId.equals(openOrderId)) {
            isOrderOpen = false;
            openOrderId = null;
            Log.info("Order " + executionInfo.orderId + " is executed.");
        }
    }

    public synchronized void onOrderPlaced(String orderId) {
        if (orderId != null) {
            isOrderOpen = true;
            openOrderId = orderId;
            Log.info("Order " + orderId + " is placed.");
        }
    }

    public synchronized boolean isOrderOpen() {
        return isOrderOpen;
    }
}