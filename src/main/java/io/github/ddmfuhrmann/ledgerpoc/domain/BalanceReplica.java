package io.github.ddmfuhrmann.ledgerpoc.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "balance_replica")
public class BalanceReplica {

    @Id
    @Column(name = "payee_id")
    private Long payeeId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "payee_id")
    private Payee payee;

    @Column(name = "available_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal availableAmount;

    @Column(name = "last_event_id", nullable = false)
    private UUID lastEventId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BalanceReplica() {
        // JPA
    }

    public BalanceReplica(Payee payee) {
        this.payee = payee;
        this.payeeId = payee.getId();
        this.availableAmount = BigDecimal.ZERO;
        this.updatedAt = Instant.now();
    }

    // ===== Event-driven update =====

    public void applyEvent(UUID eventId, BigDecimal newAmount) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event id must not be null");
        }
        this.availableAmount = newAmount;
        this.lastEventId = eventId;
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    // ===== Getters =====

    public Long getPayeeId() {
        return payeeId;
    }

    public BigDecimal getAvailableAmount() {
        return availableAmount;
    }

    public UUID getLastEventId() {
        return lastEventId;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}