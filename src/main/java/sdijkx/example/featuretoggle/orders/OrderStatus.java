package sdijkx.example.featuretoggle.orders;

/**
 * Created by steven on 15-05-16.
 */
public class OrderStatus {
    public enum Status { NEW, OPEN, PAYED, TRANSPORT, CLOSED }

    private final String orderId;
    private final Status status;

    public OrderStatus(String orderId, Status status) {
        this.orderId = orderId;
        this.status = status;
    }

    public String getOrderId() {
        return orderId;
    }

    public Status getStatus() {
        return status;
    }
}
