package io.github.ddmfuhrmann.ledgerpoc.application.command;

import java.math.BigDecimal;
import java.util.UUID;

public record RequestCashInCommand(
        UUID payeeExternalId,
        UUID paymentExternalId,
        BigDecimal amount
) {
}
