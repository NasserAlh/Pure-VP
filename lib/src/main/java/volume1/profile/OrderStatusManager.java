package volume1.profile;

import velox.api.layer1.common.Log;
import velox.api.layer1.data.ExecutionInfo;
import velox.api.layer1.data.OrderInfoUpdate;
import velox.api.layer1.data.OrderStatus;

import java.util.ArrayList;
import java.util.List;

public class OrderStatusManager {

    private boolean isOrderOpen = false;
    private String openOrderId = null;
    private final List<OrderStatusObserver> observers = new ArrayList<>();

    // Observer Interface
    public interface OrderStatusObserver {
        void onOrderStatusChanged(String orderId, OrderStatus status);
    }

    // Method to add observer
    public void addObserver(OrderStatusObserver observer) {
        observers.add(observer);
    }

    // Method to notify observers
    private void notifyObservers(String orderId, OrderStatus status) {
        for (OrderStatusObserver observer : observers) {
            observer.onOrderStatusChanged(orderId, status);
        }
    }

    public synchronized void onOrderUpdated(OrderInfoUpdate orderInfoUpdate) {
        if (orderInfoUpdate != null && orderInfoUpdate.orderId != null) {
            OrderStatus status = orderInfoUpdate.status;
            String orderId = orderInfoUpdate.orderId;

            switch (status) {
                case FILLED:
                case CANCELLED:
                case REJECTED:
                    handleOrderClosure(orderId, status);
                    break;

                case WORKING:
                case PENDING_CANCEL:
                case PENDING_MODIFY:
                case PENDING_SUBMIT:
                    handleOrderWorking(orderId, status);
                    break;

                case SUSPENDED:
                case DISCONNECTED:
                    Log.warn("Order " + orderId + " is in a " + status.name().toLowerCase() + " state.");
                    break;

                default:
                    Log.warn("Unhandled order status: " + status.name() + " for order " + orderId);
                    break;
            }

            notifyObservers(orderId, status);
        }
    }

    public synchronized void onOrderExecuted(ExecutionInfo executionInfo) {
        if (executionInfo != null && executionInfo.orderId != null) {
            String orderId = executionInfo.orderId;
            if (orderId.equals(openOrderId)) {
                handleOrderExecution(orderId, executionInfo);
            } else {
                Log.warn("Execution info received for an unknown or unmatched order ID: " + orderId);
            }
        } else {
            Log.error("Received null execution info.", new NullPointerException());
        }
    }

    public synchronized void onOrderPlaced(String orderId) {
        if (orderId != null) {
            isOrderOpen = true;
            openOrderId = orderId;
            Log.info("Order " + orderId + " is placed.");
        }
    }

    public  synchronized boolean isOrderOpen() {
        return isOrderOpen;
    }

    public synchronized String getOpenOrderId() {
        return openOrderId;
    }

    private void handleOrderClosure(String orderId, OrderStatus status) {
        if (orderId.equals(openOrderId)) {
            isOrderOpen = false;
            openOrderId = null;
            Log.info("Order " + orderId + " is " + status.name().toLowerCase() + ".");
        }
    }

    private void handleOrderWorking(String orderId, OrderStatus status) {
        isOrderOpen = true;
        openOrderId = orderId;
        Log.info("Order " + orderId + " is " + status.name().toLowerCase() + ".");
    }

    private void handleOrderExecution(String orderId, ExecutionInfo executionInfo) {
        isOrderOpen = false;
        openOrderId = null;

        Log.info("Order " + orderId + " executed.");
        Log.info("Execution details: ");
        Log.info("Execution ID: " + executionInfo.executionId);
        Log.info("Execution Price: " + executionInfo.price);
        Log.info("Execution Size: " + executionInfo.size);
        Log.info("Execution Time: " + executionInfo.time);
        Log.info("Is Execution Simulated: " + executionInfo.isSimulated);
    }
}