package io.github.ddmfuhrmann.ledgerpoc.application;

import io.github.ddmfuhrmann.ledgerpoc.application.command.ProvisionBalanceCommand;
import io.github.ddmfuhrmann.ledgerpoc.application.event.OutboxEventType;
import io.github.ddmfuhrmann.ledgerpoc.application.event.payload.BalanceProvisionedPayload;
import io.github.ddmfuhrmann.ledgerpoc.application.support.JsonSerializer;
import io.github.ddmfuhrmann.ledgerpoc.domain.Balance;
import io.github.ddmfuhrmann.ledgerpoc.domain.Payee;
import io.github.ddmfuhrmann.ledgerpoc.infra.repository.BalanceRepository;
import io.github.ddmfuhrmann.ledgerpoc.infra.repository.OutboxRepository;
import io.github.ddmfuhrmann.ledgerpoc.infra.repository.PayeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProvisionBalanceCommandService {

    private final PayeeRepository payeeRepository;
    private final BalanceRepository balanceRepository;
    private final OutboxRepository outboxRepository;
    private final JsonSerializer jsonSerializer;

    public ProvisionBalanceCommandService(
            PayeeRepository payeeRepository,
            BalanceRepository balanceRepository,
            OutboxRepository outboxRepository,
            JsonSerializer jsonSerializer
    ) {
        this.payeeRepository = payeeRepository;
        this.balanceRepository = balanceRepository;
        this.outboxRepository = outboxRepository;
        this.jsonSerializer = jsonSerializer;
    }

    @Transactional
    public void provision(ProvisionBalanceCommand command) {
        Payee payee = payeeRepository.findByExternalId(command.payeeExternalId())
                .orElseThrow(() -> new IllegalStateException("Payee not found"));

        if (balanceRepository.existsById(payee.getId())) {
            return;
        }

        balanceRepository.save(new Balance(payee));

        var payload = new BalanceProvisionedPayload(payee.getExternalId());

        outboxRepository.save(OutboxEventType.BALANCE_PROVISIONED.toEvent(
                payee.getId(),
                payee.getId(),
                jsonSerializer.serialize(payload)
        ));
    }
}
