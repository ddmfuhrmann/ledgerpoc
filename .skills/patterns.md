# Canonical Code Patterns — LedgerPOC

Snippets extracted from the live codebase. Copy these patterns exactly — do not invent variations.
Update via `/f-sync-patterns` after adding a new canonical pattern.

---

## 1. JPA Entity

**Rules:**
- Protected no-arg constructor (JPA only, comment says `// JPA`)
- Public constructor sets all required fields and sensible defaults
- Sections: `// ===== Domain behavior =====` and `// ===== Getters =====`
- FetchType.LAZY on all associations
- `BigDecimal` with `precision = 18, scale = 2`
- Timestamps as `Instant`
- No setters — state mutated only via domain methods
- `@Version` on mutable aggregates that need optimistic locking (Balance)
- Immutable fields use `updatable = false`

**Example — immutable append-only entity (LedgerEntry):**

```java
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

    public LedgerEntry(Payee payee, Payment payment, LedgerType type, BigDecimal amount) {
        this.payee = payee;
        this.payment = payment;
        this.type = type;
        this.amount = amount;
        this.createdAt = Instant.now();
    }

    // ===== Read-only access =====

    public Long getId() { return id; }
    public Payee getPayee() { return payee; }
    public Payment getPayment() { return payment; }
    public LedgerType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}
```

**Example — mutable aggregate with optimistic locking (Balance):**

```java
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

    public Long getPayeeId() { return payeeId; }
    public BigDecimal getAvailableAmount() { return availableAmount; }
    public Long getVersion() { return version; }
    public Instant getUpdatedAt() { return updatedAt; }
}
```

---

## 2. Domain Behavior

**Rules:**
- State transitions guard with `if (status != EXPECTED) throw new IllegalStateException(...)`
- Use descriptive messages: `"Payment cannot be confirmed from status " + status`
- Business invariants throw `IllegalStateException` (recoverable domain errors)
- Invalid input throws `IllegalArgumentException` (caller bug, not domain failure)
- Every mutating method calls `touch()` to update `updatedAt`
- `validateAmount()` is private, called at the start of every financial operation

**Example — state transitions (Payment):**

```java
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

private void touch() {
    this.updatedAt = Instant.now();
}
```

---

## 3. CommandService

**Rules:**
- `@Service` + constructor injection (no `@Autowired` on fields)
- Single `@Transactional` public method per command
- Pattern: lookup → new domain object → save → build payload → `OutboxEventType.X.toEvent(...)` → save outbox
- Never call `balanceRepository` or `ledgerRepository` from CommandService — that's the Processor's job
- `JsonSerializer` for payload serialization

**Example (CashOutCommandService):**

```java
@Service
public class CashOutCommandService {

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

    @Transactional
    public void request(RequestCashOutCommand command) {
        Payee payee = payeeRepository.findByExternalId(command.payeeExternalId())
                .orElseThrow(() -> new IllegalArgumentException("Payee not found"));

        Payment payment = new Payment(
                command.paymentExternalId(),
                payee,
                PaymentType.CASH_OUT,
                command.amount()
        );

        paymentRepository.save(payment);

        var payload = new CashOutRequestedPayload(
                payment.getId(),
                payee.getId(),
                command.amount()
        );

        OutboxEvent event = OutboxEventType.CASH_OUT_REQUESTED.toEvent(
                payment.getId(),
                jsonSerializer.serialize(payload)
        );

        outboxRepository.save(event);
    }
}
```

---

## 4. Processor

**Rules:**
- `@Component` (not `@Service`) + constructor injection
- `@Transactional` on the public `process(OutboxEvent event)` method
- Idempotency first: `if (payment.getStatus() != PaymentStatus.REQUESTED) return;`
- Deserialize payload via `jsonSerializer.deserialize(...)`
- Delegate actual work to private methods (`applyDebit`, `confirmCashOut`, `failCashOut`)
- Catch only `IllegalStateException` for domain failures (insufficient funds); let `IllegalArgumentException` propagate (upstream bug)
- Save outbox event for each outcome (confirmed or failed)

**Example (CashOutRequestedProcessor):**

