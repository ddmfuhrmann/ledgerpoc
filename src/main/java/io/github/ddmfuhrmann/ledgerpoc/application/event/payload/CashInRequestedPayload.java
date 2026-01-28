package io.github.ddmfuhrmann.ledgerpoc.application.event.payload;

import io.github.ddmfuhrmann.ledgerpoc.application.event.OutboxEventType;

import java.math.BigDecimal;

public record CashInRequestedPayload(
        Long paymentId,
        Long payeeId,
        BigDecimal amount
) implements OutboxPayload {

    @Override
    public OutboxEventType eventType() {
        return OutboxEventType.CASH_IN_REQUESTED;
    }
}