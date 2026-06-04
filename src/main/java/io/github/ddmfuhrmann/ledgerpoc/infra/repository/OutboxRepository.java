package io.github.ddmfuhrmann.ledgerpoc.infra.repository;

import io.github.ddmfuhrmann.ledgerpoc.application.event.OutboxEvent;
import io.github.ddmfuhrmann.ledgerpoc.application.event.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);

    @Query(value = """
            SELECT DISTINCT ON (payee_id) *
            FROM outbox
            WHERE status = 'PENDING'
            ORDER BY payee_id, created_at ASC
            FOR UPDATE SKIP LOCKED
            LIMIT :batchSize
            """, nativeQuery = true)
    List<OutboxEvent> findOldestPendingPerPayeeSkipLocked(@Param("batchSize") int batchSize);

}