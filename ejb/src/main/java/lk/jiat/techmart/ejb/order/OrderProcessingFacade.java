package lk.jiat.techmart.ejb.order;

import jakarta.ejb.EJB;
import jakarta.ejb.Local;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;
import lk.jiat.techmart.api.InsufficientStockException;
import lk.jiat.techmart.api.InventoryManagerLocal;
import lk.jiat.techmart.api.OrderProcessingLocal;
import lk.jiat.techmart.dto.OrderLineRequestDTO;
import lk.jiat.techmart.dto.OrderRequestDTO;
import lk.jiat.techmart.dto.OrderResultDTO;
import lk.jiat.techmart.entity.Customer;
import lk.jiat.techmart.entity.Order;
import lk.jiat.techmart.entity.OrderItem;
import lk.jiat.techmart.entity.Product;
import java.math.BigDecimal;
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

        LOGGER.log(Level.INFO, "Order {0} placed for customer {1}, total {2}",
                new Object[]{order.getId(), customer.getId(), total});

        return new OrderResultDTO(order.getId(), order.getStatus().name(), total);
    }

    private void dispatchOrderEvents(Order order) {
        LOGGER.log(Level.FINE,
                "Order {0} persisted — queue send and topic publish wired in Day 3 (JMS)",
                order.getId());
    }



}