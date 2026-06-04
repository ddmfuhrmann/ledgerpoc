package io.github.ddmfuhrmann.ledgerpoc.application.command;

import java.util.UUID;

public record ProvisionBalanceCommand(
        UUID payeeExternalId
) {
}
