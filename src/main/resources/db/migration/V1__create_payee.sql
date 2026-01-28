CREATE TABLE payee
(
    id          BIGSERIAL PRIMARY KEY,
    external_id UUID        NOT NULL,
    status      VARCHAR(32) NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_payee_external_id ON payee (external_id);
CREATE INDEX ix_payee_status ON payee (status);