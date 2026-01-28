package io.github.ddmfuhrmann.ledgerpoc.application.event;

public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}