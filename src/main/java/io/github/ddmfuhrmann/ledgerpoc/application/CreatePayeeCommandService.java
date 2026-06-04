package io.github.ddmfuhrmann.ledgerpoc.application;

import io.github.ddmfuhrmann.ledgerpoc.application.command.CreatePayeeCommand;
import io.github.ddmfuhrmann.ledgerpoc.application.event.OutboxEventType;
import io.github.ddmfuhrmann.ledgerpoc.application.event.payload.PayeeCreatedPayload;
import io.github.ddmfuhrmann.ledgerpoc.application.support.JsonSerializer;
import io.github.ddmfuhrmann.ledgerpoc.domain.Payee;
import io.github.ddmfuhrmann.ledgerpoc.infra.repository.OutboxRepository;
import io.github.ddmfuhrmann.ledgerpoc.infra.repository.PayeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreatePayeeCommandService {

    private final PayeeRepository payeeRepository;
    private final OutboxRepository outboxRepository;
    private final JsonSerializer jsonSerializer;

    public CreatePayeeCommandService(
            PayeeRepository payeeRepository,
            OutboxRepository outboxRepository,
            JsonSerializer jsonSerializer
    ) {
        this.payeeRepository = payeeRepository;
        this.outboxRepository = outboxRepository;
        this.jsonSerializer = jsonSerializer;
    }

    @Transactional
    public Payee create(CreatePayeeCommand command) {
        Payee payee = payeeRepository.save(new Payee(command.payeeExternalId()));

        var payload = new PayeeCreatedPayload(payee.getExternalId());

        outboxRepository.save(OutboxEventType.PAYEE_CREATED.toEvent(
                payee.getId(),
                payee.getId(),
                jsonSerializer.serialize(payload)
        ));

        return payee;
    }
}
