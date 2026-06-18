package lk.jiat.techmart.ejb.cart;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.EJB;
import jakarta.ejb.Local;
import jakarta.ejb.Remove;
import jakarta.ejb.StatefulTimeout;
import jakarta.ejb.Stateful;
import lk.jiat.techmart.api.EmptyCartException;
import lk.jiat.techmart.api.InventoryManagerLocal;
import lk.jiat.techmart.api.ProductCatalogLocal;
import lk.jiat.techmart.api.ShoppingCartLocal;
import lk.jiat.techmart.dto.CartLineDTO;
import lk.jiat.techmart.dto.CartSummaryDTO;
import lk.jiat.techmart.dto.InventoryStatusDTO;
import lk.jiat.techmart.dto.ProductDTO;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateful
@Local(ShoppingCartLocal.class)
@StatefulTimeout(value = 30, unit = TimeUnit.MINUTES)
public class ShoppingCartBean implements ShoppingCartLocal, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(ShoppingCartBean.class.getName());

    @EJB
    private ProductCatalogLocal productCatalog;

    @EJB
    private InventoryManagerLocal inventoryManager;

    private Map<String, CartLineDTO> lines;
    private LocalDateTime cartOpenedAt;
    private String ownerSessionId;

    @PostConstruct
    public void openCart() {
        this.lines = new LinkedHashMap<>();
        this.cartOpenedAt = LocalDateTime.now();
        LOGGER.log(Level.FINE, "Cart opened at {0}", cartOpenedAt);
    }

    @PreDestroy
    public void onDestroy() {
        if (!lines.isEmpty()) {
            LOGGER.log(Level.INFO,
                    "Abandoned cart at {0}: {1} distinct SKUs, opened at {2}",
                    new Object[]{LocalDateTime.now(), lines.size(), cartOpenedAt});
        }
    }

    public void setOwnerSessionId(String ownerSessionId) {
        this.ownerSessionId = ownerSessionId;
    }

    @Override
    public void addItem(String sku, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        ProductDTO product = productCatalog.findBySku(sku);
        if (product == null) {
            throw new IllegalArgumentException("Unknown SKU: " + sku);
        }
        CartLineDTO existing = lines.get(sku);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
        } else {
            lines.put(sku, new CartLineDTO(sku, product.getName(), quantity, product.getPrice()));
        }
    }

    @Override
    public void updateQuantity(String sku, int quantity) {
        if (quantity <= 0) {
            removeItem(sku);
            return;
        }
        CartLineDTO line = lines.get(sku);
        if (line != null) {
            line.setQuantity(quantity);
        }
    }

    @Override
    public void removeItem(String sku) {
        lines.remove(sku);
    }

    @Override
    public CartSummaryDTO viewCart() {
        return buildSummary();
    }

    @Override
    public void clearCart() {
        lines.clear();
    }

    @Override
    @Remove
    public CartSummaryDTO checkout() throws EmptyCartException {
        if (lines.isEmpty()) {
            throw new EmptyCartException("Cannot checkout an empty cart");
        }
        for (CartLineDTO line : lines.values()) {
            InventoryStatusDTO status = inventoryManager.checkStock(line.getSku());
            if (status == null || status.getAvailableQuantity() < line.getQuantity()) {
                throw new IllegalStateException(
                        "Insufficient stock for " + line.getSku() + " at checkout time");
            }
        }
        CartSummaryDTO finalSummary = buildSummary();
        LOGGER.log(Level.INFO, "Cart checked out: {0} lines, total {1}",
                new Object[]{finalSummary.getLines().size(), finalSummary.getGrandTotal()});
        return finalSummary;
    }

    private CartSummaryDTO buildSummary() {
        BigDecimal grandTotal = BigDecimal.ZERO;
        int itemCount = 0;
        for (CartLineDTO line : lines.values()) {
            grandTotal = grandTotal.add(line.getLineTotal());
            itemCount += line.getQuantity();
        }
        return new CartSummaryDTO(new java.util.ArrayList<>(lines.values()), grandTotal, itemCount);
    }
}