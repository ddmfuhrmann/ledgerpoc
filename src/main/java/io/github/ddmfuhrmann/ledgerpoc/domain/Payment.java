package io.github.ddmfuhrmann.ledgerpoc.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, updatable = false)
    private UUID externalId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payee_id", nullable = false)
    private Payee payee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false, precision = 18, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Payment() {
        // JPA
    }

    public Payment(UUID externalId, Payee payee, PaymentType type, BigDecimal amount) {
        this.externalId = externalId;
        this.payee = payee;
        this.type = type;
        this.amount = amount;
        this.status = PaymentStatus.REQUESTED;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    // ===== Domain behavior =====

    public void confirm() {
        if (this.status != PaymentStatus.REQUESTED) {
            throw new IllegalStateException(
                    "Payment cannot be confirmed from status " + status
            );
        }
        this.status = PaymentStatus.CONFIRMED;
        touch();
    }

    public void fail() {
        if (this.status == PaymentStatus.CONFIRMED) {
            throw new IllegalStateException("Confirmed payment cannot be failed");
        }
        this.status = PaymentStatus.FAILED;
        touch();
    }

    public void cancel() {
        if (this.status == PaymentStatus.CONFIRMED) {
            throw new IllegalStateException("Confirmed payment cannot be cancelled");
        }
        this.status = PaymentStatus.CANCELLED;
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    // ===== Getters =====

    public Long getId() {
        return id;
    }

    public UUID getExternalId() {
        return externalId;
    }

    public Payee getPayee() {
        return payee;
    }

    public PaymentType getType() {
        return type;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}