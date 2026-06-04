package io.github.ddmfuhrmann.ledgerpoc.application.command;

import java.util.UUID;

public record CreatePayeeCommand(
        UUID payeeExternalId
) {
}
