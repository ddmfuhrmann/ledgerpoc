CREATE TABLE balance_replica
(
    payee_id         BIGINT PRIMARY KEY,
    available_amount NUMERIC(18, 2) NOT NULL,
    last_event_id    UUID           NOT NULL,
    updated_at       TIMESTAMP      NOT NULL DEFAULT now(),

    CONSTRAINT fk_balance_replica_payee
        FOREIGN KEY (payee_id)
            REFERENCES payee (id)
);