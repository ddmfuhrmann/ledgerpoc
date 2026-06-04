-- Extend monthly ledger partitions through end of 2026.
-- Also adds a DEFAULT partition to catch rows that fall outside explicit ranges.

CREATE TABLE ledger_2026_03
    PARTITION OF ledger
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');

CREATE TABLE ledger_2026_04
    PARTITION OF ledger
    FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');

CREATE TABLE ledger_2026_05
    PARTITION OF ledger
    FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');

CREATE TABLE ledger_2026_06
    PARTITION OF ledger
    FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');

CREATE TABLE ledger_2026_07
    PARTITION OF ledger
    FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');

CREATE TABLE ledger_2026_08
    PARTITION OF ledger
    FOR VALUES FROM ('2026-08-01') TO ('2026-09-01');

CREATE TABLE ledger_2026_09
    PARTITION OF ledger
    FOR VALUES FROM ('2026-09-01') TO ('2026-10-01');

CREATE TABLE ledger_2026_10
    PARTITION OF ledger
    FOR VALUES FROM ('2026-10-01') TO ('2026-11-01');

CREATE TABLE ledger_2026_11
    PARTITION OF ledger
    FOR VALUES FROM ('2026-11-01') TO ('2026-12-01');

CREATE TABLE ledger_2026_12
    PARTITION OF ledger
    FOR VALUES FROM ('2026-12-01') TO ('2027-01-01');

CREATE TABLE ledger_default
    PARTITION OF ledger DEFAULT;
