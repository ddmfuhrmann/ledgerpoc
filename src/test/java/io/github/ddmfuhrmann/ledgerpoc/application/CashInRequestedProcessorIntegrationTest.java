package io.github.ddmfuhrmann.ledgerpoc.application;

import io.github.ddmfuhrmann.ledgerpoc.application.event.OutboxEvent;
import io.github.ddmfuhrmann.ledgerpoc.application.event.OutboxEventType;
import io.github.ddmfuhrmann.ledgerpoc.application.event.payload.CashInConfirmedPayload;
import io.github.ddmfuhrmann.ledgerpoc.application.event.payload.CashInRequestedPayload;
import io.github.ddmfuhrmann.ledgerpoc.application.support.JsonSerializer;
import io.github.ddmfuhrmann.ledgerpoc.domain.Balance;
import io.github.ddmfuhrmann.ledgerpoc.domain.LedgerEntry;
import io.github.ddmfuhrmann.ledgerpoc.domain.LedgerType;
import io.github.ddmfuhrmann.ledgerpoc.domain.Payee;
import io.github.ddmfuhrmann.ledgerpoc.domain.Payment;
import io.github.ddmfuhrmann.ledgerpoc.domain.PaymentStatus;
import io.github.ddmfuhrmann.ledgerpoc.domain.PaymentType;
import io.github.ddmfuhrmann.ledgerpoc.infra.repository.BalanceRepository;
import io.github.ddmfuhrmann.ledgerpoc.infra.repository.LedgerRepository;
import io.github.ddmfuhrmann.ledgerpoc.infra.repository.OutboxRepository;
import io.github.ddmfuhrmann.ledgerpoc.infra.repository.PayeeRepository;
import io.github.ddmfuhrmann.ledgerpoc.infra.repository.PaymentRepository;
import io.github.ddmfuhrmann.ledgerpoc.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class CashInRequestedProcessorIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CashInRequestedProcessor processor;

    @Autowired
    private PayeeRepository payeeRepository;

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private LedgerRepository ledgerRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private JsonSerializer jsonSerializer;

    // -------------------------------------------------------------------------
    // Test 1: Happy path — balance credited, CONFIRMED, ledger entry created
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    void shouldConfirmPaymentAndCreditBalanceAndCreateLedgerEntryWhenCashInIsProcessed() {
        // given
        Payee payee = new Payee(UUID.randomUUID());
        payeeRepository.save(payee);

        Balance balance = new Balance(payee); // zero balance
        balanceRepository.save(balance);

        Payment payment = new Payment(UUID.randomUUID(), payee, PaymentType.CASH_IN, BigDecimal.valueOf(100.00));
        paymentRepository.save(payment);

        OutboxEvent triggerEvent = buildCashInRequestedEvent(payment, payee, BigDecimal.valueOf(100.00));
        outboxRepository.save(triggerEvent);

        // when
        processor.process(triggerEvent);

        // then — payment confirmed
        Payment savedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);

        // then — balance credited from 0 to 100
        Balance savedBalance = balanceRepository.findById(payee.getId()).orElseThrow();
        assertThat(savedBalance.getAvailableAmount()).isEqualByComparingTo("100.00");

        // then — one CREDIT ledger entry with correct amount
        List<LedgerEntry> entries = ledgerRepository.findAll();
        assertThat(entries).hasSize(1);
        LedgerEntry entry = entries.get(0);
        assertThat(entry.getType()).isEqualTo(LedgerType.CREDIT);
        assertThat(entry.getAmount()).isEqualByComparingTo("100.00");

        // then — CASH_IN_CONFIRMED outbox event saved with correct payload
        List<OutboxEvent> allEvents = outboxRepository.findAll();
        OutboxEvent confirmedEvent = allEvents.stream()
                .filter(e -> OutboxEventType.CASH_IN_CONFIRMED.name().equals(e.getEventType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("CASH_IN_CONFIRMED event not found"));

        assertThat(confirmedEvent.getAggregateId()).isEqualTo(payment.getId());

        CashInConfirmedPayload confirmedPayload =
                jsonSerializer.deserialize(confirmedEvent.getPayload(), CashInConfirmedPayload.class);
        assertThat(confirmedPayload.paymentId()).isEqualTo(payment.getId());
        assertThat(confirmedPayload.payeeId()).isEqualTo(payee.getId());
        assertThat(confirmedPayload.amount()).isEqualByComparingTo("100.00");
    }

    // -------------------------------------------------------------------------
    // Test 2: Idempotency — payment already CONFIRMED
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    void shouldSkipProcessingAndNotSaveNewOutboxEventWhenPaymentIsAlreadyConfirmed() {
        // given
        Payee payee = new Payee(UUID.randomUUID());
        payeeRepository.save(payee);

        Balance balance = new Balance(payee);
        balance.credit(BigDecimal.valueOf(100.00));
        balanceRepository.save(balance);

        Payment payment = new Payment(UUID.randomUUID(), payee, PaymentType.CASH_IN, BigDecimal.valueOf(100.00));
        payment.confirm(); // already confirmed before processor runs
        paymentRepository.save(payment);

        OutboxEvent triggerEvent = buildCashInRequestedEvent(payment, payee, BigDecimal.valueOf(100.00));
        outboxRepository.save(triggerEvent);

        int outboxCountBefore = outboxRepository.findAll().size();

        // when
        processor.process(triggerEvent);

        // then — no new outbox events added
        int outboxCountAfter = outboxRepository.findAll().size();
        assertThat(outboxCountAfter).isEqualTo(outboxCountBefore);

        // then — balance unchanged
        Balance savedBalance = balanceRepository.findById(payee.getId()).orElseThrow();
        assertThat(savedBalance.getAvailableAmount()).isEqualByComparingTo("100.00");

        // then — no ledger entries created
        assertThat(ledgerRepository.findAll()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Test 3: Idempotency — payment already FAILED
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    void shouldSkipProcessingAndNotSaveNewOutboxEventWhenPaymentIsAlreadyFailed() {
        // given
        Payee payee = new Payee(UUID.randomUUID());
        payeeRepository.save(payee);

        Balance balance = new Balance(payee);
        balance.credit(BigDecimal.valueOf(100.00));
        balanceRepository.save(balance);

        Payment payment = new Payment(UUID.randomUUID(), payee, PaymentType.CASH_IN, BigDecimal.valueOf(100.00));
        payment.fail(); // already failed before processor runs
        paymentRepository.save(payment);

        OutboxEvent triggerEvent = buildCashInRequestedEvent(payment, payee, BigDecimal.valueOf(100.00));
        outboxRepository.save(triggerEvent);

        int outboxCountBefore = outboxRepository.findAll().size();

        // when
        processor.process(triggerEvent);

        // then — no new outbox events added
        int outboxCountAfter = outboxRepository.findAll().size();
        assertThat(outboxCountAfter).isEqualTo(outboxCountBefore);

        // then — balance unchanged
        Balance savedBalance = balanceRepository.findById(payee.getId()).orElseThrow();
        assertThat(savedBalance.getAvailableAmount()).isEqualByComparingTo("100.00");

        // then — no ledger entries created
        assertThat(ledgerRepository.findAll()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Test 4: Balance not found
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    void shouldThrowIllegalStateExceptionWhenBalanceRowDoesNotExist() {
        // given — payee and payment exist but NO Balance row
        Payee payee = new Payee(UUID.randomUUID());
        payeeRepository.save(payee);

        Payment payment = new Payment(UUID.randomUUID(), payee, PaymentType.CASH_IN, BigDecimal.valueOf(100.00));
        paymentRepository.save(payment);

        OutboxEvent triggerEvent = buildCashInRequestedEvent(payment, payee, BigDecimal.valueOf(100.00));
        outboxRepository.save(triggerEvent);

        // when / then
        assertThatThrownBy(() -> processor.process(triggerEvent))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Balance not found");
    }

    // -------------------------------------------------------------------------
    // Fixture helpers
    // -------------------------------------------------------------------------

    private OutboxEvent buildCashInRequestedEvent(Payment payment, Payee payee, BigDecimal amount) {
        CashInRequestedPayload payload = new CashInRequestedPayload(
                payment.getId(),
                payee.getId(),
                amount
        );
        return OutboxEventType.CASH_IN_REQUESTED.toEvent(
                payment.getId(),
                payee.getId(),
                jsonSerializer.serialize(payload)
        );
    }
}
