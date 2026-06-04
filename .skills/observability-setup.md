# Skill: Observability Setup

## Purpose

Configure the running application to emit enough signal to identify where a bottleneck exists
before running EXPLAIN ANALYZE, benchmarks, or messaging analysis.
This is the prerequisite step for every optimizer investigation.

## Rule

Do not start optimizing until the app is emitting the signals below that are relevant to the concern.
"I think the query is slow" is not a bottleneck — a log line with timing is.

---

## 1. SQL logging (Hibernate)

Add to `application.yaml` for local investigation (never commit to production profile):

```yaml
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.orm.jdbc.bind: TRACE   # logs bind parameters
    org.hibernate.stat: DEBUG            # session statistics

spring:
  jpa:
    properties:
      hibernate:
        generate_statistics: true
        session:
          events:
            log:
              LOG_QUERIES_SLOWER_THAN_MS: 50   # log queries taking > 50ms
```

What to look for in the output:
- Queries issued per request/event (N+1 detection)
- Bind parameter values (confirms filters are being applied)
- Execution count vs expected count

## 2. Outbox processor logging

Add structured timing logs around the processor entry point:

```java
// In CashOutRequestedProcessor.process()
long start = System.currentTimeMillis();
log.debug("Processing outbox event id={} aggregateId={}", event.getId(), event.getAggregateId());

// ... processing ...

log.debug("Processed outbox event id={} durationMs={}", event.getId(), System.currentTimeMillis() - start);
```

Use `log.debug` so it can be toggled per environment:

```yaml
logging:
  level:
    io.github.ddmfuhrmann.ledgerpoc: DEBUG
```

## 3. Spring Actuator metrics

`spring-boot-starter-actuator` is already on the classpath. Expose endpoints:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    metrics:
      enabled: true
```

Useful endpoints once the app is running:

```bash
# All available metrics
curl http://localhost:8080/actuator/metrics

# JPA/Hibernate pool usage
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active

# JVM memory
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

## 4. Outbox backlog query (direct DB)

To see backlog depth without the app:

```sql
-- Pending events grouped by type and age
SELECT
    event_type,
    COUNT(*)                                    AS pending_count,
    MIN(created_at)                             AS oldest_event,
    EXTRACT(EPOCH FROM (NOW() - MIN(created_at))) AS oldest_age_seconds
FROM outbox
WHERE status = 'PENDING'
GROUP BY event_type
ORDER BY oldest_age_seconds DESC;
```

Run this before and after any processor change to see if backlog shrinks.

## 5. Balance lock contention (Postgres)

To detect optimistic lock conflicts on `balance`:

```sql
-- Active locks on balance table
SELECT pid, relation::regclass, mode, granted
FROM pg_locks
WHERE relation = 'balance'::regclass;

-- Recent lock wait events
SELECT wait_event_type, wait_event, query, state
FROM pg_stat_activity
WHERE wait_event_type = 'Lock';
```

## Checklist before calling the Optimizer

- [ ] SQL logging enabled and output captured for target operation
- [ ] Slow query threshold set (50ms or appropriate for the concern)
- [ ] Outbox backlog query run — baseline depth recorded
- [ ] Actuator metrics endpoint reachable
- [ ] Relevant log level set to DEBUG for the package under investigation
