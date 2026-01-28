package io.github.ddmfuhrmann.ledgerpoc.application.event.payload;

import java.math.BigDecimal;

public record CashOutFailedPayload(
        Long paymentId,
        Long payeeId,
        BigDecimal amount,
        String reason
) {}