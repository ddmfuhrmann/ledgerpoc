# Overview
This project is a technical POC for a payment ledger and balance system, focused on financial correctness, 
concurrency, and scalability. It models cash-in and cash-out flows using an append-only ledger, a strongly consistent balance for 
decision-making, and event-driven replicas for read optimization. The main goal is to explore how real payment systems behave under load, including asynchronous 
processing, backpressure, and horizontal scaling.

---

# Tech Stack
- Java 21
- Spring Boot / Spring Data JPA
- PostgreSQL
- Flyway (schema migrations)
- Testcontainers

### Cloud & Runtime
- Docker
- Kubernetes (HPA-ready, horizontal scaling experiments)
- Amazon SQS / LocalStack (async processing & retries)

### Observability (planned)
- Metrics, logs, and tracing to support scaling and backlog-based autoscaling

---

# Context

## Payments
Responsible for processing monetary movements (cash-in and cash-out),
maintaining financial consistency, and exposing current balances.
It does not handle customer data, fraud, fees, or reconciliation.

## Technical Scope
This POC also aims to evaluate event-driven horizontal scaling (HPA)
based on backlog metrics, as well as observability under burst traffic
scenarios.

---

# Entities

## Payee

Represents the owner of the balance and defines the isolation boundary for
payment processing. It holds stable identity and operational status, but does
not store balance or transaction history.

## Payment

Represents the lifecycle of a payment process. It tracks the technical state of
cash-in and cash-out operations and correlates asynchronous events, without
representing financial movements directly.

### Payment States
* **REQUESTED:** The payment request was accepted by the system and persisted, but processing
  has not started yet.
* **IN_PROGRESS:** The payment is being processed asynchronously and has been sent to the external
  payment rail or downstream system.
* **CONFIRMED:** The payment was successfully completed and the financial movement was
  persisted in the ledger.
* **FAILED:** The payment could not be completed due to rejection, timeout, or external
  failure, and no financial movement was recorded.

## Ledger

Represents the source of truth for confirmed financial movements. It is
append-only, immutable, and used for auditability and financial consistency.

## Balance

Represents the current available balance for a payee. It is a strong, 1:1
snapshot used for fast validation of cash-out operations and can be rebuilt
from the ledger.

---

# Domain Events
Domain events represent business facts and are emitted only when a meaningful
state transition occurs in the payment lifecycle.

### CashInRequested
Indicates that a cash-in operation was requested and accepted by the system for
asynchronous processing.

### CashInConfirmed
Indicates that a cash-in operation was successfully completed and the
corresponding financial movement was recorded in the ledger.

### CashInFailed
Indicates that a cash-in operation failed or was rejected, and no financial
movement was recorded.

### CashOutRequested
Indicates that a cash-out operation was requested and accepted by the system for
asynchronous processing.

### CashOutConfirmed
Indicates that a cash-out operation was successfully completed and the
corresponding financial movement was recorded in the ledger.

### CashOutFailed
Indicates that a cash-out operation failed or was rejected, and no financial
movement was recorded.

---

# Service Level Objectives (SLA)
SLAs apply to asynchronous processing time rather than synchronous request
latency, and are measured from request acceptance to terminal state.

## CashIn Processing SLA
Cash-in requests must be processed and reach a terminal state
(CONFIRMED or FAILED) within a defined maximum processing time under normal
operating conditions.

## CashOut Processing SLA
Cash-out requests must be processed and reach a terminal state
(CONFIRMED or FAILED) within a defined maximum processing time, ensuring
financial consistency and balance validation.

## Balance Read SLA
Balance queries must return the most recent available balance with low latency,
accepting eventual consistency for read replicas.

---

# Serialization Rules
Serialization is applied at the payee level only. The system does not attempt
to parallelize operations that compete for the same balance.

## Single-Threaded Processing per Payee
All payment operations (cash-in and cash-out) for the same payee must be
processed sequentially to guarantee financial consistency.

## Isolation Boundary
Each payee defines an isolation boundary. Operations for different payees may be
processed in parallel without coordination.

## Ordering Guarantee
Payment events for a given payee must be processed in the order they are
received, ensuring deterministic balance updates.

## Atomic Decision Making
Balance validation and ledger updates for a payee must be executed atomically
within the same serialized execution context.

---

# Ledger Partitioning Strategy
## Primary Partitioning
The ledger is partitioned by time, using a RANGE partition on created_at.
- Partitions are created on a monthly basis
- Time-based partitioning controls unbounded growth
- Older partitions become immutable and cold
- Maintenance and archiving are simplified

## Initial Scope
For the initial phase, the ledger uses time-based partitioning only, without
hash-based subpartitioning.
- Reduces complexity	
- Keeps DDL simple	
- Fully supports the current access patterns

## Indexing Strategy
Indexes are defined at the parent table level and propagated to all
partitions.
- (payee_id, created_at) for payee-scoped history and replay
- (payment_id) for correlation and idempotency checks

## Evolution Strategy
If write contention or index pressure becomes a bottleneck, the ledger may
evolve to a multi-level partitioning strategy:
- Primary: RANGE (created_at)
- Secondary: HASH (payee_id)
This evolution requires a new ledger table and controlled migration, without
changing the domain model.

## Partitioning Rationale
- Time controls data volume
- Payee controls logical isolation
- Balance eliminates the need for cross-partition aggregation
- Partitioning decisions remain purely physical, not domain-driven