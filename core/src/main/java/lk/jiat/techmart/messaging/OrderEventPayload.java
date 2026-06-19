package lk.jiat.techmart.messaging;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OrderEventPayload implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Pattern NUMERIC_FIELD = Pattern.compile("\"%s\":([^,}]+)");
    private static final Pattern STRING_FIELD = Pattern.compile("\"%s\":\"([^\"]*)\"");

    private final Long orderId;
    private final Long customerId;
    private final String status;
    private final BigDecimal totalAmount;
    private final String eventType;

    private OrderEventPayload(Long orderId, Long customerId, String status,
                              BigDecimal totalAmount, String eventType) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.eventType = eventType;
    }

    public static OrderEventPayload parse(String json) {
        Long orderId = extractLong(json, "orderId");
        Long customerId = extractLong(json, "customerId");
        String status = extractString(json, "status");
        BigDecimal totalAmount = extractDecimal(json, "totalAmount");
        String eventType = extractString(json, "eventType");
        return new OrderEventPayload(orderId, customerId, status, totalAmount, eventType);
    }

    private static Long extractLong(String json, String field) {
        String raw = extractNumericRaw(json, field);
        return raw == null ? null : Long.valueOf(raw);
    }

    private static BigDecimal extractDecimal(String json, String field) {
        String raw = extractNumericRaw(json, field);
        return raw == null ? null : new BigDecimal(raw);
    }

    private static String extractNumericRaw(String json, String field) {
        Pattern pattern = Pattern.compile(String.format(NUMERIC_FIELD.pattern(), field));
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private static String extractString(String json, String field) {
        Pattern pattern = Pattern.compile(String.format(STRING_FIELD.pattern(), field));
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getEventType() {
        return eventType;
    }

    @Override
    public String toString() {
        return "OrderEventPayload{orderId=" + orderId + ", customerId=" + customerId
                + ", status='" + status + "', totalAmount=" + totalAmount
                + ", eventType='" + eventType + "'}";
    }
}