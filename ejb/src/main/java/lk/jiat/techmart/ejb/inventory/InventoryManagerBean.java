package lk.jiat.techmart.ejb.inventory;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Local;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;
import lk.jiat.techmart.api.InsufficientStockException;
import lk.jiat.techmart.api.InventoryManagerLocal;
import lk.jiat.techmart.dto.InventoryStatusDTO;
import lk.jiat.techmart.entity.InventoryItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Singleton
@Startup
@Local(InventoryManagerLocal.class)
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
public class InventoryManagerBean implements InventoryManagerLocal {

    private static final Logger LOGGER = Logger.getLogger(InventoryManagerBean.class.getName());

    @PersistenceContext(unitName = "TechMartPU", type = PersistenceContextType.TRANSACTION)
    private EntityManager em;

    private final Map<String, InventoryItem> stockCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void initializeCache() {
        List<InventoryItem> items = em.createQuery(
                        "SELECT i FROM InventoryItem i JOIN FETCH i.product", InventoryItem.class)
                .getResultList();
        for (InventoryItem item : items) {
            stockCache.put(item.getProduct().getSku(), item);
        }
        LOGGER.log(Level.INFO, "InventoryManagerBean cache primed with {0} SKUs", stockCache.size());
    }

    @PreDestroy
    public void flushCache() {
        LOGGER.log(Level.INFO, "InventoryManagerBean shutting down, cache held {0} SKUs", stockCache.size());
        stockCache.clear();
    }

    @Override
    @Lock(LockType.READ)
    public InventoryStatusDTO checkStock(String sku) {
        InventoryItem item = stockCache.get(sku);
        if (item == null) {
            return null;
        }
        return toDTO(item);
    }

    @Override
    @Lock(LockType.READ)
    public List<InventoryStatusDTO> checkStockBulk(List<String> skus) {
        List<InventoryStatusDTO> results = new ArrayList<>(skus.size());
        for (String sku : skus) {
            InventoryItem item = stockCache.get(sku);
            if (item != null) {
                results.add(toDTO(item));
            }
        }
        return results;
    }

    @Override
    @Lock(LockType.WRITE)
    public void reserveStock(String sku, int quantity) throws InsufficientStockException {
        InventoryItem item = stockCache.get(sku);
        if (item == null) {
            throw new InsufficientStockException(sku, quantity, 0);
        }
        int available = item.getAvailableQuantity();
        if (available < quantity) {
            throw new InsufficientStockException(sku, quantity, available);
        }
        item.setQuantityReserved(item.getQuantityReserved() + quantity);
        em.merge(item);
    }

    @Override
    @Lock(LockType.WRITE)
    public void releaseReservation(String sku, int quantity) {
        InventoryItem item = stockCache.get(sku);
        if (item == null) {
            return;
        }
        int newReserved = Math.max(0, item.getQuantityReserved() - quantity);
        item.setQuantityReserved(newReserved);
        em.merge(item);
    }

    @Override
    @Lock(LockType.WRITE)
    public void restock(String sku, int quantity) {
        InventoryItem item = stockCache.get(sku);
        if (item == null) {
            return;
        }
        item.setQuantityOnHand(item.getQuantityOnHand() + quantity);
        em.merge(item);
    }

    @Override
    @Lock(LockType.READ)
    public int getCachedSkuCount() {
        return stockCache.size();
    }

    private InventoryStatusDTO toDTO(InventoryItem item) {
        return new InventoryStatusDTO(
                item.getProduct().getSku(),
                item.getWarehouseCode(),
                item.getQuantityOnHand(),
                item.getQuantityReserved(),
                item.getReorderThreshold(),
                item.getLastUpdated()
        );
    }
}