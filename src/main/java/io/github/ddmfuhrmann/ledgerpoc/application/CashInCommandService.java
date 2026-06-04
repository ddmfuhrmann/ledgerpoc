package io.github.ddmfuhrmann.ledgerpoc.application;

import io.github.ddmfuhrmann.ledgerpoc.application.command.RequestCashInCommand;
import io.github.ddmfuhrmann.ledgerpoc.application.event.OutboxEvent;
import io.github.ddmfuhrmann.ledgerpoc.application.event.OutboxEventType;
import io.github.ddmfuhrmann.ledgerpoc.application.event.payload.CashInRequestedPayload;
import io.github.ddmfuhrmann.ledgerpoc.application.support.JsonSerializer;
import io.github.ddmfuhrmann.ledgerpoc.domain.Payee;
import io.github.ddmfuhrmann.ledgerpoc.domain.Payment;
import io.github.ddmfuhrmann.ledgerpoc.domain.PaymentType;
import io.github.ddmfuhrmann.ledgerpoc.infra.repository.OutboxRepository;
import io.github.ddmfuhrmann.ledgerpoc.infra.repository.PayeeRepository;
import io.github.ddmfuhrmann.ledgerpoc.infra.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CashInCommandService {

    private final PayeeRepository payeeRepository;
    private final PaymentRepository paymentRepository;
    private final OutboxRepository outboxRepository;
    private final JsonSerializer jsonSerializer;

    public CashInCommandService(
            PayeeRepository payeeRepository,
            PaymentRepository paymentRepository,
            OutboxRepository outboxRepository,
            JsonSerializer jsonSerializer
    ) {
        this.payeeRepository = payeeRepository;
        this.paymentRepository = paymentRepository;
        this.outboxRepository = outboxRepository;
        this.jsonSerializer = jsonSerializer;
    }

    @Transactional
    public void request(RequestCashInCommand command) {
        Payee payee = payeeRepository.findByExternalId(command.payeeExternalId())
                .orElseThrow(() -> new IllegalArgumentException("Payee not found"));

        Payment payment = new Payment(
                command.paymentExternalId(),
                payee,
                PaymentType.CASH_IN,
                command.amount()
        );

        paymentRepository.save(payment);

        var payload = new CashInRequestedPayload(
                payment.getId(),
                payee.getId(),
                command.amount()
        );

        var eventType = OutboxEventType.CASH_IN_REQUESTED;

        OutboxEvent event = eventType.toEvent(
                payment.getId(),
                payee.getId(),
                jsonSerializer.serialize(payload)
        );

        outboxRepository.save(event);
    }
}
