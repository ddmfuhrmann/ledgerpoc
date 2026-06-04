package io.github.ddmfuhrmann.ledgerpoc.application;

import io.github.ddmfuhrmann.ledgerpoc.application.event.OutboxEvent;
import io.github.ddmfuhrmann.ledgerpoc.application.event.OutboxEventType;
import io.github.ddmfuhrmann.ledgerpoc.application.event.OutboxStatus;
import io.github.ddmfuhrmann.ledgerpoc.application.event.payload.BalanceProvisionedPayload;
import io.github.ddmfuhrmann.ledgerpoc.application.support.JsonSerializer;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BalanceProvisionedProcessorIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private BalanceProvisionedProcessor processor;

    @Autowired
    private PayeeRepository payeeRepository;

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private JsonSerializer jsonSerializer;

    // -------------------------------------------------------------------------
    // Test 1: Happy path — event status transitions from PENDING to PUBLISHED
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    void shouldMarkBalanceProvisionedEventPublishedWhenProcessed() {
        // given — create payee + balance
        Payee payee = new Payee(UUID.randomUUID());
        payeeRepository.save(payee);

        Balance balance = new Balance(payee);
        balanceRepository.save(balance);

        // given — manually save a BALANCE_PROVISIONED event in PENDING state
        BalanceProvisionedPayload payload = new BalanceProvisionedPayload(payee.getExternalId());
        OutboxEvent event = OutboxEventType.BALANCE_PROVISIONED.toEvent(
                payee.getId(),
                payee.getId(),
                jsonSerializer.serialize(payload)
        );
        outboxRepository.save(event);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);

        // when
        processor.process(event);

        // then — event status is PUBLISHED
        OutboxEvent savedEvent = outboxRepository.findById(event.getId()).orElseThrow();
        assertThat(savedEvent.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
    }
}
