package lk.jiat.techmart.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class OrderResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long orderId;
    private String status;
    private BigDecimal totalAmount;

    public OrderResultDTO() {
    }

    public OrderResultDTO(Long orderId, String status, BigDecimal totalAmount) {
        this.orderId = orderId;
        this.status = status;
        this.totalAmount = totalAmount;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}