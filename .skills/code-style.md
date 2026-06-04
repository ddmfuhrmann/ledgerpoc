# Skill: Code Style

## Naming

| Element | Convention | Example |
|---|---|---|
| Classes | PascalCase | `CashOutCommandService`, `OutboxEventType` |
| Methods | camelCase, verb-first | `request()`, `findByExternalId()`, `markPublished()` |
| Constants / enum values | UPPER_SNAKE | `CASH_OUT_REQUESTED`, `PENDING` |
| Database columns | snake_case | `payee_id`, `created_at`, `available_amount` |
| Test methods | `shouldXxxWhenYyy` | `shouldCreatePaymentAndOutboxEventWhenCashOutIsRequested` |

## DTOs

- Commands and payloads are Java `record` types — immutable, no getters to write.
- Commands go in `application/command/`, payloads in `application/event/payload/`.
- Payload records implement `OutboxPayload` marker interface.

## Entities

- Protected no-arg constructor for JPA only (comment: `// JPA`).
- No public setters — all mutation through named domain methods.
- Getters are plain `getX()`, no annotations.
- `@Column` annotations explicitly set `nullable`, `updatable`, `precision/scale` as appropriate.

## Injection

- Constructor injection only. No `@Autowired` fields.
- Constructor is package-private or public — no `@Autowired` annotation needed in Spring Boot.

## Method size

- Target: ≤ 20 lines per method.
- Extract to private methods with descriptive names rather than adding comments.

## Comments

- No comments for obvious code.
- Comments explain WHY, not WHAT (`// Idempotência simples por estado` is acceptable).
- No Javadoc on internal classes. Public API only — and there is none yet.

## Imports

- No wildcard imports.
- Static imports only for test assertions (`import static org.assertj.core.api.Assertions.assertThat`).

## Exceptions

- See `.skills/error-handling.md`.

## Formatting

- Standard IntelliJ/IDE formatting. 4-space indent.
- No trailing whitespace.
