package lk.jiat.techmart.ejb.order;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.Local;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.Queue;
import jakarta.jms.Topic;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;
import lk.jiat.techmart.api.InsufficientStockException;
import lk.jiat.techmart.api.InventoryManagerLocal;
import lk.jiat.techmart.api.NotificationServiceLocal;
import lk.jiat.techmart.api.OrderProcessingLocal;
import lk.jiat.techmart.dto.OrderLineRequestDTO;
import lk.jiat.techmart.dto.OrderRequestDTO;
import lk.jiat.techmart.dto.OrderResultDTO;
import lk.jiat.techmart.entity.Customer;
import lk.jiat.techmart.entity.Order;
import lk.jiat.techmart.entity.OrderItem;
import lk.jiat.techmart.entity.Product;

import java.math.BigDecimal;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@Local(OrderProcessingLocal.class)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class OrderProcessingFacade implements OrderProcessingLocal {

    private static final Logger LOGGER = Logger.getLogger(OrderProcessingFacade.class.getName());

    @PersistenceContext(unitName = "TechMartPU", type = PersistenceContextType.TRANSACTION)
    private EntityManager em;

    @EJB
    private InventoryManagerLocal inventoryManager;

    @EJB
    private NotificationServiceLocal notificationService;

    @Inject
    private JMSContext jmsContext;

    @Resource(lookup = "jms/OrderProcessingQueue")
    private Queue orderProcessingQueue;

    @Resource(lookup = "jms/OrderEventsTopic")
    private Topic orderEventsTopic;

    @Override
    public OrderResultDTO placeOrder(OrderRequestDTO request) throws InsufficientStockException {
        Customer customer = em.find(Customer.class, request.getCustomerId());
        if (customer == null) {
            throw new IllegalArgumentException("Unknown customer ID: " + request.getCustomerId());
        }

        for (OrderLineRequestDTO line : request.getLines()) {
            inventoryManager.reserveStock(line.getSku(), line.getQuantity());
        }

        Order order = new Order(customer, BigDecimal.ZERO);
        BigDecimal total = BigDecimal.ZERO;

        for (OrderLineRequestDTO line : request.getLines()) {
            Product product = em.createNamedQuery("Product.findBySku", Product.class)
                    .setParameter("sku", line.getSku())
                    .setMaxResults(1)
                    .getResultList()
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Product vanished mid-order: " + line.getSku()));

            OrderItem item = new OrderItem(product, line.getQuantity(), product.getPrice());
            order.addItem(item);
            total = total.add(item.getLineTotal());
        }

        order.setTotalAmount(total);
        order.setStatus(Order.OrderStatus.CONFIRMED);
        em.persist(order);
        em.flush();

        dispatchOrderEvents(order);
        sendFulfillmentMessage(order);
        publishOrderEvent(order, "ORDER_CONFIRMED");

        LOGGER.log(Level.INFO, "Order {0} placed for customer {1}, total {2}",
                new Object[]{order.getId(), customer.getId(), total});

        return new OrderResultDTO(order.getId(), order.getStatus().name(), total);
    }

    private void dispatchOrderEvents(Order order) {
        Future<Boolean> notificationResult = notificationService.sendOrderConfirmation(
                order.getId(), order.getCustomer().getEmail());

        try {
            boolean sent = notificationResult.get(2, TimeUnit.SECONDS);
            if (!sent) {
                LOGGER.log(Level.WARNING, "Notification dispatch returned false for order {0}", order.getId());
            }
        } catch (TimeoutException te) {
            LOGGER.log(Level.WARNING, "Notification for order {0} timed out after 2s, continuing without blocking checkout", order.getId());
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.log(Level.SEVERE, "Notification dispatch failed for order " + order.getId(), e);
        }
    }

    private void sendFulfillmentMessage(Order order) {
        String payload = buildOrderEventJson(order, "FULFILLMENT_REQUESTED");

        JMSProducer producer = jmsContext.createProducer();
        producer.setProperty("orderId", order.getId().longValue());
        producer.setDeliveryMode(jakarta.jms.DeliveryMode.PERSISTENT);
        producer.send(orderProcessingQueue, payload);

        LOGGER.log(Level.FINE, "Order {0} sent to jms/OrderProcessingQueue for fulfillment", order.getId());
    }

    private void publishOrderEvent(Order order, String eventType) {
        String payload = buildOrderEventJson(order, eventType);

        JMSProducer producer = jmsContext.createProducer();
        producer.setProperty("orderId", order.getId().longValue());
        producer.setProperty("eventType", eventType);
        producer.setDeliveryMode(jakarta.jms.DeliveryMode.NON_PERSISTENT);
        producer.send(orderEventsTopic, payload);

        LOGGER.log(Level.FINE, "Order {0} event {1} published to jms/OrderEventsTopic",
                new Object[]{order.getId(), eventType});
    }

    private String buildOrderEventJson(Order order, String eventType) {
        return "{"
                + "\"orderId\":" + order.getId() + ","
                + "\"customerId\":" + order.getCustomer().getId() + ","
                + "\"status\":\"" + order.getStatus().name() + "\","
                + "\"totalAmount\":" + order.getTotalAmount() + ","
                + "\"eventType\":\"" + eventType + "\""
                + "}";
    }
}