package lk.jiat.techmart.ejb.order;

import jakarta.ejb.EJB;
import lk.jiat.techmart.api.InsufficientStockException;
import lk.jiat.techmart.api.InventoryManagerLocal;
import lk.jiat.techmart.api.OrderProcessingLocal;
import lk.jiat.techmart.dto.InventoryStatusDTO;
import lk.jiat.techmart.dto.OrderLineRequestDTO;
import lk.jiat.techmart.dto.OrderRequestDTO;
import lk.jiat.techmart.dto.OrderResultDTO;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ArquillianExtension.class)
class OrderProcessingFacadeIT {

    private static final Long SEEDED_CUSTOMER_ID = 1L;
    private static final String SEEDED_SKU = "SKU-PERIPH-005";

    @EJB
    private OrderProcessingLocal orderProcessing;

    @EJB
    private InventoryManagerLocal inventoryManager;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "techmart-order-it.jar")
                .addPackages(true,
                        "lk.jiat.techmart.entity",
                        "lk.jiat.techmart.dto",
                        "lk.jiat.techmart.api",
                        "lk.jiat.techmart.messaging",
                        "lk.jiat.techmart.ejb")
                .addAsManifestResource("META-INF/persistence.xml", "persistence.xml")
                .addAsManifestResource(
                        new ByteArrayInputStream("<beans/>".getBytes()).toString(),
                        "beans.xml");
    }

    @Test
    void placeOrder_withSeededCustomerAndProduct_reservesStockAndPersistsConfirmedOrder() {
        InventoryStatusDTO beforeStatus = inventoryManager.checkStock(SEEDED_SKU);
        assertNotNull(beforeStatus, "Seeded SKU must exist in inventory before placing an order");
        int availableBefore = beforeStatus.getAvailableQuantity();
        assertTrue(availableBefore > 0, "Seeded inventory must have available stock to run this test");

        OrderRequestDTO request = new OrderRequestDTO(
                SEEDED_CUSTOMER_ID,
                List.of(new OrderLineRequestDTO(SEEDED_SKU, 1)));

        OrderResultDTO result = orderProcessing.placeOrder(request);

        assertNotNull(result.getOrderId(), "A persisted order must be assigned a generated ID");
        assertEquals("CONFIRMED", result.getStatus());
        assertTrue(result.getTotalAmount().compareTo(BigDecimal.ZERO) > 0,
                "Order total must reflect the seeded product price");

        InventoryStatusDTO afterStatus = inventoryManager.checkStock(SEEDED_SKU);
        assertEquals(availableBefore - 1, afterStatus.getAvailableQuantity(),
                "Available quantity must decrease by exactly the reserved amount after a confirmed order");
    }

    @Test
    void placeOrder_requestingMoreThanAvailable_throwsInsufficientStockExceptionAndReservesNothing() {
        InventoryStatusDTO beforeStatus = inventoryManager.checkStock(SEEDED_SKU);
        int impossibleQuantity = beforeStatus.getAvailableQuantity() + 1000;

        OrderRequestDTO request = new OrderRequestDTO(
                SEEDED_CUSTOMER_ID,
                List.of(new OrderLineRequestDTO(SEEDED_SKU, impossibleQuantity)));

        assertThrows(InsufficientStockException.class, () -> orderProcessing.placeOrder(request));

        InventoryStatusDTO afterStatus = inventoryManager.checkStock(SEEDED_SKU);
        assertEquals(beforeStatus.getAvailableQuantity(), afterStatus.getAvailableQuantity(),
                "A rejected order must leave inventory unchanged, with no partial reservation");
    }

    @Test
    void placeOrder_unknownCustomerId_throwsIllegalArgumentException() {
        OrderRequestDTO request = new OrderRequestDTO(
                999999L,
                List.of(new OrderLineRequestDTO(SEEDED_SKU, 1)));

        assertThrows(IllegalArgumentException.class, () -> orderProcessing.placeOrder(request));
    }
}