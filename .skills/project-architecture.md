# Skill: Project Architecture

## Package structure

```
src/
  main/java/io/github/ddmfuhrmann/ledgerpoc/
    LedgerpocApplication.java
    domain/
      Payee.java, PayeeStatus.java
      Payment.java, PaymentStatus.java, PaymentType.java
      Balance.java
      BalanceReplica.java
      LedgerEntry.java, LedgerType.java
    application/
      CashOutCommandService.java       ← transactional command handler
      CashOutRequestedProcessor.java   ← outbox event processor
      command/
        RequestCashOutCommand.java     ← record DTO
      event/
        OutboxEvent.java
        OutboxEventType.java           ← enum with toEvent() factory
        OutboxStatus.java, AggregateType.java
        payload/
          OutboxPayload.java           ← marker interface
          CashOut*.java, CashIn*.java  ← record payloads per event type
      support/
        JsonSerializer.java            ← wraps ObjectMapper
    infra/
      repository/
        PayeeRepository.java
        PaymentRepository.java
        BalanceRepository.java
        BalanceReplicaRepository.java
        LedgerRepository.java
        OutboxRepository.java
  resources/
    application.yaml
    db/migration/                      ← Flyway V1..V7

  test/java/io/github/ddmfuhrmann/ledgerpoc/
    integration/
      AbstractIntegrationTest.java     ← shared Testcontainers base
    application/
      *IntegrationTest.java
    infra/repository/
      *IntegrationTest.java, *SmokeTest.java
```

---

## Layering rules

| Layer | Responsibility | May depend on |
|---|---|---|
| `domain/` | JPA entities + domain behavior (state transitions, invariants) | Nothing external |
| `application/` | Use cases, processors, command handlers, outbox events | `domain`, `infra.repository` (injected) |
| `infra/repository/` | Spring Data JPA interfaces, data access only | `domain` (entity types) |

**Hard rules:**
- `domain` classes have zero Spring / framework imports (except JPA annotations).
- `application` classes never touch `EntityManager` directly — they use repositories.
- `infra` classes never contain business logic.
- No controller layer yet — this is a headless POC.

---

## Key conventions

### IDs
- **Internal (`Long`, BIGSERIAL):** used for all FK joins and outbox `aggregate_id`. Never exposed to callers.
- **External (`UUID`):** stable identifiers exposed in commands and payloads.

### Entities
- Protected no-arg constructor for JPA only.
- All state changes through named domain methods (`confirm()`, `fail()`, `credit()`, `debit()`).
- No public setters.
- `Instant` for all timestamps; `BigDecimal(NUMERIC 18,2)` for all monetary amounts.

### Commands and payloads
- Java `record` types (immutable, no boilerplate).
- Commands live in `application/command/`, payloads in `application/event/payload/`.
- Payloads implement `OutboxPayload` marker interface.

### Transactions
- `@Transactional` declared at the service/processor method level, not class level.
- Every write that touches `Payment` also saves an `OutboxEvent` in the same transaction.

### OutboxEventType
- The enum owns the factory: `OutboxEventType.CASH_OUT_REQUESTED.toEvent(aggregateId, payloadJson)`.
- Processors are idempotent: check `payment.getStatus()` before any side effect.

### Balance concurrency
- `Balance` has `@Version` (optimistic lock). A lost-update on balance debit will throw `OptimisticLockException`.
- The payee serialization guarantee (single-threaded per payee) prevents this in practice,
  but the lock exists as a safety net.

---

## Database schema summary

| Table | Purpose | Key constraint |
|---|---|---|
| `payee` | Identity + operational status | `ux_payee_external_id` |
| `payment` | Payment lifecycle | `ux_payment_external_id`, FK → payee |
| `ledger` | Append-only financial movements (partitioned) | RANGE partition on `created_at` |
| `balance` | Current available amount (1:1 with payee) | PK = `payee_id`, `@Version` |
| `balance_replica` | Eventually-consistent read replica | PK = `payee_id`, tracks `last_event_id` |
| `outbox` | Event queue for async processing | `ix_outbox_status_created` |

Ledger is partitioned `PARTITION BY RANGE (created_at)`, monthly partitions. PK is `(id, created_at)`.
