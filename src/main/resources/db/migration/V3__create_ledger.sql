CREATE TABLE ledger
(
    id         BIGSERIAL,
    payee_id   BIGINT         NOT NULL,
    payment_id BIGINT         NOT NULL,
    type       VARCHAR(16)    NOT NULL,
    amount     NUMERIC(18, 2) NOT NULL,
    created_at TIMESTAMP      NOT NULL DEFAULT now(),

    PRIMARY KEY (id, created_at),

    CONSTRAINT fk_ledger_payee
        FOREIGN KEY (payee_id)
            REFERENCES payee (id),

    CONSTRAINT fk_ledger_payment
        FOREIGN KEY (payment_id)
            REFERENCES payment (id)
) PARTITION BY RANGE (created_at);

CREATE INDEX ix_ledger_payee_created_at
    ON ledger (payee_id, created_at);
CREATE INDEX ix_ledger_payment_id
    ON ledger (payment_id);