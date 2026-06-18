package lk.jiat.techmart.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

public class CartSummaryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<CartLineDTO> lines;
    private BigDecimal grandTotal;
    private int totalItemCount;

    public CartSummaryDTO() {
    }

    public CartSummaryDTO(List<CartLineDTO> lines, BigDecimal grandTotal, int totalItemCount) {
        this.lines = lines;
        this.grandTotal = grandTotal;
        this.totalItemCount = totalItemCount;
    }

    public List<CartLineDTO> getLines() {
        return lines;
    }

    public void setLines(List<CartLineDTO> lines) {
        this.lines = lines;
    }

    public BigDecimal getGrandTotal() {
        return grandTotal;
    }

    public void setGrandTotal(BigDecimal grandTotal) {
        this.grandTotal = grandTotal;
    }

    public int getTotalItemCount() {
        return totalItemCount;
    }

    public void setTotalItemCount(int totalItemCount) {
        this.totalItemCount = totalItemCount;
    }
}