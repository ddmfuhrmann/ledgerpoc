package io.github.ddmfuhrmann.ledgerpoc.application;

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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ProvisionBalanceCommandServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ProvisionBalanceCommandService service;

    @Autowired
    private PayeeRepository payeeRepository;

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    // -------------------------------------------------------------------------
    // Test 1: Happy path — saves Balance(0) + BALANCE_PROVISIONED event
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    void shouldCreateBalanceAndOutboxEventWhenProvisionIsRequested() {
        // given
        Payee payee = new Payee(UUID.randomUUID());
        payeeRepository.save(payee);

        // when
        service.provision(new ProvisionBalanceCommand(payee.getExternalId()));

        // then — balance exists with availableAmount=0
        Balance savedBalance = balanceRepository.findById(payee.getId()).orElseThrow();
        assertThat(savedBalance.getAvailableAmount()).isEqualByComparingTo("0");

        // then — one BALANCE_PROVISIONED outbox event with status PENDING
        List<OutboxEvent> events = outboxRepository.findAll();
        assertThat(events).hasSize(1);

        OutboxEvent event = events.get(0);
        assertThat(event.getEventType()).isEqualTo(OutboxEventType.BALANCE_PROVISIONED.name());
        assertThat(event.getAggregateType()).isEqualTo("PAYEE");
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
    }

    // -------------------------------------------------------------------------
    // Test 2: Idempotency — second provision is a no-op
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    void shouldBeIdempotentWhenProvisionCalledTwice() {
        // given
        Payee payee = new Payee(UUID.randomUUID());
        payeeRepository.save(payee);

        // when — provision twice
        service.provision(new ProvisionBalanceCommand(payee.getExternalId()));
        service.provision(new ProvisionBalanceCommand(payee.getExternalId()));

        // then — still exactly one Balance row
        List<Balance> balances = balanceRepository.findAll();
        assertThat(balances).hasSize(1);
        assertThat(balances.get(0).getAvailableAmount()).isEqualByComparingTo("0");

        // then — still exactly one BALANCE_PROVISIONED event
        List<OutboxEvent> events = outboxRepository.findAll();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getEventType()).isEqualTo(OutboxEventType.BALANCE_PROVISIONED.name());
    }

    // -------------------------------------------------------------------------
    // Test 3: Payee not found — throws IllegalStateException
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    void shouldThrowIllegalStateExceptionWhenPayeeDoesNotExist() {
        // given — unknown payee external id
        UUID unknownId = UUID.randomUUID();

        // when / then
        assertThatThrownBy(() -> service.provision(new ProvisionBalanceCommand(unknownId)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Payee not found");

        // then — no Balance or event created
        assertThat(balanceRepository.findAll()).isEmpty();
        assertThat(outboxRepository.findAll()).isEmpty();
    }
}
