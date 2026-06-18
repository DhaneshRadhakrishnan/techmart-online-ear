package lk.jiat.techmart.dto;

import java.io.Serializable;

public class OrderLineRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String sku;
    private int quantity;

    public OrderLineRequestDTO() {
    }

    public OrderLineRequestDTO(String sku, int quantity) {
        this.sku = sku;
        this.quantity = quantity;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}