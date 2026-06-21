package java.lk.jiat.techmart.ejb.cart;

import lk.jiat.techmart.api.EmptyCartException;
import lk.jiat.techmart.api.InventoryManagerLocal;
import lk.jiat.techmart.api.ProductCatalogLocal;
import lk.jiat.techmart.dto.CartSummaryDTO;
import lk.jiat.techmart.dto.InventoryStatusDTO;
import lk.jiat.techmart.dto.ProductDTO;
import lk.jiat.techmart.ejb.cart.ShoppingCartBean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShoppingCartBeanLifecycleTest {

    private static final String SKU_A = "TM-ALPHA-001";
    private static final String SKU_B = "TM-BETA-002";

    private ShoppingCartBean cart;
    private ProductCatalogLocal mockCatalog;
    private InventoryManagerLocal mockInventory;

    @BeforeEach
    void setUp() throws Exception {
        mockCatalog = mock(ProductCatalogLocal.class);
        mockInventory = mock(InventoryManagerLocal.class);

        when(mockCatalog.findBySku(SKU_A)).thenReturn(
                new ProductDTO(1L, SKU_A, "Alpha Widget", "desc", "general", new BigDecimal("19.99"), "ACTIVE", LocalDateTime.now()));
        when(mockCatalog.findBySku(SKU_B)).thenReturn(
                new ProductDTO(2L, SKU_B, "Beta Widget", "desc", "general", new BigDecimal("9.50"), "ACTIVE", LocalDateTime.now()));
        when(mockCatalog.findBySku("UNKNOWN-SKU")).thenReturn(null);

        cart = new ShoppingCartBean();
        setField(cart, "productCatalog", mockCatalog);
        setField(cart, "inventoryManager", mockInventory);
        cart.openCart();
    }

    @AfterEach
    void tearDown() {
        cart.onDestroy();
    }

    @Test
    void openCart_initializesEmptyCart() {
        CartSummaryDTO summary = cart.viewCart();
        assertEquals(0, summary.getTotalItemCount());
        assertEquals(BigDecimal.ZERO, summary.getGrandTotal());
        assertTrue(summary.getLines().isEmpty());
    }

    @Test
    void addItem_singleLine_reflectsInCartSummary() {
        cart.addItem(SKU_A, 2);
        CartSummaryDTO summary = cart.viewCart();
        assertEquals(1, summary.getLines().size());
        assertEquals(2, summary.getTotalItemCount());
        assertEquals(new BigDecimal("39.98"), summary.getGrandTotal());
    }

    @Test
    void addItem_sameSkuTwice_accumulatesQuantity() {
        cart.addItem(SKU_A, 2);
        cart.addItem(SKU_A, 3);
        CartSummaryDTO summary = cart.viewCart();
        assertEquals(1, summary.getLines().size());
        assertEquals(5, summary.getTotalItemCount());
    }

    @Test
    void addItem_zeroOrNegativeQuantity_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> cart.addItem(SKU_A, 0));
        assertThrows(IllegalArgumentException.class, () -> cart.addItem(SKU_A, -1));
    }

    @Test
    void addItem_unknownSku_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> cart.addItem("UNKNOWN-SKU", 1));
    }

    @Test
    void updateQuantity_toZero_removesLine() {
        cart.addItem(SKU_A, 2);
        cart.updateQuantity(SKU_A, 0);
        assertTrue(cart.viewCart().getLines().isEmpty());
    }

    @Test
    void updateQuantity_toPositive_changesLineQuantity() {
        cart.addItem(SKU_A, 2);
        cart.updateQuantity(SKU_A, 7);
        CartSummaryDTO summary = cart.viewCart();
        assertEquals(7, summary.getLines().get(0).getQuantity());
    }

    @Test
    void removeItem_removesOnlyTargetedSku() {
        cart.addItem(SKU_A, 1);
        cart.addItem(SKU_B, 1);
        cart.removeItem(SKU_A);
        CartSummaryDTO summary = cart.viewCart();
        assertEquals(1, summary.getLines().size());
        assertEquals(SKU_B, summary.getLines().get(0).getSku());
    }

    @Test
    void clearCart_emptiesAllLines() {
        cart.addItem(SKU_A, 1);
        cart.addItem(SKU_B, 1);
        cart.clearCart();
        assertTrue(cart.viewCart().getLines().isEmpty());
    }

    @Test
    void checkout_emptyCart_throwsEmptyCartException() {
        assertThrows(EmptyCartException.class, () -> cart.checkout());
    }

    @Test
    void checkout_sufficientStock_returnsSummaryAndSucceeds() {
        cart.addItem(SKU_A, 2);
        when(mockInventory.checkStock(SKU_A)).thenReturn(
                new InventoryStatusDTO(SKU_A, "WH-MAIN", 50, 0, 5, LocalDateTime.now()));

        CartSummaryDTO summary = cart.checkout();
        assertEquals(1, summary.getLines().size());
        assertEquals(new BigDecimal("39.98"), summary.getGrandTotal());
    }

    @Test
    void checkout_insufficientStock_throwsIllegalStateException() {
        cart.addItem(SKU_A, 10);
        when(mockInventory.checkStock(SKU_A)).thenReturn(
                new InventoryStatusDTO(SKU_A, "WH-MAIN", 5, 0, 1, LocalDateTime.now()));

        assertThrows(IllegalStateException.class, () -> cart.checkout());
    }

    @Test
    void checkout_skuMissingFromInventory_throwsIllegalStateException() {
        cart.addItem(SKU_A, 1);
        when(mockInventory.checkStock(SKU_A)).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> cart.checkout());
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}