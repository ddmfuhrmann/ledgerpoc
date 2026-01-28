package io.github.ddmfuhrmann.ledgerpoc.application.event;

public enum OutboxEventType {

    CASH_IN_REQUESTED,
    CASH_IN_CONFIRMED,
    CASH_IN_FAILED,

    CASH_OUT_REQUESTED,
    CASH_OUT_CONFIRMED,
    CASH_OUT_FAILED

}