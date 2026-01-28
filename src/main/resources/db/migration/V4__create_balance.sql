CREATE TABLE balance
(
    payee_id         BIGINT PRIMARY KEY,
    available_amount NUMERIC(18, 2) NOT NULL,
    version          BIGINT         NOT NULL,
    updated_at       TIMESTAMP      NOT NULL DEFAULT now(),

    CONSTRAINT fk_balance_payee
        FOREIGN KEY (payee_id)
            REFERENCES payee (id)
);