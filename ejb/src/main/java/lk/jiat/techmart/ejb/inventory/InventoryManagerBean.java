package lk.jiat.techmart.ejb.inventory;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
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

@Singleton
@Startup
@DependsOn("DataSeedBean")
@Local(InventoryManagerLocal.class)
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
public class InventoryManagerBean implements InventoryManagerLocal {

    private static final Logger LOGGER = Logger.getLogger(InventoryManagerBean.class.getName());

    @PersistenceContext(unitName = "TechMartPU", type = PersistenceContextType.TRANSACTION)
    private EntityManager em;

    private final Map<String, Long> skuToIdCache = new ConcurrentHashMap<>();
    private final Map<String, InventoryStatusDTO> stockCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void initializeCache() {
        List<InventoryItem> items = em.createQuery(
                        "SELECT i FROM InventoryItem i JOIN FETCH i.product", InventoryItem.class)
                .getResultList();
        for (InventoryItem item : items) {
            String sku = item.getProduct().getSku();
            skuToIdCache.put(sku, item.getId());
            stockCache.put(sku, toDTO(item));
        }
        LOGGER.log(Level.INFO, "InventoryManagerBean cache primed with {0} SKUs", stockCache.size());
    }

    @PreDestroy
    public void flushCache() {
        LOGGER.log(Level.INFO, "InventoryManagerBean shutting down, cache held {0} SKUs", stockCache.size());
        stockCache.clear();
        skuToIdCache.clear();
    }

    @Override
    @Lock(LockType.READ)
    public InventoryStatusDTO checkStock(String sku) {
        return stockCache.get(sku);
    }

    @Override
    @Lock(LockType.READ)
    public List<InventoryStatusDTO> checkStockBulk(List<String> skus) {
        List<InventoryStatusDTO> results = new ArrayList<>(skus.size());
        for (String sku : skus) {
            InventoryStatusDTO dto = stockCache.get(sku);
            if (dto != null) {
                results.add(dto);
            }
        }
        return results;
    }

    @Override
    @Lock(LockType.WRITE)
    public void reserveStock(String sku, int quantity) throws InsufficientStockException {
        Long id = skuToIdCache.get(sku);
        if (id == null) {
            throw new InsufficientStockException(sku, quantity, 0);
        }

        InventoryItem managed = em.find(InventoryItem.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (managed == null) {
            throw new InsufficientStockException(sku, quantity, 0);
        }

        int available = managed.getAvailableQuantity();
        if (available < quantity) {
            throw new InsufficientStockException(sku, quantity, available);
        }

        managed.setQuantityReserved(managed.getQuantityReserved() + quantity);
        stockCache.put(sku, toDTO(managed));
    }

    @Override
    @Lock(LockType.WRITE)
    public void releaseReservation(String sku, int quantity) {
        Long id = skuToIdCache.get(sku);
        if (id == null) {
            return;
        }

        InventoryItem managed = em.find(InventoryItem.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (managed == null) {
            return;
        }

        managed.setQuantityReserved(Math.max(0, managed.getQuantityReserved() - quantity));
        stockCache.put(sku, toDTO(managed));
    }

    @Override
    @Lock(LockType.WRITE)
    public void restock(String sku, int quantity) {
        Long id = skuToIdCache.get(sku);
        if (id == null) {
            return;
        }

        InventoryItem managed = em.find(InventoryItem.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (managed == null) {
            return;
        }

        managed.setQuantityOnHand(managed.getQuantityOnHand() + quantity);
        stockCache.put(sku, toDTO(managed));
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