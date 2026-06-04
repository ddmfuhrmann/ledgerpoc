# LedgerPOC — Architecture

This document explains the *why* behind the system's design. It is required reading for any agent or developer touching `Balance`, `LedgerEntry`, or `Payment`.

---

## Overview

LedgerPOC is a payment ledger and balance system built around three core concerns:

1. **Financial correctness** — every debit/credit must be atomic and consistent; no double-spending, no phantom reads.
2. **Concurrency safety** — multiple cash-out requests for the same payee must not race each other.
3. **Horizontal scalability** — the system must support adding more processors without sacrificing correctness.

The system separates *writing* (strongly consistent, serialized) from *reading* (eventually consistent, via a read replica). The ledger is append-only; the `Balance` entity is the strongly consistent decision-making surface.

---

## Main Flows

### Cash-out

```
CommandService.request(command)
  │
  ├─ lookup Payee by externalId
  ├─ new Payment(CASH_OUT, REQUESTED)
  ├─ paymentRepository.save(payment)
  ├─ new CashOutRequestedPayload(paymentId, payeeId, amount)
  ├─ OutboxEventType.CASH_OUT_REQUESTED.toEvent(...)
  └─ outboxRepository.save(event)   ← same @Transactional block

  [outbox poller dispatches event]

CashOutRequestedProcessor.process(event)
  │
  ├─ paymentRepository.findById(aggregateId)
  ├─ if payment.status != REQUESTED → return  (idempotency guard)
  ├─ deserialize CashOutRequestedPayload
  ├─ balanceRepository.findById(payeeId)
  │
  ├─ balance.debit(amount)
  │     └─ throws IllegalStateException("Insufficient funds") if balance < amount
  │
  ├─ [insufficient funds path]
  │     ├─ payment.fail()
  │     └─ outboxRepository.save(CASH_OUT_FAILED event)
  │
  └─ [sufficient funds path]
        ├─ ledgerRepository.save(new LedgerEntry(DEBIT, amount))
        ├─ payment.confirm()
        └─ outboxRepository.save(CASH_OUT_CONFIRMED event)
```

All steps inside `process()` are in a single `@Transactional` block. The debit and the ledger append are atomic.

### Cash-in

The cash-in flow mirrors cash-out in structure:

```
CommandService.request(command)
  ├─ new Payment(CASH_IN, REQUESTED)
  ├─ paymentRepository.save(payment)
  └─ outboxRepository.save(CASH_IN_REQUESTED event)

CashInRequestedProcessor.process(event)
  ├─ idempotency guard
  ├─ balance.credit(amount)       ← no guard clause needed (credits cannot fail)
  ├─ ledgerRepository.save(new LedgerEntry(CREDIT, amount))
  ├─ payment.confirm()
  └─ outboxRepository.save(CASH_IN_CONFIRMED event)
```

---

## Design Decisions

### Why append-only ledger?

`LedgerEntry` records are never updated or deleted. Every financial movement — credit or debit — produces a new row. This gives the system:

- **Auditability** — the full history of every balance change is preserved.
- **Immutability** — once written, a ledger entry cannot be altered by a bug or a concurrent transaction.
- **Correctness on failure** — if a processor crashes mid-flight, replaying the outbox event is safe because the idempotency guard (`payment.status != REQUESTED`) prevents double-writes.

The immutability is enforced in the JPA mapping: all monetary and temporal columns use `updatable = false`.

### Why optimistic locking (`@Version`) on Balance?

`Balance` is the decision-making surface for cash-out: it must not approve a debit if another transaction has already modified it. Without locking, two concurrent processors could both read the same `availableAmount`, both decide "enough funds", and both subtract — producing a negative balance.

`@Version` (JPA optimistic locking) solves this without a `SELECT FOR UPDATE`. When the processor calls `balanceRepository.save(balance)` at transaction commit time, Hibernate checks the version column. If another transaction committed first, the update fails with `OptimisticLockException`, which triggers a retry. No deadlocks; minimal contention.

This means the `balance.debit()` + `ledgerRepository.save()` block inside the processor is protected end-to-end by the version check.

### Why outbox pattern instead of direct calls?

The naive approach would be: debit balance → publish SQS message in the same code path. The problem is that message delivery is not transactional with the database write. If the app crashes after the database commit but before the SQS publish, the event is lost.

The outbox pattern solves this:

1. The `OutboxEvent` row is saved in the **same database transaction** as the `Payment` row. Both commit or both roll back together.
2. A separate poller reads `PENDING` outbox events and publishes them to SQS. If the poller crashes, it simply retries — the rows are still there.
3. Consumers are idempotent, so duplicate delivery is safe.

This gives **at-least-once delivery** with transactional guarantees on the write side.

### Why serialize operations by `payeeId`?

For a given payee, the sequence of balance mutations must be:

```
read balance → apply debit (check funds) → write balance + ledger
```

If two processors handle two cash-out requests for the same payee concurrently, they both read the same balance and both believe there are sufficient funds. The optimistic lock catches this at commit time, but it means one of them will fail and retry.

To avoid this waste, the design rule is: **all outbox events for the same `payeeId` must be processed by a single thread at a time**. In practice this is implemented via consistent hashing or per-payee queue partitioning. The invariant is stated in `CLAUDE.md` and must not be violated by any processor implementation.

### Why separate internal IDs (Long) from external IDs (UUID)?

- **External IDs** (`UUID`, e.g. `Payment.externalId`) are stable across environments, safe to expose in APIs, and do not leak internal state.
- **Internal IDs** (`Long`, BIGSERIAL) are used for foreign key joins and outbox correlation. They are fast to index and compact to store, but they reveal insertion order and row counts — information that should not be visible to callers.

The `OutboxEvent` stores the `aggregateId` (a `Long`) for correlation with the `Payment` row. The processor uses this to look up the payment. External callers receive only the `externalId` (UUID).

---

## Package Structure

```
src/main/java/io/github/ddmfuhrmann/ledgerpoc/
  domain/          ← JPA entities + domain behavior (no external deps)
  application/     ← use cases, command services, processors, outbox events
    command/       ← command record DTOs
    event/         ← OutboxEvent, OutboxEventType, payloads
    support/       ← JsonSerializer
  infra/
    repository/    ← Spring Data JPA interfaces (data access only)
```

Layering rule: `domain` ← `application` ← `infra`. Nothing flows upward. Domain entities do not import Spring or Jackson.
