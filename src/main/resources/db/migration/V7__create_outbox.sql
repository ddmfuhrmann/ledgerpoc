CREATE TABLE outbox
(
    id             BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(32) NOT NULL,
    aggregate_id   BIGINT      NOT NULL,
    event_type     VARCHAR(64) NOT NULL,
    payload        JSONB       NOT NULL,
    status         VARCHAR(16) NOT NULL,
    created_at     TIMESTAMP   NOT NULL DEFAULT now(),
    published_at   TIMESTAMP
);

CREATE INDEX ix_outbox_status_created
    ON outbox (status, created_at);