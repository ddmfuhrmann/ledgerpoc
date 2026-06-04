# Canonical EXPLAIN ANALYZE Report

This is the expected format for documenting a query optimization. Copy this structure into the Optimizer's output.

---

## Query Optimization: Find active orders by user

**Date:** 2024-01-15
**PR:** #142

### Query

```sql
SELECT o.id, o.status, o.total_amount, o.created_at
FROM orders o
WHERE o.user_id = $1
  AND o.status = 'ACTIVE'
ORDER BY o.created_at DESC
LIMIT 20;
```

### Baseline

**Data volume:** 500k orders, 50k active, avg 10 per user
**Method:** EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT)

```
Limit  (cost=1234.56..1234.61 rows=20 width=48)
       (actual time=245.123..245.134 rows=20 loops=1)
  ->  Sort  (cost=1234.56..1247.06 rows=5000 width=48)
             (actual time=245.121..245.127 rows=20 loops=1)
        Sort Key: created_at DESC
        Sort Method: top-N heapsort  Memory: 27kB
        ->  Seq Scan on orders  (cost=0.00..9876.00 rows=5000 width=48)
                                 (actual time=0.012..240.456 rows=10 loops=1)
              Filter: ((user_id = $1) AND (status = 'ACTIVE'))
              Rows Removed by Filter: 499990
Buffers: shared hit=4321
Planning Time: 0.4 ms
Execution Time: 245.2 ms
```

**Problem:** Sequential scan on 500k rows to find 10. Missing index on `(user_id, status, created_at)`.

### Change

```sql
CREATE INDEX CONCURRENTLY idx_orders_user_status_created
    ON orders (user_id, status, created_at DESC);
```

### After Measurement

```
Limit  (cost=0.56..12.34 rows=20 width=48)
       (actual time=0.034..0.067 rows=20 loops=1)
  ->  Index Scan using idx_orders_user_status_created on orders
        (cost=0.56..123.45 rows=200 width=48)
        (actual time=0.032..0.059 rows=20 loops=1)
        Index Cond: ((user_id = $1) AND (status = 'ACTIVE'))
Buffers: shared hit=5
Planning Time: 0.3 ms
Execution Time: 0.1 ms
```

**Delta:** 245ms → 0.1ms (-99.96%)

### Trade-offs

- Write amplification: INSERT/UPDATE on `orders` now maintains an additional index (~5–10% slower writes)
- Index size: approximately 45MB for 500k rows
- `CONCURRENTLY` avoids table lock during creation — safe for production

### Recommendation

**APPLY**
The query is on a hot path (called on every order list page). Write overhead is acceptable given the read improvement.
