package io.github.ddmfuhrmann.ledgerpoc.application;

import io.github.ddmfuhrmann.ledgerpoc.application.event.OutboxEvent;
import io.github.ddmfuhrmann.ledgerpoc.application.event.OutboxEventType;
import io.github.ddmfuhrmann.ledgerpoc.application.event.payload.CashInConfirmedPayload;
import io.github.ddmfuhrmann.ledgerpoc.application.event.payload.CashInRequestedPayload;
import io.github.ddmfuhrmann.ledgerpoc.application.support.JsonSerializer;
import io.github.ddmfuhrmann.ledgerpoc.domain.Balance;
import io.github.ddmfuhrmann.ledgerpoc.domain.LedgerEntry;
import io.github.ddmfuhrmann.ledgerpoc.domain.LedgerType;
import io.github.ddmfuhrmann.ledgerpoc.domain.Payment;
import io.github.ddmfuhrmann.ledgerpoc.domain.PaymentStatus;
import io.github.ddmfuhrmann.ledgerpoc.infra.repository.BalanceRepository;
import io.github.ddmfuhrmann.ledgerpoc.infra.repository.LedgerRepository;
import io.github.ddmfuhrmann.ledgerpoc.infra.repository.OutboxRepository;
import io.github.ddmfuhrmann.ledgerpoc.infra.repository.PaymentRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CashInRequestedProcessor {

    private final PaymentRepository paymentRepository;
    private final BalanceRepository balanceRepository;
    private final LedgerRepository ledgerRepository;
    private final OutboxRepository outboxRepository;
    private final JsonSerializer jsonSerializer;

    public CashInRequestedProcessor(
            PaymentRepository paymentRepository,
            BalanceRepository balanceRepository,
            LedgerRepository ledgerRepository,
            OutboxRepository outboxRepository,
            JsonSerializer jsonSerializer
    ) {
        this.paymentRepository = paymentRepository;
        this.balanceRepository = balanceRepository;
        this.ledgerRepository = ledgerRepository;
        this.outboxRepository = outboxRepository;
        this.jsonSerializer = jsonSerializer;
    }

    @Transactional
    public void process(OutboxEvent event) {
        Payment payment = paymentRepository.findById(event.getAggregateId())
                .orElseThrow(() -> new IllegalStateException("Payment not found"));

        if (payment.getStatus() != PaymentStatus.REQUESTED) {
            return;
        }

        CashInRequestedPayload payload = jsonSerializer.deserialize(event.getPayload(), CashInRequestedPayload.class);

        Balance balance = balanceRepository.findById(payload.payeeId())
                .orElseThrow(() -> new IllegalStateException("Balance not found"));

        balance.credit(payload.amount());

        ledgerRepository.save(new LedgerEntry(payment.getPayee(), payment, LedgerType.CREDIT, payload.amount()));

        payment.confirm();

        outboxRepository.save(OutboxEventType.CASH_IN_CONFIRMED.toEvent(
                payment.getId(),
                jsonSerializer.serialize(new CashInConfirmedPayload(payload.paymentId(), payload.payeeId(), payload.amount()))
        ));
    }
}
