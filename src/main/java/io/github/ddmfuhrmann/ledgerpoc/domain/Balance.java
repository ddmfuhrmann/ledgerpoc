package io.github.ddmfuhrmann.ledgerpoc.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "balance")
public class Balance {

    @Id
    @Column(name = "payee_id")
    private Long payeeId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "payee_id")
    private Payee payee;

    @Column(name = "available_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal availableAmount;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Balance() {
        // JPA
    }

    public Balance(Payee payee) {
        this.payee = payee;
        this.payeeId = payee.getId();
        this.availableAmount = BigDecimal.ZERO;
        this.updatedAt = Instant.now();
    }

    // ===== Domain behavior =====

    public void credit(BigDecimal amount) {
        validateAmount(amount);
        this.availableAmount = this.availableAmount.add(amount);
        touch();
    }

    public void debit(BigDecimal amount) {
        validateAmount(amount);
        if (this.availableAmount.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds");
        }
        this.availableAmount = this.availableAmount.subtract(amount);
        touch();
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
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

    public Long getVersion() {
        return version;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}