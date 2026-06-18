package lk.jiat.techmart.api;

import java.util.concurrent.Future;

public interface NotificationServiceLocal {

    Future<Boolean> sendOrderConfirmation(Long orderId, String customerEmail);

    Future<Boolean> sendShipmentNotification(Long orderId, String customerEmail, String trackingCode);
}