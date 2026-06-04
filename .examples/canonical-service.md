# Canonical Application Service

The standard pattern for a transactional command service in this project.
Based on `CashOutCommandService`.

```java
@Service
public class CashOutCommandService {

    // 1. Dependencies declared final, injected via constructor
    private final PayeeRepository payeeRepository;
    private final PaymentRepository paymentRepository;
    private final OutboxRepository outboxRepository;
    private final JsonSerializer jsonSerializer;

    public CashOutCommandService(
            PayeeRepository payeeRepository,
            PaymentRepository paymentRepository,
            OutboxRepository outboxRepository,
            JsonSerializer jsonSerializer
    ) {
        this.payeeRepository = payeeRepository;
        this.paymentRepository = paymentRepository;
        this.outboxRepository = outboxRepository;
        this.jsonSerializer = jsonSerializer;
    }

    // 2. @Transactional at method level, not class level
    @Transactional
    public void request(RequestCashOutCommand command) {

        // 3. Load aggregate root, throw immediately if not found
        Payee payee = payeeRepository.findByExternalId(command.payeeExternalId())
                .orElseThrow(() -> new IllegalArgumentException("Payee not found"));

        // 4. Create domain entity via constructor (no setters)
        Payment payment = new Payment(
                command.paymentExternalId(),
                payee,
                PaymentType.CASH_OUT,
                command.amount()
        );
        paymentRepository.save(payment);

        // 5. Build payload as a record
        var payload = new CashOutRequestedPayload(
                payment.getId(),
                payee.getId(),
                command.amount()
        );

        // 6. Use OutboxEventType enum factory to build and save outbox event
        //    Always in the same transaction as the Payment save
        OutboxEvent event = OutboxEventType.CASH_OUT_REQUESTED.toEvent(
                payment.getId(),
                jsonSerializer.serialize(payload)
        );
        outboxRepository.save(event);
    }
}
```

## Rules captured here

- **No `@Autowired` fields** — constructor injection only.
- **`@Transactional` at method level** — not on the class.
- **Payment + OutboxEvent in same transaction** — never save one without the other.
- **`OutboxEventType.toEvent()`** — enum owns the event factory; don't instantiate `OutboxEvent` directly.
- **`JsonSerializer`** for payload serialization — not raw `ObjectMapper`.
- **Throw `IllegalArgumentException`** for "not found" at system boundaries.
