package io.github.ddmfuhrmann.ledgerpoc.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ddmfuhrmann.ledgerpoc.application.command.RequestCashOutCommand;
import io.github.ddmfuhrmann.ledgerpoc.application.event.OutboxEvent;
import io.github.ddmfuhrmann.ledgerpoc.application.event.OutboxEventType;
import io.github.ddmfuhrmann.ledgerpoc.application.event.payload.CashOutRequestedPayload;
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
class CashOutCommandServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CashOutCommandService service;

    @Autowired
    private PayeeRepository payeeRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Transactional
    void shouldCreatePaymentAndOutboxEventWhenCashOutIsRequested() throws Exception {

        // given
        Payee payee = new Payee(
                UUID.randomUUID()
        );
        payeeRepository.save(payee);

        UUID paymentExternalId = UUID.randomUUID();

        RequestCashOutCommand command = new RequestCashOutCommand(
                payee.getExternalId(),
                paymentExternalId,
                BigDecimal.valueOf(100.00)
        );

        // when
        service.request(command);

        // then - payment
        Payment payment = paymentRepository.findByExternalId(paymentExternalId)
                .orElseThrow();

        assertThat(payment.getType()).isEqualTo(PaymentType.CASH_OUT);
        assertThat(payment.getAmount()).isEqualByComparingTo("100.00");

        // then - outbox event
        OutboxEvent event = outboxRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow();

        assertThat(event.getAggregateType()).isEqualTo("PAYMENT");
        assertThat(event.getAggregateId()).isEqualTo(payment.getId());
        assertThat(event.getEventType()).isEqualTo(OutboxEventType.CASH_OUT_REQUESTED.name());

        CashOutRequestedPayload payload =
                objectMapper.readValue(event.getPayload(), CashOutRequestedPayload.class);

        assertThat(payload.paymentId()).isEqualTo(payment.getId());
        assertThat(payload.payeeId()).isEqualTo(payee.getId());
        assertThat(payload.amount()).isEqualByComparingTo("100.00");
    }
}