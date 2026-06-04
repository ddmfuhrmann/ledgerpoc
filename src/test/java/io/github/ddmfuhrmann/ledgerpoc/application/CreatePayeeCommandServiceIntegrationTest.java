package io.github.ddmfuhrmann.ledgerpoc.application;

import io.github.ddmfuhrmann.ledgerpoc.application.command.CreatePayeeCommand;
import io.github.ddmfuhrmann.ledgerpoc.application.event.OutboxEvent;
import io.github.ddmfuhrmann.ledgerpoc.application.event.OutboxEventType;
import io.github.ddmfuhrmann.ledgerpoc.application.event.OutboxStatus;
import io.github.ddmfuhrmann.ledgerpoc.domain.Payee;
import io.github.ddmfuhrmann.ledgerpoc.domain.PayeeStatus;
import io.github.ddmfuhrmann.ledgerpoc.infra.repository.OutboxRepository;
import io.github.ddmfuhrmann.ledgerpoc.infra.repository.PayeeRepository;
import io.github.ddmfuhrmann.ledgerpoc.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CreatePayeeCommandServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CreatePayeeCommandService service;

    @Autowired
    private PayeeRepository payeeRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    // -------------------------------------------------------------------------
    // Test 1: Happy path — persists Payee(ACTIVE) + PAYEE_CREATED outbox event
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    void shouldCreatePayeeAndOutboxEventWhenCreateIsRequested() {
        // given
        UUID externalId = UUID.randomUUID();

        // when
        Payee payee = service.create(new CreatePayeeCommand(externalId));

        // then — payee persisted with ACTIVE status and correct external id
        Payee savedPayee = payeeRepository.findByExternalId(externalId).orElseThrow();
        assertThat(savedPayee.getExternalId()).isEqualTo(externalId);
        assertThat(savedPayee.getStatus()).isEqualTo(PayeeStatus.ACTIVE);

        // then — one PAYEE_CREATED outbox event with status PENDING
        List<OutboxEvent> events = outboxRepository.findAll();
        assertThat(events).hasSize(1);

        OutboxEvent event = events.get(0);
        assertThat(event.getEventType()).isEqualTo(OutboxEventType.PAYEE_CREATED.name());
        assertThat(event.getAggregateType()).isEqualTo("PAYEE");
        assertThat(event.getAggregateId()).isEqualTo(payee.getId());
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
    }
}
