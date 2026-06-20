package lk.jiat.techmart.ejb.seed;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.DependsOn;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;
import lk.jiat.techmart.entity.Customer;
import lk.jiat.techmart.entity.InventoryItem;
import lk.jiat.techmart.entity.Product;

import java.math.BigDecimal;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
public class DataSeedBean {

    private static final Logger LOGGER = Logger.getLogger(DataSeedBean.class.getName());

    @PersistenceContext(unitName = "TechMartPU", type = PersistenceContextType.TRANSACTION)
    private EntityManager em;

    @PostConstruct
    public void seedIfEmpty() {
        Long existingProducts = em.createQuery("SELECT COUNT(p) FROM Product p", Long.class)
                .getSingleResult();

        if (existingProducts != null && existingProducts > 0) {
            LOGGER.log(Level.INFO, "DataSeedBean skipping seed, {0} products already present", existingProducts);
            return;
        }

        LOGGER.log(Level.INFO, "DataSeedBean seeding initial catalog, customer, and inventory data");

        Product laptop = new Product("SKU-LAPTOP-001", "TechMart UltraBook 14",
                "14-inch ultrabook, 16GB RAM, 512GB SSD", "Electronics", new BigDecimal("899.99"));
        Product headphones = new Product("SKU-AUDIO-002", "TechMart NoiseCancel Pro",
                "Over-ear wireless noise-cancelling headphones", "Electronics", new BigDecimal("149.50"));
        Product deskChair = new Product("SKU-FURN-003", "TechMart ErgoSeat",
                "Ergonomic mesh-back office chair with lumbar support", "Furniture", new BigDecimal("219.00"));
        Product monitor = new Product("SKU-DISPLAY-004", "TechMart ClearView 27\"",
                "27-inch QHD IPS monitor, 144Hz", "Electronics", new BigDecimal("329.00"));
        Product keyboard = new Product("SKU-PERIPH-005", "TechMart MechType",
                "Mechanical keyboard, hot-swappable switches", "Electronics", new BigDecimal("89.99"));

        em.persist(laptop);
        em.persist(headphones);
        em.persist(deskChair);
        em.persist(monitor);
        em.persist(keyboard);

        Customer customer = new Customer(
                "test.customer@techmart.example",
                "Test",
                "Customer",
                "+94 77 000 0000",
                "12 Galle Road, Negombo, Western Province, LK"
        );
        em.persist(customer);

        em.flush();

        em.persist(new InventoryItem(laptop, "WH-COLOMBO-01", 50, 10));
        em.persist(new InventoryItem(headphones, "WH-COLOMBO-01", 120, 20));
        em.persist(new InventoryItem(deskChair, "WH-KANDY-01", 30, 5));
        em.persist(new InventoryItem(monitor, "WH-COLOMBO-01", 75, 15));
        em.persist(new InventoryItem(keyboard, "WH-COLOMBO-01", 200, 30));

        LOGGER.log(Level.INFO, "DataSeedBean seed complete: 5 products, 1 customer, 5 inventory rows");
    }
}