ALTER TABLE outbox ADD COLUMN payee_id BIGINT NOT NULL DEFAULT 0;
ALTER TABLE outbox ALTER COLUMN payee_id DROP DEFAULT;
CREATE INDEX ix_outbox_payee_status_created ON outbox (payee_id, status, created_at);
