package io.github.ddmfuhrmann.ledgerpoc.application;

import io.github.ddmfuhrmann.ledgerpoc.application.command.CreatePayeeCommand;
import io.github.ddmfuhrmann.ledgerpoc.application.command.ProvisionBalanceCommand;
import io.github.ddmfuhrmann.ledgerpoc.application.event.OutboxEvent;
import io.github.ddmfuhrmann.ledgerpoc.application.event.OutboxEventType;
import io.github.ddmfuhrmann.ledgerpoc.application.event.OutboxStatus;
import io.github.ddmfuhrmann.ledgerpoc.domain.Balance;
import io.github.ddmfuhrmann.ledgerpoc.domain.Payee;
import io.github.ddmfuhrmann.ledgerpoc.infra.repository.BalanceRepository;
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
class PayeeCreatedProcessorIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PayeeCreatedProcessor processor;

    @Autowired
    private CreatePayeeCommandService createPayeeCommandService;

    @Autowired
    private ProvisionBalanceCommandService provisionBalanceCommandService;

    @Autowired
    private PayeeRepository payeeRepository;

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    // -------------------------------------------------------------------------
    // Test 1: Happy path — Balance provisioned, BALANCE_PROVISIONED emitted,
    //         PAYEE_CREATED marked PUBLISHED
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    void shouldProvisionBalanceAndMarkPayeeCreatedPublishedWhenProcessed() {
        // given — create payee + PAYEE_CREATED event via command service
        UUID externalId = UUID.randomUUID();
        Payee payee = createPayeeCommandService.create(new CreatePayeeCommand(externalId));

        OutboxEvent payeeCreatedEvent = outboxRepository.findAll().stream()
                .filter(e -> OutboxEventType.PAYEE_CREATED.name().equals(e.getEventType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("PAYEE_CREATED event not found"));

        // when
        processor.process(payeeCreatedEvent);

        // then — Balance exists with availableAmount=0
        Balance savedBalance = balanceRepository.findById(payee.getId()).orElseThrow();
        assertThat(savedBalance.getAvailableAmount()).isEqualByComparingTo("0");

        // then — BALANCE_PROVISIONED event is PENDING
        List<OutboxEvent> allEvents = outboxRepository.findAll();
        OutboxEvent balanceProvisionedEvent = allEvents.stream()
                .filter(e -> OutboxEventType.BALANCE_PROVISIONED.name().equals(e.getEventType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("BALANCE_PROVISIONED event not found"));
        assertThat(balanceProvisionedEvent.getStatus()).isEqualTo(OutboxStatus.PENDING);

        // then — PAYEE_CREATED event is now PUBLISHED
        OutboxEvent refreshedPayeeCreatedEvent = outboxRepository.findById(payeeCreatedEvent.getId()).orElseThrow();
        assertThat(refreshedPayeeCreatedEvent.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
    }

    // -------------------------------------------------------------------------
    // Test 2: Idempotency (re-drive) — Balance already exists, no exception,
    //         PAYEE_CREATED still marked PUBLISHED
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    void shouldBeIdempotentWhenBalanceAlreadyExistsOnRedrive() {
        // given — create payee + PAYEE_CREATED event
        UUID externalId = UUID.randomUUID();
        Payee payee = createPayeeCommandService.create(new CreatePayeeCommand(externalId));

        OutboxEvent payeeCreatedEvent = outboxRepository.findAll().stream()
                .filter(e -> OutboxEventType.PAYEE_CREATED.name().equals(e.getEventType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("PAYEE_CREATED event not found"));

        // given — balance already provisioned before processor runs
        provisionBalanceCommandService.provision(new ProvisionBalanceCommand(externalId));

        int outboxCountBefore = outboxRepository.findAll().size();

        // when — process the PAYEE_CREATED event again
        processor.process(payeeCreatedEvent);

        // then — no additional outbox events created (provision was a no-op)
        int outboxCountAfter = outboxRepository.findAll().size();
        assertThat(outboxCountAfter).isEqualTo(outboxCountBefore);

        // then — exactly one Balance row
        assertThat(balanceRepository.findAll()).hasSize(1);

        // then — PAYEE_CREATED event is PUBLISHED
        OutboxEvent refreshedEvent = outboxRepository.findById(payeeCreatedEvent.getId()).orElseThrow();
        assertThat(refreshedEvent.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
    }
}
