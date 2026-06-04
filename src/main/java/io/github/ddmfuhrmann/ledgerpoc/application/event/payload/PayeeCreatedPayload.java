package io.github.ddmfuhrmann.ledgerpoc.application.event.payload;

import io.github.ddmfuhrmann.ledgerpoc.application.event.OutboxEventType;

import java.util.UUID;

public record PayeeCreatedPayload(
        UUID payeeExternalId
) implements OutboxPayload {

    @Override
    public OutboxEventType eventType() {
        return OutboxEventType.PAYEE_CREATED;
    }
}
