# Skill: Messaging Analysis

## Purpose

Measure performance of the asynchronous processing layer: outbox throughput, consumer latency,
SQS/LocalStack backlog, and message failure rate.
Use this skill when the concern is in the event pipeline, not in a specific query or endpoint.

## Signals to measure

| Signal | What it reveals |
|---|---|
| Outbox backlog depth (PENDING count) | Whether the processor is keeping up with producers |
| Processing latency (created_at → published_at) | End-to-end async processing time vs SLA |
| Failure rate (FAILED / total) | Reliability of the processor under load |
| Throughput (events processed / second) | Capacity ceiling before backlog grows |
| Consumer lag (SQS ApproximateNumberOfMessages) | Whether the SQS consumer is behind |

---

## 1. Outbox throughput and latency (direct SQL)

```sql
-- Throughput: events processed in the last 60 seconds
SELECT
    event_type,
    COUNT(*) AS processed_count
FROM outbox
WHERE status = 'PUBLISHED'
  AND published_at >= NOW() - INTERVAL '60 seconds'
GROUP BY event_type;

-- Processing latency: time from creation to publication (p50, p95, p99)
SELECT
    event_type,
    PERCENTILE_CONT(0.50) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (published_at - created_at))) AS p50_sec,
    PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (published_at - created_at))) AS p95_sec,
    PERCENTILE_CONT(0.99) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (published_at - created_at))) AS p99_sec
FROM outbox
WHERE status = 'PUBLISHED'
  AND published_at >= NOW() - INTERVAL '10 minutes'
GROUP BY event_type;

-- Failure rate
SELECT
    event_type,
    COUNT(*) FILTER (WHERE status = 'FAILED')   AS failed,
    COUNT(*) FILTER (WHERE status = 'PUBLISHED') AS published,
    COUNT(*)                                     AS total,
    ROUND(
        100.0 * COUNT(*) FILTER (WHERE status = 'FAILED') / NULLIF(COUNT(*), 0), 2
    )                                            AS failure_pct
FROM outbox
GROUP BY event_type;

-- Backlog trend over time (1-minute buckets)
SELECT
    DATE_TRUNC('minute', created_at) AS bucket,
    COUNT(*) AS events_created,
    COUNT(*) FILTER (WHERE status = 'PENDING') AS still_pending
FROM outbox
WHERE created_at >= NOW() - INTERVAL '30 minutes'
GROUP BY bucket
ORDER BY bucket;
```

## 2. SQS / LocalStack consumer lag

When SQS is wired up, use the AWS CLI against LocalStack:

```bash
# Queue depth (how many messages waiting to be consumed)
aws --endpoint-url=http://localhost:4566 sqs get-queue-attributes \
    --queue-url http://localhost:4566/000000000000/cash-out-requested \
    --attribute-names ApproximateNumberOfMessages ApproximateNumberOfMessagesNotVisible

# Watch lag in real time (1-second interval)
watch -n 1 "aws --endpoint-url=http://localhost:4566 sqs get-queue-attributes \
    --queue-url http://localhost:4566/000000000000/cash-out-requested \
    --attribute-names ApproximateNumberOfMessages"
```

Key attributes:
- `ApproximateNumberOfMessages` — visible messages (not yet consumed)
- `ApproximateNumberOfMessagesNotVisible` — in-flight (consumed but not deleted)
- Rising `ApproximateNumberOfMessages` under steady load = consumer is falling behind

## 3. Baseline capture format

Always capture before any change. Record:

```
Target: outbox processor throughput — CASH_OUT_REQUESTED
Data volume: X payees seeded, Y events inserted (see .skills/database-seeding.md)
Window: 5-minute steady load via [tool / script]

Outbox backlog before: N PENDING events
Processing p50 latency: Xs
Processing p99 latency: Xs
Throughput: N events/sec
SQS lag (if applicable): N messages
Failure rate: X%
```

## 4. Load generation for messaging layer

To stress the outbox processor without an HTTP layer, insert events directly:

```sql
-- Simulate 1000 pending CASH_OUT_REQUESTED events (requires seeded payees + payments)
INSERT INTO outbox (aggregate_type, aggregate_id, event_type, payload, status, created_at)
SELECT
    'PAYMENT',
    p.id,
    'CASH_OUT_REQUESTED',
    jsonb_build_object('paymentId', p.id, 'payeeId', p.payee_id, 'amount', 100.00),
    'PENDING',
    NOW() - (random() * INTERVAL '5 minutes')
FROM payment p
LIMIT 1000;
```

Then watch the backlog queries in section 1 drain as the processor runs.

## 5. Payee serialization verification

To confirm single-threaded processing per payee is being respected under load:

```sql
-- Check for overlapping processing windows per payee (should be empty if correct)
SELECT
    o1.aggregate_id AS payment_1,
    o2.aggregate_id AS payment_2,
    p1.payee_id
FROM outbox o1
JOIN outbox o2
    ON o1.id < o2.id
    AND o1.event_type = o2.event_type
JOIN payment p1 ON o1.aggregate_id = p1.id
JOIN payment p2 ON o2.aggregate_id = p2.id
WHERE p1.payee_id = p2.payee_id
  AND o1.published_at > o2.created_at   -- o2 started before o1 finished
  AND o2.published_at < o1.published_at -- but o2 finished before o1
  AND o1.status = 'PUBLISHED'
  AND o2.status = 'PUBLISHED';
```

## SLA reference (from README)

- CashOut processing: request accepted → CONFIRMED or FAILED within defined max time
- Balance read: low latency, eventual consistency acceptable for replica

Document actual p99 vs SLA target in every optimization report.
