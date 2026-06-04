package io.github.ddmfuhrmann.ledgerpoc.application;

import io.github.ddmfuhrmann.ledgerpoc.application.event.OutboxEvent;
import io.github.ddmfuhrmann.ledgerpoc.application.event.OutboxEventType;
import io.github.ddmfuhrmann.ledgerpoc.application.event.payload.CashOutConfirmedPayload;
import io.github.ddmfuhrmann.ledgerpoc.application.event.payload.CashOutFailedPayload;
import io.github.ddmfuhrmann.ledgerpoc.application.event.payload.CashOutRequestedPayload;
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
public class CashOutRequestedProcessor {

    private final PaymentRepository paymentRepository;
    private final BalanceRepository balanceRepository;
    private final LedgerRepository ledgerRepository;
    private final OutboxRepository outboxRepository;
    private final JsonSerializer jsonSerializer;

    public CashOutRequestedProcessor(
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

        // Idempotência simples por estado
        if (payment.getStatus() != PaymentStatus.REQUESTED) {
            return;
        }

        CashOutRequestedPayload payload =
                jsonSerializer.deserialize(event.getPayload(), CashOutRequestedPayload.class);

        Balance balance = balanceRepository.findById(payload.payeeId())
                .orElseThrow(() -> new IllegalStateException("Balance not found"));

        applyDebit(payment, balance, payload);
    }

    private void applyDebit(Payment payment, Balance balance, CashOutRequestedPayload payload) {
        // Catches IllegalStateException (insufficient funds) only.
        // IllegalArgumentException from an invalid amount propagates intentionally —
        // a non-positive amount in a persisted outbox payload is an upstream bug, not a business failure.
        try {
            balance.debit(payload.amount());
        } catch (IllegalStateException e) {
            failCashOut(payment, payload);
            return;
        }
        confirmCashOut(payment, payload);
    }

    private void failCashOut(Payment payment, CashOutRequestedPayload payload) {
        payment.fail();
        outboxRepository.save(
                OutboxEventType.CASH_OUT_FAILED.toEvent(
                        payment.getId(),
                        jsonSerializer.serialize(new CashOutFailedPayload(
                                payload.paymentId(),
                                payload.payeeId(),
                                payload.amount(),
                                "INSUFFICIENT_FUNDS"
                        ))
                )
        );
    }

    private void confirmCashOut(Payment payment, CashOutRequestedPayload payload) {
        ledgerRepository.save(
                new LedgerEntry(payment.getPayee(), payment, LedgerType.DEBIT, payload.amount())
        );
        payment.confirm();
        outboxRepository.save(
                OutboxEventType.CASH_OUT_CONFIRMED.toEvent(
                        payment.getId(),
                        jsonSerializer.serialize(new CashOutConfirmedPayload(
                                payload.paymentId(),
                                payload.payeeId(),
                                payload.amount()
                        ))
                )
        );
    }
}
