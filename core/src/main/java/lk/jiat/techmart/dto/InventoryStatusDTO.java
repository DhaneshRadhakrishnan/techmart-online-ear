package lk.jiat.techmart.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class InventoryStatusDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String sku;
    private String warehouseCode;
    private int quantityOnHand;
    private int quantityReserved;
    private int availableQuantity;
    private int reorderThreshold;
    private LocalDateTime lastUpdated;

    public InventoryStatusDTO() {
    }

    public InventoryStatusDTO(String sku, String warehouseCode, int quantityOnHand,
                              int quantityReserved, int reorderThreshold, LocalDateTime lastUpdated) {
        this.sku = sku;
        this.warehouseCode = warehouseCode;
        this.quantityOnHand = quantityOnHand;
        this.quantityReserved = quantityReserved;
        this.availableQuantity = quantityOnHand - quantityReserved;
        this.reorderThreshold = reorderThreshold;
        this.lastUpdated = lastUpdated;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public void setWarehouseCode(String warehouseCode) {
        this.warehouseCode = warehouseCode;
    }

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public void setQuantityOnHand(int quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
    }

    public int getQuantityReserved() {
        return quantityReserved;
    }

    public void setQuantityReserved(int quantityReserved) {
        this.quantityReserved = quantityReserved;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public int getReorderThreshold() {
        return reorderThreshold;
    }

    public void setReorderThreshold(int reorderThreshold) {
        this.reorderThreshold = reorderThreshold;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}