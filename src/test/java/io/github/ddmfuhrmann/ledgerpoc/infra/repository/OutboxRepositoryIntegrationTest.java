package io.github.ddmfuhrmann.ledgerpoc.infra.repository;

import io.github.ddmfuhrmann.ledgerpoc.application.event.OutboxEvent;
import io.github.ddmfuhrmann.ledgerpoc.application.event.OutboxStatus;
import io.github.ddmfuhrmann.ledgerpoc.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    // -------------------------------------------------------------------------
    // findOldestPendingPerPayeeSkipLocked — one event per payee (oldest)
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnOneEventPerPayeeWithOldestFirstWhenMultipleEventsExist() throws InterruptedException {
        long payeeA = 10L;
        long payeeB = 20L;

        OutboxEvent payeeA_oldest = pendingEvent(payeeA, 101L, "CashInRequested");
        outboxRepository.saveAndFlush(payeeA_oldest);
        Thread.sleep(2);

        OutboxEvent payeeA_middle = pendingEvent(payeeA, 102L, "CashInConfirmed");
        outboxRepository.saveAndFlush(payeeA_middle);
        Thread.sleep(2);

        OutboxEvent payeeA_newest = pendingEvent(payeeA, 103L, "CashOutRequested");
        outboxRepository.saveAndFlush(payeeA_newest);
        Thread.sleep(2);

        OutboxEvent payeeB_oldest = pendingEvent(payeeB, 201L, "CashInRequested");
        outboxRepository.saveAndFlush(payeeB_oldest);
        Thread.sleep(2);

        OutboxEvent payeeB_newest = pendingEvent(payeeB, 202L, "CashInConfirmed");
        outboxRepository.saveAndFlush(payeeB_newest);

        List<OutboxEvent> result = outboxRepository.findOldestPendingPerPayeeSkipLocked(10);

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(OutboxEvent::getPayeeId)
                .containsExactlyInAnyOrder(payeeA, payeeB);

        OutboxEvent returnedForPayeeA = result.stream()
                .filter(e -> e.getPayeeId().equals(payeeA))
                .findFirst().orElseThrow();
        assertThat(returnedForPayeeA.getId()).isEqualTo(payeeA_oldest.getId());

        OutboxEvent returnedForPayeeB = result.stream()
                .filter(e -> e.getPayeeId().equals(payeeB))
                .findFirst().orElseThrow();
        assertThat(returnedForPayeeB.getId()).isEqualTo(payeeB_oldest.getId());
    }

    @Test
    void shouldRespectBatchSizeAndReturnOnlyOneEventWhenBatchSizeIsOne() throws InterruptedException {
        long payeeA = 30L;
        long payeeB = 40L;

        outboxRepository.saveAndFlush(pendingEvent(payeeA, 301L, "CashInRequested"));
        Thread.sleep(2);
        outboxRepository.saveAndFlush(pendingEvent(payeeB, 401L, "CashInRequested"));

        List<OutboxEvent> result = outboxRepository.findOldestPendingPerPayeeSkipLocked(1);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getStatus()).isEqualTo(OutboxStatus.PENDING);
    }

    @Test
    void shouldNotReturnPublishedEventsWhenAllEventsForPayeeArePublished() {
        long payeeA = 50L;
        long payeeB = 60L;

        OutboxEvent publishedEvent = pendingEvent(payeeA, 501L, "CashInConfirmed");
        outboxRepository.saveAndFlush(publishedEvent);
        publishedEvent.markPublished();
        outboxRepository.saveAndFlush(publishedEvent);

        outboxRepository.saveAndFlush(pendingEvent(payeeB, 601L, "CashOutRequested"));

        List<OutboxEvent> result = outboxRepository.findOldestPendingPerPayeeSkipLocked(10);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getPayeeId()).isEqualTo(payeeB);
        assertThat(result.getFirst().getStatus()).isEqualTo(OutboxStatus.PENDING);
    }

    // -------------------------------------------------------------------------
    // Fixture helpers
    // -------------------------------------------------------------------------

    private OutboxEvent pendingEvent(long payeeId, long aggregateId, String eventType) {
        return new OutboxEvent("PAYMENT", aggregateId, payeeId, eventType, "{\"amount\":100}");
    }
}