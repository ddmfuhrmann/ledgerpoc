package io.github.ddmfuhrmann.ledgerpoc.application.event.payload;

import java.math.BigDecimal;

public record CashOutRequestedPayload(
        Long paymentId,
        Long payeeId,
        BigDecimal amount
) {}