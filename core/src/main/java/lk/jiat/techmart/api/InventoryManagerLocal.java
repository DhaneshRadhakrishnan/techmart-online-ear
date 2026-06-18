package lk.jiat.techmart.api;

import lk.jiat.techmart.dto.InventoryStatusDTO;
import java.util.List;

public interface InventoryManagerLocal {

    InventoryStatusDTO checkStock(String sku);

    List<InventoryStatusDTO> checkStockBulk(List<String> skus);

    void reserveStock(String sku, int quantity) throws InsufficientStockException;

    void releaseReservation(String sku, int quantity);

    void restock(String sku, int quantity);

    int getCachedSkuCount();
}