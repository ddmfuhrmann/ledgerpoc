# Skill: Testcontainers

## When to use

- Any test that touches a query, migration, index, constraint, partition, or transaction.
- Any test that involves the outbox table or balance versioning.
- All `application/` and `infra/repository/` tests.

## When NOT to use

- Pure domain logic with no I/O (e.g. testing `Payment.confirm()` state transitions).

## Base class

All integration tests extend `AbstractIntegrationTest`:

```java
// src/test/java/io/github/ddmfuhrmann/ledgerpoc/integration/AbstractIntegrationTest.java
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

Flyway migrations run automatically on startup — no manual schema setup needed.

## Usage pattern

```java
@SpringBootTest
class CashOutRequestedProcessorIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CashOutRequestedProcessor processor;

    @Test
    @Transactional
    void shouldDebitBalanceAndEmitConfirmedEventWhenFundsAreSufficient() {
        // given — set up payee, balance, payment, and outbox event
        // when — processor.process(event)
        // then — assert payment status, balance amount, new outbox event
    }
}
```

## Rules

- Do not mock repositories or services when an integration test is more appropriate.
- Use `@Transactional` on tests that should roll back after each test.
- Do not use `@Transactional` when the test needs to verify committed state across transactions.
- See `.examples/canonical-integration-test.md` for the full annotated example.
