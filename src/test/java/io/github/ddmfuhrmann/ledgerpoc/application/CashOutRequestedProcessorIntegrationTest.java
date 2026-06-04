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
class CashOutRequestedProcessorIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CashOutRequestedProcessor processor;

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
    // Test 1: Happy path — funds sufficient
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    void shouldConfirmPaymentAndDebitBalanceAndCreateLedgerEntryWhenFundsAreSufficient() {
        // given
        Payee payee = new Payee(UUID.randomUUID());
        payeeRepository.save(payee);

        Balance balance = new Balance(payee);
        balance.credit(BigDecimal.valueOf(100.00));
        balanceRepository.save(balance);

        Payment payment = new Payment(UUID.randomUUID(), payee, PaymentType.CASH_OUT, BigDecimal.valueOf(100.00));
        paymentRepository.save(payment);

        OutboxEvent triggerEvent = buildCashOutRequestedEvent(payment, payee, BigDecimal.valueOf(100.00));
        outboxRepository.save(triggerEvent);

        // when
        processor.process(triggerEvent);

        // then — payment status
        Payment savedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);

        // then — balance fully debited
        Balance savedBalance = balanceRepository.findById(payee.getId()).orElseThrow();
        assertThat(savedBalance.getAvailableAmount()).isEqualByComparingTo("0.00");

        // then — one DEBIT ledger entry with correct amount
        List<LedgerEntry> entries = ledgerRepository.findAll();
        assertThat(entries).hasSize(1);
        LedgerEntry entry = entries.get(0);
        assertThat(entry.getType()).isEqualTo(LedgerType.DEBIT);
        assertThat(entry.getAmount()).isEqualByComparingTo("100.00");

        // then — CASH_OUT_CONFIRMED outbox event saved
        List<OutboxEvent> allEvents = outboxRepository.findAll();
        OutboxEvent confirmedEvent = allEvents.stream()
                .filter(e -> OutboxEventType.CASH_OUT_CONFIRMED.name().equals(e.getEventType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("CASH_OUT_CONFIRMED event not found"));

        assertThat(confirmedEvent.getAggregateId()).isEqualTo(payment.getId());

        CashOutConfirmedPayload confirmedPayload =
                jsonSerializer.deserialize(confirmedEvent.getPayload(), CashOutConfirmedPayload.class);
        assertThat(confirmedPayload.paymentId()).isEqualTo(payment.getId());
        assertThat(confirmedPayload.payeeId()).isEqualTo(payee.getId());
        assertThat(confirmedPayload.amount()).isEqualByComparingTo("100.00");
    }

    // -------------------------------------------------------------------------
    // Test 2: Insufficient funds
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    void shouldFailPaymentAndSaveFailedEventWhenBalanceIsInsufficient() {
        // given
        Payee payee = new Payee(UUID.randomUUID());
        payeeRepository.save(payee);

        Balance balance = new Balance(payee); // zero balance
        balanceRepository.save(balance);

        Payment payment = new Payment(UUID.randomUUID(), payee, PaymentType.CASH_OUT, BigDecimal.valueOf(100.00));
        paymentRepository.save(payment);

        OutboxEvent triggerEvent = buildCashOutRequestedEvent(payment, payee, BigDecimal.valueOf(100.00));
        outboxRepository.save(triggerEvent);

        // when
        processor.process(triggerEvent);

        // then — payment status FAILED
        Payment savedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);

        // then — balance unchanged (still 0)
        Balance savedBalance = balanceRepository.findById(payee.getId()).orElseThrow();
        assertThat(savedBalance.getAvailableAmount()).isEqualByComparingTo("0.00");

        // then — no ledger entries created
        assertThat(ledgerRepository.findAll()).isEmpty();

        // then — CASH_OUT_FAILED outbox event with reason INSUFFICIENT_FUNDS
        List<OutboxEvent> allEvents = outboxRepository.findAll();
        OutboxEvent failedEvent = allEvents.stream()
                .filter(e -> OutboxEventType.CASH_OUT_FAILED.name().equals(e.getEventType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("CASH_OUT_FAILED event not found"));

        assertThat(failedEvent.getAggregateId()).isEqualTo(payment.getId());

        CashOutFailedPayload failedPayload =
                jsonSerializer.deserialize(failedEvent.getPayload(), CashOutFailedPayload.class);
        assertThat(failedPayload.reason()).isEqualTo("INSUFFICIENT_FUNDS");
        assertThat(failedPayload.paymentId()).isEqualTo(payment.getId());
        assertThat(failedPayload.payeeId()).isEqualTo(payee.getId());
        assertThat(failedPayload.amount()).isEqualByComparingTo("100.00");
    }

    // -------------------------------------------------------------------------
    // Test 3: Idempotency — payment already CONFIRMED
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

        Payment payment = new Payment(UUID.randomUUID(), payee, PaymentType.CASH_OUT, BigDecimal.valueOf(100.00));
        payment.confirm(); // already confirmed before processor runs
        paymentRepository.save(payment);

        OutboxEvent triggerEvent = buildCashOutRequestedEvent(payment, payee, BigDecimal.valueOf(100.00));
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
    }

    // -------------------------------------------------------------------------
    // Test 4: Idempotency — payment already FAILED
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

        Payment payment = new Payment(UUID.randomUUID(), payee, PaymentType.CASH_OUT, BigDecimal.valueOf(100.00));
        payment.fail(); // already failed before processor runs
        paymentRepository.save(payment);

        OutboxEvent triggerEvent = buildCashOutRequestedEvent(payment, payee, BigDecimal.valueOf(100.00));
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
    }

    // -------------------------------------------------------------------------
    // Test 5: Balance not found
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    void shouldThrowIllegalStateExceptionWhenBalanceRowDoesNotExist() {
        // given — payee and payment exist but NO Balance row
        Payee payee = new Payee(UUID.randomUUID());
        payeeRepository.save(payee);

        Payment payment = new Payment(UUID.randomUUID(), payee, PaymentType.CASH_OUT, BigDecimal.valueOf(100.00));
        paymentRepository.save(payment);

        OutboxEvent triggerEvent = buildCashOutRequestedEvent(payment, payee, BigDecimal.valueOf(100.00));
        outboxRepository.save(triggerEvent);

        // when / then
        assertThatThrownBy(() -> processor.process(triggerEvent))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Balance not found");
    }

    // -------------------------------------------------------------------------
    // Fixture helpers
    // -------------------------------------------------------------------------

    private OutboxEvent buildCashOutRequestedEvent(Payment payment, Payee payee, BigDecimal amount) {
        CashOutRequestedPayload payload = new CashOutRequestedPayload(
                payment.getId(),
                payee.getId(),
                amount
        );
        return OutboxEventType.CASH_OUT_REQUESTED.toEvent(
                payment.getId(),
                payee.getId(),
                jsonSerializer.serialize(payload)
        );
    }
}
