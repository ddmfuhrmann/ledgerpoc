# Skill: Fixtures

## Purpose

Reusable, realistic test data composed without coupling tests to each other.

## Rules

- Name fixtures after their role, not their type: `activePayee`, `pendingCashOut`, not `payee1`.
- Do not share mutable entity instances between tests.
- Fixtures should represent realistic state — set all non-nullable fields.

## Domain fixtures

### Payee

```java
// Active payee — ready to receive payments
Payee payee = new Payee(UUID.randomUUID());
payeeRepository.save(payee);

// Blocked payee — should not process payments
Payee blockedPayee = new Payee(UUID.randomUUID());
blockedPayee.block();
payeeRepository.save(blockedPayee);
```

### Balance

```java
// Zero balance (default on creation)
Balance balance = new Balance(payee);
balanceRepository.save(balance);

// Balance with funds
Balance fundedBalance = new Balance(payee);
fundedBalance.credit(BigDecimal.valueOf(500.00));
balanceRepository.save(fundedBalance);
```

### Payment

```java
// REQUESTED cash-out (entry state for processor)
Payment payment = new Payment(
    UUID.randomUUID(),
    payee,
    PaymentType.CASH_OUT,
    BigDecimal.valueOf(100.00)
);
paymentRepository.save(payment);
```

### OutboxEvent

```java
// CashOutRequested event linked to a payment
CashOutRequestedPayload payload = new CashOutRequestedPayload(
    payment.getId(),
    payee.getId(),
    BigDecimal.valueOf(100.00)
);
OutboxEvent event = OutboxEventType.CASH_OUT_REQUESTED.toEvent(
    payment.getId(),
    jsonSerializer.serialize(payload)
);
outboxRepository.save(event);
```

## Scenario composition

```java
// Full happy-path scenario: payee with funds + pending cash-out event
Payee payee = savePayee();
Balance balance = saveBalanceWithFunds(payee, BigDecimal.valueOf(500.00));
Payment payment = savePendingCashOut(payee, BigDecimal.valueOf(100.00));
OutboxEvent event = saveCashOutRequestedEvent(payment, payee);
```

## Edge case fixtures

- `payeeWithZeroBalance` — for insufficient-funds path
- `payeeWithExactBalance(amount)` — for boundary-value balance check
- `alreadyConfirmedPayment` — for idempotency test
- `alreadyFailedPayment` — for idempotency test
