package lk.jiat.techmart.ejb.notification;

import jakarta.ejb.AsyncResult;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.Local;
import jakarta.ejb.Stateless;
import lk.jiat.techmart.api.NotificationServiceLocal;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@Local(NotificationServiceLocal.class)
public class NotificationServiceBean implements NotificationServiceLocal {

    private static final Logger LOGGER = Logger.getLogger(NotificationServiceBean.class.getName());

    @Override
    @Asynchronous
    public Future<Boolean> sendOrderConfirmation(Long orderId, String customerEmail) {
        try {
            boolean dispatched = dispatchEmail(customerEmail, "Order Confirmation",
                    "Your order #" + orderId + " has been received.");
            return new AsyncResult<>(dispatched);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to send order confirmation for order " + orderId, e);
            return new AsyncResult<>(false);
        }
    }

    @Override
    @Asynchronous
    public Future<Boolean> sendShipmentNotification(Long orderId, String customerEmail, String trackingCode) {
        try {
            boolean dispatched = dispatchEmail(customerEmail, "Order Shipped",
                    "Your order #" + orderId + " has shipped. Tracking: " + trackingCode);
            return new AsyncResult<>(dispatched);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to send shipment notification for order " + orderId, e);
            return new AsyncResult<>(false);
        }
    }

    private boolean dispatchEmail(String recipient, String subject, String body) throws InterruptedException {
        Thread.sleep(150);
        LOGGER.log(Level.INFO, "Email dispatched to {0}: {1}", new Object[]{recipient, subject});
        return true;
    }
}