package lk.jiat.techmart.api;

import lk.jiat.techmart.dto.CartSummaryDTO;

public interface ShoppingCartLocal {

    void addItem(String sku, int quantity);

    void updateQuantity(String sku, int quantity);

    void removeItem(String sku);

    CartSummaryDTO viewCart();

    void clearCart();

    CartSummaryDTO checkout() throws EmptyCartException;
}