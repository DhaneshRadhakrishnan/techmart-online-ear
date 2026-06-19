package lk.jiat.techmart.ejb.messaging;

import jakarta.ejb.ActivationConfigProperty;
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
import lk.jiat.techmart.entity.Order;
import lk.jiat.techmart.messaging.OrderEventPayload;

import java.util.logging.Level;
import java.util.logging.Logger;

@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "jms/OrderProcessingQueue"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge")
})
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class OrderFulfillmentMDB implements MessageListener {

    private static final Logger LOGGER = Logger.getLogger(OrderFulfillmentMDB.class.getName());

    @PersistenceContext(unitName = "TechMartPU", type = PersistenceContextType.TRANSACTION)
    private EntityManager em;

    @Override
    public void onMessage(Message message) {
        if (!(message instanceof TextMessage)) {
            LOGGER.log(Level.WARNING, "OrderFulfillmentMDB received a non-text message, ignoring");
            return;
        }

        try {
            String body = ((TextMessage) message).getText();
            OrderEventPayload event = OrderEventPayload.parse(body);

            LOGGER.log(Level.INFO, "OrderFulfillmentMDB dispatching warehouse fulfillment for order {0}",
                    event.getOrderId());

            markFulfilled(event.getOrderId());

        } catch (JMSException e) {
            LOGGER.log(Level.SEVERE, "Failed to read fulfillment message body", e);
            throw new jakarta.ejb.EJBException(e);
        }
    }

    private void markFulfilled(Long orderId) {
        Order order = em.find(Order.class, orderId);
        if (order == null) {
            LOGGER.log(Level.WARNING, "OrderFulfillmentMDB could not find order {0}, skipping", orderId);
            return;
        }
        order.setStatus(Order.OrderStatus.FULFILLED);
        em.merge(order);
        LOGGER.log(Level.INFO, "Order {0} marked FULFILLED by warehouse dispatch", orderId);
    }
}