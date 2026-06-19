package lk.jiat.techmart.ejb.messaging;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJB;
import jakarta.ejb.MessageDriven;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;
import lk.jiat.techmart.api.NotificationServiceLocal;
import lk.jiat.techmart.entity.Order;
import lk.jiat.techmart.messaging.OrderEventPayload;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "jms/OrderEventsTopic"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Topic"),
        @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "Durable"),
        @ActivationConfigProperty(propertyName = "clientId", propertyValue = "techmart-app"),
        @ActivationConfigProperty(propertyName = "subscriptionName", propertyValue = "CustomerNotificationSub"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge")
})
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class CustomerNotificationMDB implements MessageListener {

    private static final Logger LOGGER = Logger.getLogger(CustomerNotificationMDB.class.getName());

    @PersistenceContext(unitName = "TechMartPU", type = PersistenceContextType.TRANSACTION)
    private EntityManager em;

    @EJB
    private NotificationServiceLocal notificationService;

    @Override
    public void onMessage(Message message) {
        if (!(message instanceof TextMessage)) {
            LOGGER.log(Level.WARNING, "CustomerNotificationMDB received a non-text message, ignoring");
            return;
        }

        try {
            String body = ((TextMessage) message).getText();
            OrderEventPayload event = OrderEventPayload.parse(body);

            if (!"ORDER_CONFIRMED".equals(event.getEventType())) {
                LOGGER.log(Level.FINE, "CustomerNotificationMDB ignoring event type {0} for order {1}",
                        new Object[]{event.getEventType(), event.getOrderId()});
                return;
            }

            notifyCustomer(event.getOrderId());

        } catch (JMSException e) {
            LOGGER.log(Level.SEVERE, "Failed to read notification message body", e);
            throw new jakarta.ejb.EJBException(e);
        }
    }

    private void notifyCustomer(Long orderId) {
        Order order = em.find(Order.class, orderId);
        if (order == null || order.getCustomer() == null) {
            LOGGER.log(Level.WARNING, "CustomerNotificationMDB could not resolve order/customer for {0}", orderId);
            return;
        }

        Future<Boolean> result = notificationService.sendShipmentNotification(
                orderId, order.getCustomer().getEmail(), "PENDING-DISPATCH");

        try {
            boolean sent = result.get(2, TimeUnit.SECONDS);
            LOGGER.log(Level.INFO, "Topic-driven notification for order {0} dispatched={1}",
                    new Object[]{orderId, sent});
        } catch (TimeoutException te) {
            LOGGER.log(Level.WARNING, "Topic-driven notification for order {0} timed out after 2s", orderId);
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.log(Level.SEVERE, "Topic-driven notification failed for order " + orderId, e);
        }
    }
}