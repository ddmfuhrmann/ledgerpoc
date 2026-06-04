# Skill: Error Handling

## Exception strategy

| Scenario | Exception | Example |
|---|---|---|
| Domain invariant violated | `IllegalStateException` | `Payment.confirm()` when not REQUESTED; `Balance.debit()` with insufficient funds |
| Invalid input / argument | `IllegalArgumentException` | Payee not found by external ID; null/negative amount |
| Serialization failure | `IllegalStateException` (wrapping `JsonProcessingException`) | `JsonSerializer.serialize()` / `deserialize()` |
| Idempotent no-op (not an error) | Silent return | Processor finds payment already beyond REQUESTED |

## Domain behavior

Domain methods enforce their own invariants and throw immediately:

```java
// Payment — invalid state transition
public void confirm() {
    if (this.status != PaymentStatus.REQUESTED) {
        throw new IllegalStateException(
            "Payment cannot be confirmed from status " + status
        );
    }
    ...
}

// Balance — insufficient funds
public void debit(BigDecimal amount) {
    validateAmount(amount);
    if (this.availableAmount.compareTo(amount) < 0) {
        throw new IllegalStateException("Insufficient funds");
    }
    ...
}
```

## Application layer

Command services and processors do not catch domain exceptions — they let them propagate.
The caller (or transaction boundary) is responsible for rollback.

Processors handle business-level failure (e.g. insufficient funds) as a domain outcome,
not an exception: they save a `CASH_OUT_FAILED` outbox event and return normally.

## Infrastructure

`JsonSerializer` wraps Jackson exceptions in `IllegalStateException` with a descriptive message:

```java
throw new IllegalStateException(
    "Failed to serialize outbox event payload: " + payload.getClass().getSimpleName(), e
);
```

## No HTTP mapping yet

There is no controller layer. When REST endpoints are added, map exceptions as follows:

| Exception | HTTP status |
|---|---|
| `IllegalArgumentException` | 400 |
| Entity not found | 404 |
| Conflict / duplicate external ID | 409 |
| `IllegalStateException` (domain) | 422 or 409 depending on context |
| Unhandled | 500 |
