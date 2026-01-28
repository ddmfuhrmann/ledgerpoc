package io.github.ddmfuhrmann.ledgerpoc.application.event.payload;

import io.github.ddmfuhrmann.ledgerpoc.application.event.OutboxEventType;

import java.math.BigDecimal;

public record CashOutFailedPayload(
        Long paymentId,
        Long payeeId,
        BigDecimal amount,
        String reason
) implements OutboxPayload {

    @Override
    public OutboxEventType eventType() {
        return OutboxEventType.CASH_OUT_FAILED;
    }
}