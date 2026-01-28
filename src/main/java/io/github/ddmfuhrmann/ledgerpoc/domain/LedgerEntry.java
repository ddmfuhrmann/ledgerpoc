package io.github.ddmfuhrmann.ledgerpoc.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ledger")
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payee_id", nullable = false)
    private Payee payee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerType type;

    @Column(nullable = false, precision = 18, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LedgerEntry() {
        // JPA
    }

    public LedgerEntry(
            Payee payee,
            Payment payment,
            LedgerType type,
            BigDecimal amount
    ) {
        this.payee = payee;
        this.payment = payment;
        this.type = type;
        this.amount = amount;
        this.createdAt = Instant.now();
    }

    // ===== Read-only access =====

    public Long getId() {
        return id;
    }

    public Payee getPayee() {
        return payee;
    }

    public Payment getPayment() {
        return payment;
    }

    public LedgerType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}