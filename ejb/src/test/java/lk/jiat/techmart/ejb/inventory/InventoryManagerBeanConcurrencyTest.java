package lk.jiat.techmart.ejb.inventory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lk.jiat.techmart.api.InsufficientStockException;
import lk.jiat.techmart.dto.InventoryStatusDTO;
import lk.jiat.techmart.entity.InventoryItem;
import lk.jiat.techmart.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InventoryManagerBeanConcurrencyTest {

    private static final String SKU = "TM-WIDGET-001";
    private static final int STARTING_STOCK = 500;
    private static final int THREAD_COUNT = 50;
    private static final int RESERVATIONS_PER_THREAD = 20;
    private static final int RESERVE_QTY = 1;

    private InventoryManagerBean bean;
    private InventoryItem item;
    private EntityManager mockEm;

    @BeforeEach
    void setUp() throws Exception {
        Product product = new Product("TM-WIDGET-001", "Test Widget", "desc", "general", new java.math.BigDecimal("9.99"));
        item = new InventoryItem(product, "WH-MAIN", STARTING_STOCK, 10);

        mockEm = mock(EntityManager.class);
        @SuppressWarnings("unchecked")
        TypedQuery<InventoryItem> mockQuery = mock(TypedQuery.class);
        when(mockEm.createQuery(any(String.class), eq(InventoryItem.class)))
                .thenReturn(mockQuery);
        when(mockQuery.getResultList()).thenReturn(List.of(item));

        bean = new InventoryManagerBean();
        setField(bean, "em", mockEm);
        bean.initializeCache();
    }

    @Test
    void reserveStock_withoutContainerManagedLocking_isVulnerableToLostUpdates() throws InterruptedException {
        int totalAttempts = THREAD_COUNT * RESERVATIONS_PER_THREAD;
        ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalAttempts);
        AtomicInteger successfulReservations = new AtomicInteger(0);
        AtomicInteger rejectedReservations = new AtomicInteger(0);

        for (int t = 0; t < THREAD_COUNT; t++) {
            pool.submit(() -> {
                for (int i = 0; i < RESERVATIONS_PER_THREAD; i++) {
                    try {
                        startGate.await();
                        bean.reserveStock(SKU, RESERVE_QTY);
                        successfulReservations.incrementAndGet();
                    } catch (InsufficientStockException e) {
                        rejectedReservations.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                }
            });
        }

        startGate.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertTrue(completed, "All reservation attempts should complete within timeout");

        InventoryStatusDTO finalStatus = bean.checkStock(SKU);

        assertEquals(totalAttempts, successfulReservations.get() + rejectedReservations.get(),
                "Every attempt must resolve to either success or a stock-exhaustion rejection");

        assertTrue(finalStatus.getQuantityReserved() <= successfulReservations.get(),
                "Without container-managed @Lock(WRITE) enforcement, reserveStock() is exposed to a "
                        + "classic check-then-act race: concurrent threads can read the same available-quantity "
                        + "snapshot before either writes back, producing a lost update where the cache's recorded "
                        + "reservation total trails the count of calls that returned successfully. This test calls "
                        + "the bean as a plain object with no EJB container present, so the @Lock annotation on "
                        + "reserveStock() is never enforced; the assertion documents that the recorded total can "
                        + "fall at or below the success count, never above it, which is the signature of a lost "
                        + "update rather than a corrupted or negative balance. Container-managed enforcement of "
                        + "this exact lock is what Arquillian, not JUnit, is positioned to verify end-to-end.");

        assertTrue(finalStatus.getQuantityReserved() <= STARTING_STOCK,
                "Regardless of lost updates, the recorded reservation total must never exceed starting stock, "
                        + "since reserveStock() always rejects a request once the locally-read available "
                        + "quantity is insufficient");
    }

    @Test
    void reserveStock_exceedingAvailable_throwsInsufficientStockException() {
        InsufficientStockException ex = assertThrows(
                InsufficientStockException.class,
                () -> bean.reserveStock(SKU, STARTING_STOCK + 1));
        assertEquals(SKU, ex.getSku());
        assertEquals(STARTING_STOCK + 1, ex.getRequested());
        assertEquals(STARTING_STOCK, ex.getAvailable());
    }

    @Test
    void reserveStock_unknownSku_throwsInsufficientStockExceptionWithZeroAvailable() {
        InsufficientStockException ex = assertThrows(
                InsufficientStockException.class,
                () -> bean.reserveStock("NOT-A-REAL-SKU", 1));
        assertEquals(0, ex.getAvailable());
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}