# Canonical Integration Test

Full annotated example for testing application-layer behavior with real Postgres.

```java
@SpringBootTest                          // loads full Spring context
class CashOutRequestedProcessorIntegrationTest extends AbstractIntegrationTest {
//                                       ^^^ provides Testcontainers Postgres

    @Autowired private CashOutRequestedProcessor processor;
    @Autowired private PayeeRepository payeeRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private BalanceRepository balanceRepository;
    @Autowired private OutboxRepository outboxRepository;
    @Autowired private JsonSerializer jsonSerializer;

    @Test
    @Transactional                       // rolls back after test; remove if verifying cross-tx state
    void shouldDebitBalanceAndEmitConfirmedEventWhenFundsAreSufficient() {
        // given
        Payee payee = new Payee(UUID.randomUUID());
        payeeRepository.save(payee);

        Balance balance = new Balance(payee);
        balance.credit(BigDecimal.valueOf(500.00));
        balanceRepository.save(balance);

        Payment payment = new Payment(
            UUID.randomUUID(), payee, PaymentType.CASH_OUT, BigDecimal.valueOf(100.00)
        );
        paymentRepository.save(payment);

        CashOutRequestedPayload payload =
            new CashOutRequestedPayload(payment.getId(), payee.getId(), BigDecimal.valueOf(100.00));
        OutboxEvent event = OutboxEventType.CASH_OUT_REQUESTED.toEvent(
            payment.getId(), jsonSerializer.serialize(payload)
        );
        outboxRepository.save(event);

        // when
        processor.process(event);

        // then — payment confirmed
        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);

        // then — balance debited
        Balance updatedBalance = balanceRepository.findById(payee.getId()).orElseThrow();
        assertThat(updatedBalance.getAvailableAmount()).isEqualByComparingTo("400.00");

        // then — confirmed outbox event emitted
        List<OutboxEvent> events = outboxRepository.findAll();
        assertThat(events).hasSize(2);  // original REQUESTED + new CONFIRMED
        OutboxEvent confirmedEvent = events.stream()
            .filter(e -> e.getEventType().equals(OutboxEventType.CASH_OUT_CONFIRMED.name()))
            .findFirst().orElseThrow();
        assertThat(confirmedEvent.getAggregateId()).isEqualTo(payment.getId());
    }

    @Test
    @Transactional
    void shouldFailPaymentAndEmitFailedEventWhenInsufficientFunds() {
        // given — zero balance payee + cash-out for 100
        // when — processor.process(event)
        // then — payment FAILED, balance unchanged, CASH_OUT_FAILED event emitted
    }

    @Test
    @Transactional
    void shouldBeIdempotentWhenPaymentIsAlreadyProcessed() {
        // given — payment already in CONFIRMED state
        // when — processor.process(event) called again
        // then — no state change, no additional outbox event
    }
}
```

## Key rules

- Extend `AbstractIntegrationTest` — never configure a container inline.
- `@Transactional` on tests rolls back automatically. Use it by default.
- Remove `@Transactional` only when the test must verify state committed by a different transaction.
- Assert behavior (status, amounts, event types), not implementation details.
