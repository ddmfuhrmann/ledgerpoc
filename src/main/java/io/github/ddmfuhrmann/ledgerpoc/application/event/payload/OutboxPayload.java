package io.github.ddmfuhrmann.ledgerpoc.application.event.payload;

import io.github.ddmfuhrmann.ledgerpoc.application.event.OutboxEventType;

public interface OutboxPayload {

    OutboxEventType eventType();
}