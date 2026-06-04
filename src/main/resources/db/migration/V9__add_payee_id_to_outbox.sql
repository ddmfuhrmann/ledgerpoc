ALTER TABLE outbox ADD COLUMN payee_id BIGINT NOT NULL;
CREATE INDEX ix_outbox_payee_status_created ON outbox (payee_id, status, created_at);
