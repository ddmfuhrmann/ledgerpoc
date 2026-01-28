package io.github.ddmfuhrmann.ledgerpoc.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ddmfuhrmann.ledgerpoc.application.command.RequestCashOutCommand;
import io.github.ddmfuhrmann.ledgerpoc.application.event.OutboxEvent;
import io.github.ddmfuhrmann.ledgerpoc.application.event.OutboxEventType;
import io.github.ddmfuhrmann.ledgerpoc.application.event.payload.CashOutRequestedPayload;
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
public class CashOutCommandService {

    private final PayeeRepository payeeRepository;
    private final PaymentRepository paymentRepository;
    private final OutboxRepository outboxRepository;
    private final JsonSerializer jsonSerializer;

    public CashOutCommandService(
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
    public void request(RequestCashOutCommand command) {
        Payee payee = payeeRepository.findByExternalId(command.payeeExternalId())
                .orElseThrow(() -> new IllegalArgumentException("Payee not found"));

        Payment payment = new Payment(
                command.paymentExternalId(),
                payee,
                PaymentType.CASH_OUT,
                command.amount()
        );

        paymentRepository.save(payment);

        var payload = new CashOutRequestedPayload(
                payment.getId(),
                payee.getId(),
                command.amount()
        );

        var eventType = OutboxEventType.CASH_OUT_REQUESTED;

        OutboxEvent event = eventType.toEvent(
                payment.getId(),
                jsonSerializer.serialize(payload)
        );

        outboxRepository.save(event);
    }
}