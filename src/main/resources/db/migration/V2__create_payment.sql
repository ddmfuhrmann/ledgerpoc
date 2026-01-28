CREATE TABLE payment
(
    id          BIGSERIAL PRIMARY KEY,
    external_id UUID           NOT NULL,
    payee_id    BIGINT         NOT NULL,
    type        VARCHAR(16)    NOT NULL,
    status      VARCHAR(32)    NOT NULL,
    amount      NUMERIC(18, 2) NOT NULL,
    created_at  TIMESTAMP      NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP      NOT NULL DEFAULT now(),

    CONSTRAINT fk_payment_payee
        FOREIGN KEY (payee_id)
            REFERENCES payee (id)
);

CREATE UNIQUE INDEX ux_payment_external_id ON payment (external_id);
CREATE INDEX ix_payment_payee_id ON payment (payee_id);
CREATE INDEX ix_payment_status ON payment (status);
CREATE INDEX ix_payment_created_at ON payment (created_at);