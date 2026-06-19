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
import lk.jiat.techmart.entity.AuditLog;
import lk.jiat.techmart.messaging.OrderEventPayload;

import java.util.logging.Level;
import java.util.logging.Logger;

@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "jms/OrderEventsTopic"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Topic"),
        @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "Durable"),
        @ActivationConfigProperty(propertyName = "clientId", propertyValue = "techmart-app"),
        @ActivationConfigProperty(propertyName = "subscriptionName", propertyValue = "AuditLogSub"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge")
})
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class AuditLogMDB implements MessageListener {

    private static final Logger LOGGER = Logger.getLogger(AuditLogMDB.class.getName());

    @PersistenceContext(unitName = "TechMartPU", type = PersistenceContextType.TRANSACTION)
    private EntityManager em;

    @Override
    public void onMessage(Message message) {
        if (!(message instanceof TextMessage)) {
            LOGGER.log(Level.WARNING, "AuditLogMDB received a non-text message, ignoring");
            return;
        }

        try {
            String body = ((TextMessage) message).getText();
            OrderEventPayload event = OrderEventPayload.parse(body);

            AuditLog entry = new AuditLog(
                    event.getOrderId(),
                    event.getEventType(),
                    "status=" + event.getStatus() + ", totalAmount=" + event.getTotalAmount()
            );
            em.persist(entry);

            LOGGER.log(Level.FINE, "AuditLogMDB recorded event {0} for order {1}",
                    new Object[]{event.getEventType(), event.getOrderId()});

        } catch (JMSException e) {
            LOGGER.log(Level.SEVERE, "Failed to read audit message body", e);
            throw new jakarta.ejb.EJBException(e);
        }
    }
}