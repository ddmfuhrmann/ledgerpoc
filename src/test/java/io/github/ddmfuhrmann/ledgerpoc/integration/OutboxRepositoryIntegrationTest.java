package io.github.ddmfuhrmann.ledgerpoc.integration;

import io.github.ddmfuhrmann.ledgerpoc.application.event.OutboxEvent;
import io.github.ddmfuhrmann.ledgerpoc.application.event.OutboxStatus;
import io.github.ddmfuhrmann.ledgerpoc.infra.repository.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class OutboxRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private OutboxRepository outboxRepository;

    @Test
    void shouldPersistAndLoadPendingOutboxEvent() {
        OutboxEvent event = new OutboxEvent(
                "PAYMENT",
                1L,
                "CashOutConfirmed",
                "{\"amount\":100}"
        );

        outboxRepository.save(event);

        var pendingEvents =
                outboxRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        assertThat(pendingEvents).hasSize(1);

        OutboxEvent loaded = pendingEvents.getFirst();

        assertThat(loaded.getAggregateType()).isEqualTo("PAYMENT");
        assertThat(loaded.getAggregateId()).isEqualTo(1L);
        assertThat(loaded.getEventType()).isEqualTo("CashOutConfirmed");
        assertThat(loaded.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getPublishedAt()).isNull();
    }

    @Test
    void shouldMarkEventAsPublished() {
        OutboxEvent event = new OutboxEvent(
                "PAYMENT",
                2L,
                "CashInConfirmed",
                "{\"amount\":50}"
        );

        outboxRepository.save(event);

        event.markPublished();
        outboxRepository.save(event);
        outboxRepository.flush();

        OutboxEvent reloaded =
                outboxRepository.findById(event.getId()).orElseThrow();

        assertThat(reloaded.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(reloaded.getPublishedAt()).isNotNull();
    }
}