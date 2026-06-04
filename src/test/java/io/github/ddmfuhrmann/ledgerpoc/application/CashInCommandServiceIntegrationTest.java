package io.github.ddmfuhrmann.ledgerpoc.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ddmfuhrmann.ledgerpoc.application.command.RequestCashInCommand;
import io.github.ddmfuhrmann.ledgerpoc.application.event.OutboxEvent;
import io.github.ddmfuhrmann.ledgerpoc.application.event.OutboxEventType;
import io.github.ddmfuhrmann.ledgerpoc.application.event.payload.CashInRequestedPayload;
import io.github.ddmfuhrmann.ledgerpoc.domain.Payee;
import io.github.ddmfuhrmann.ledgerpoc.domain.Payment;
import io.github.ddmfuhrmann.ledgerpoc.domain.PaymentType;
import io.github.ddmfuhrmann.ledgerpoc.infra.repository.OutboxRepository;
import io.github.ddmfuhrmann.ledgerpoc.infra.repository.PayeeRepository;
import io.github.ddmfuhrmann.ledgerpoc.infra.repository.PaymentRepository;
import io.github.ddmfuhrmann.ledgerpoc.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CashInCommandServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CashInCommandService service;

    @Autowired
    private PayeeRepository payeeRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // -------------------------------------------------------------------------
    // Test 1: Happy path — persists Payment(CASH_IN, REQUESTED) + CASH_IN_REQUESTED outbox event
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    void shouldCreatePaymentAndOutboxEventWhenCashInIsRequested() throws Exception {
        // given
        Payee payee = new Payee(UUID.randomUUID());
        payeeRepository.save(payee);

        UUID paymentExternalId = UUID.randomUUID();

        RequestCashInCommand command = new RequestCashInCommand(
                payee.getExternalId(),
                paymentExternalId,
                BigDecimal.valueOf(100.00)
        );

        // when
        service.request(command);

        // then — payment persisted with correct type and status
        Payment payment = paymentRepository.findByExternalId(paymentExternalId)
                .orElseThrow();

        assertThat(payment.getType()).isEqualTo(PaymentType.CASH_IN);
        assertThat(payment.getAmount()).isEqualByComparingTo("100.00");

        // then — outbox event saved with correct type and payload
        OutboxEvent event = outboxRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow();

        assertThat(event.getAggregateType()).isEqualTo("PAYMENT");
        assertThat(event.getAggregateId()).isEqualTo(payment.getId());
        assertThat(event.getEventType()).isEqualTo(OutboxEventType.CASH_IN_REQUESTED.name());

        CashInRequestedPayload payload =
                objectMapper.readValue(event.getPayload(), CashInRequestedPayload.class);

        assertThat(payload.paymentId()).isEqualTo(payment.getId());
        assertThat(payload.payeeId()).isEqualTo(payee.getId());
        assertThat(payload.amount()).isEqualByComparingTo("100.00");
    }
}
