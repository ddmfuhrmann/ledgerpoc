package io.github.ddmfuhrmann.ledgerpoc.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
public class Payee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, updatable = false)
    private UUID externalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayeeStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Payee() {
        // JPA
    }

    public Payee(UUID externalId) {
        this.externalId = externalId;
        this.status = PayeeStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    // ===== Domain behavior =====

    public void block() {
        if (this.status == PayeeStatus.BLOCKED) {
            return;
        }
        this.status = PayeeStatus.BLOCKED;
        touch();
    }

    public void activate() {
        if (this.status == PayeeStatus.ACTIVE) {
            return;
        }
        this.status = PayeeStatus.ACTIVE;
        touch();
    }

    public boolean isActive() {
        return this.status == PayeeStatus.ACTIVE;
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

    public PayeeStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

}