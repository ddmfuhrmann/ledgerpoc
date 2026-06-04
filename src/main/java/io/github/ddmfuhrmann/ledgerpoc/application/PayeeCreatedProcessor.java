package io.github.ddmfuhrmann.ledgerpoc.application;

import io.github.ddmfuhrmann.ledgerpoc.application.command.ProvisionBalanceCommand;
import io.github.ddmfuhrmann.ledgerpoc.application.event.OutboxEvent;
import io.github.ddmfuhrmann.ledgerpoc.application.event.payload.PayeeCreatedPayload;
import io.github.ddmfuhrmann.ledgerpoc.application.support.JsonSerializer;
import io.github.ddmfuhrmann.ledgerpoc.infra.repository.OutboxRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PayeeCreatedProcessor {

    private final ProvisionBalanceCommandService provisionBalanceCommandService;
    private final OutboxRepository outboxRepository;
    private final JsonSerializer jsonSerializer;

    public PayeeCreatedProcessor(
            ProvisionBalanceCommandService provisionBalanceCommandService,
            OutboxRepository outboxRepository,
            JsonSerializer jsonSerializer
    ) {
        this.provisionBalanceCommandService = provisionBalanceCommandService;
        this.outboxRepository = outboxRepository;
        this.jsonSerializer = jsonSerializer;
    }

    @Transactional
    public void process(OutboxEvent event) {
        PayeeCreatedPayload payload = jsonSerializer.deserialize(event.getPayload(), PayeeCreatedPayload.class);

        provisionBalanceCommandService.provision(new ProvisionBalanceCommand(payload.payeeExternalId()));

        event.markPublished();
        outboxRepository.save(event);
    }
}
