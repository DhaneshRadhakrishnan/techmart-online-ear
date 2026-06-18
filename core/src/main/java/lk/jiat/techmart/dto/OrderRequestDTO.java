package lk.jiat.techmart.dto;

import java.io.Serializable;
import java.util.List;

public class OrderRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long customerId;
    private List<OrderLineRequestDTO> lines;

    public OrderRequestDTO() {
    }

    public OrderRequestDTO(Long customerId, List<OrderLineRequestDTO> lines) {
        this.customerId = customerId;
        this.lines = lines;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public List<OrderLineRequestDTO> getLines() {
        return lines;
    }

    public void setLines(List<OrderLineRequestDTO> lines) {
        this.lines = lines;
    }
}