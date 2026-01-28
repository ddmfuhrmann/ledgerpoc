-- Initial monthly ledger partitions.
-- Ledger partitions must be created ahead of time on a monthly basis.

CREATE TABLE ledger_2026_01
    PARTITION OF ledger
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');

CREATE TABLE ledger_2026_02
    PARTITION OF ledger
    FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');