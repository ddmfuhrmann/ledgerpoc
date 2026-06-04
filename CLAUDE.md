# LedgerPOC — CLAUDE.md

Payment ledger and balance system POC focused on financial correctness, concurrency, and horizontal scalability.
Cash-in and cash-out flows use an append-only ledger, a strongly consistent balance for decision-making,
and an event-driven read replica. The system explores async processing, backpressure, and HPA under burst traffic.

---

## Workflow

```
/plan → /grill-me → /revise-plan → /implement → /test → /review → [/optimize] → /handoff
```

- No code before a revised plan exists.
- No optimization without a measured baseline.
- No scope expansion without an explicit note in the plan.
- Reviewer always diffs against the **revised** plan, not the original.

---

## Stack

- **Language:** Java 21
- **Framework:** Spring Boot 3.5 / Spring Data JPA
- **Database:** PostgreSQL 18 (partitioned ledger, JSONB outbox)
- **Migrations:** Flyway (`src/main/resources/db/migration/`)
- **Test framework:** JUnit 5 + AssertJ + Testcontainers
- **Container tooling:** Docker Compose (`docker-compose.yml`)
- **Async / messaging:** Amazon SQS / LocalStack (planned)
- **Build:** Maven (`mvn`)

---

## Architecture

See `.skills/project-architecture.md` for full rules.

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

**Layering rule:** `domain` ← `application` ← `infra`. Nothing flows upward.
Controllers do not exist yet — this is a headless POC.

---

## Critical Domain Constraints

These are non-negotiable invariants. Any plan that touches balance, ledger, or payment
must address all that apply.

### Financial correctness
- `Balance` uses `@Version` (optimistic locking). Cash-out debit and ledger append are always in the same `@Transactional` block.
- `LedgerEntry` is append-only and immutable — no updates, no deletes.
- `Balance.debit()` throws `IllegalStateException` on insufficient funds — it must not be bypassed.

### Payee serialization
- All operations for the same `payeeId` must be processed sequentially (single-threaded per payee).
- Operations for different payees may run in parallel.
- Balance validation + ledger append must be atomic within the same serialized execution.

### Outbox pattern
- Every state-changing operation saves a `Payment` and an `OutboxEvent` in the same transaction.
- Processors read outbox events and are idempotent — they check `payment.getStatus()` before acting.
- `OutboxEvent` status transitions: `PENDING` → `PUBLISHED` | `FAILED`.

### Identifiers
- External IDs (`UUID`) — stable, safe to expose to callers.
- Internal IDs (`Long`, BIGSERIAL) — used for FK joins and outbox correlation. Never leak externally.

---

## Agents

| Agent | File | Role |
|---|---|---|
| Feature Implementer | `.agents/feature-implementer.md` | Executes revised plan, no silent scope expansion |
| Test Implementer | `.agents/test-implementer.md` | Adds/improves tests, picks test type |
| Reviewer | `.agents/reviewer.md` | Diffs output against revised plan + guidelines |
| Optimizer | `.agents/optimizer.md` | Evidence-based performance work only |

---

## Skills

| Skill | Purpose |
|---|---|
| `.skills/project-architecture.md` | Package structure, layering rules, conventions |
| `.skills/code-style.md` | Naming, method size, comments, DTO patterns |
| `.skills/error-handling.md` | Exception strategy, domain invariants |
| `.skills/plan-first-development.md` | Plan workflow rules and checklist |
| `.skills/grill-me.md` | How to challenge a plan effectively |
| `.skills/diff-review.md` | Review process and severity labeling |
| `.skills/testcontainers.md` | Integration test setup with Postgres |
| `.skills/fixtures.md` | Test fixture patterns for this domain |
| `.skills/testing-strategy.md` | Which test type to use and when |
| `.skills/edge-case-generation.md` | Systematic edge case discovery |
| `.skills/notion-docs.md` | Templates e procedure para salvar planos, ADRs, PRDs e handoffs no Notion |
| `.skills/local-env-orchestration.md` | Starting local environment |
| `.skills/observability-setup.md` | Configure logs/metrics before performance investigation |
| `.skills/messaging-analysis.md` | Outbox throughput, processing latency, SQS consumer lag |
| `.skills/database-seeding.md` | Seeding realistic data for perf tests |
| `.skills/postgres-explain-analyze.md` | Query analysis procedure |
| `.skills/benchmark-execution.md` | Load testing methodology |
| `.skills/optimization-reporting.md` | Optimization report format |
