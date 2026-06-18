package lk.jiat.techmart.api;

import lk.jiat.techmart.dto.OrderRequestDTO;
import lk.jiat.techmart.dto.OrderResultDTO;

public interface OrderProcessingLocal {

    OrderResultDTO placeOrder(OrderRequestDTO request) throws InsufficientStockException;
}