```java
@Component
public class CashOutRequestedProcessor {

    // ... constructor injection ...

    @Transactional
    public void process(OutboxEvent event) {
        Payment payment = paymentRepository.findById(event.getAggregateId())
                .orElseThrow(() -> new IllegalStateException("Payment not found"));

        // Idempotência simples por estado
        if (payment.getStatus() != PaymentStatus.REQUESTED) {
            return;
        }

        CashOutRequestedPayload payload =
                jsonSerializer.deserialize(event.getPayload(), CashOutRequestedPayload.class);

        Balance balance = balanceRepository.findById(payload.payeeId())
                .orElseThrow(() -> new IllegalStateException("Balance not found"));

        applyDebit(payment, balance, payload);
    }

    private void applyDebit(Payment payment, Balance balance, CashOutRequestedPayload payload) {
        try {
            balance.debit(payload.amount());
        } catch (IllegalStateException e) {
            failCashOut(payment, payload);
            return;
        }
        confirmCashOut(payment, payload);
    }

    private void failCashOut(Payment payment, CashOutRequestedPayload payload) {
        payment.fail();
        outboxRepository.save(
                OutboxEventType.CASH_OUT_FAILED.toEvent(
                        payment.getId(),
                        jsonSerializer.serialize(new CashOutFailedPayload(
                                payload.paymentId(), payload.payeeId(),
                                payload.amount(), "INSUFFICIENT_FUNDS"
                        ))
                )
        );
    }

    private void confirmCashOut(Payment payment, CashOutRequestedPayload payload) {
        ledgerRepository.save(
                new LedgerEntry(payment.getPayee(), payment, LedgerType.DEBIT, payload.amount())
        );
        payment.confirm();
        outboxRepository.save(
                OutboxEventType.CASH_OUT_CONFIRMED.toEvent(
                        payment.getId(),
                        jsonSerializer.serialize(new CashOutConfirmedPayload(
                                payload.paymentId(), payload.payeeId(), payload.amount()
                        ))
                )
        );
    }
}
```

---

## 5. Integration Test

**Rules:**
- Extend `AbstractIntegrationTest` (Testcontainers Postgres, `@DynamicPropertySource`)
- `@SpringBootTest` + `@Transactional` on each test method (auto-rollback)
- Structure: `// given` → `// when` → `// then`
- Fixture helpers are private methods at the bottom, after `// Fixture helpers` comment separator
- Test method naming: `shouldDoXWhenY` (verb-first, camelCase)
- Use `assertThat(...).isEqualByComparingTo("100.00")` for BigDecimal comparisons
- `assertThatThrownBy(...)` for exception assertions

**Example:**

```java
@SpringBootTest
class CashOutRequestedProcessorIntegrationTest extends AbstractIntegrationTest {

    @Autowired private CashOutRequestedProcessor processor;
    @Autowired private PayeeRepository payeeRepository;
    @Autowired private BalanceRepository balanceRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private LedgerRepository ledgerRepository;
    @Autowired private OutboxRepository outboxRepository;
    @Autowired private JsonSerializer jsonSerializer;

    @Test
    @Transactional
    void shouldConfirmPaymentAndDebitBalanceAndCreateLedgerEntryWhenFundsAreSufficient() {
        // given
        Payee payee = new Payee(UUID.randomUUID());
        payeeRepository.save(payee);

        Balance balance = new Balance(payee);
        balance.credit(BigDecimal.valueOf(100.00));
        balanceRepository.save(balance);

        Payment payment = new Payment(UUID.randomUUID(), payee, PaymentType.CASH_OUT, BigDecimal.valueOf(100.00));
        paymentRepository.save(payment);

        OutboxEvent triggerEvent = buildCashOutRequestedEvent(payment, payee, BigDecimal.valueOf(100.00));
        outboxRepository.save(triggerEvent);

        // when
        processor.process(triggerEvent);

        // then
        Payment savedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);

        Balance savedBalance = balanceRepository.findById(payee.getId()).orElseThrow();
        assertThat(savedBalance.getAvailableAmount()).isEqualByComparingTo("0.00");
    }

    // -------------------------------------------------------------------------
    // Fixture helpers
    // -------------------------------------------------------------------------

    private OutboxEvent buildCashOutRequestedEvent(Payment payment, Payee payee, BigDecimal amount) {
        CashOutRequestedPayload payload = new CashOutRequestedPayload(
                payment.getId(), payee.getId(), amount
        );
        return OutboxEventType.CASH_OUT_REQUESTED.toEvent(
                payment.getId(), jsonSerializer.serialize(payload)
        );
    }
}
```

**AbstractIntegrationTest (do not modify):**

```java
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres")
                    .withDatabaseName("ledgerpoc")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```
