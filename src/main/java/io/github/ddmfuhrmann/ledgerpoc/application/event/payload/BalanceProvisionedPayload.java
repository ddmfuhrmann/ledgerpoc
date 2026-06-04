package io.github.ddmfuhrmann.ledgerpoc.application.event.payload;

import io.github.ddmfuhrmann.ledgerpoc.application.event.OutboxEventType;

import java.util.UUID;

public record BalanceProvisionedPayload(
        UUID payeeExternalId
) implements OutboxPayload {

    @Override
    public OutboxEventType eventType() {
        return OutboxEventType.BALANCE_PROVISIONED;
    }
}
