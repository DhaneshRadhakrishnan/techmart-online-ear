package lk.jiat.techmart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log", indexes = {
        @Index(name = "idx_audit_order", columnList = "order_id"),
        @Index(name = "idx_audit_recorded_at", columnList = "recorded_at")
})
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_log_id")
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "detail", length = 500)
    private String detail;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private LocalDateTime recordedAt;

    public AuditLog() {
    }

    public AuditLog(Long orderId, String eventType, String detail) {
        this.orderId = orderId;
        this.eventType = eventType;
        this.detail = detail;
    }

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        this.recordedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getDetail() {
        return detail;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }
}