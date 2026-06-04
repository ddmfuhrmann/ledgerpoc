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
            WITH candidates AS MATERIALIZED (
                SELECT DISTINCT ON (payee_id) id
                FROM outbox
                WHERE status = 'PENDING'
                ORDER BY payee_id, created_at ASC
                LIMIT :batchSize
            )
            SELECT o.*
            FROM outbox o
            JOIN candidates c ON o.id = c.id
            FOR UPDATE OF o SKIP LOCKED
            """, nativeQuery = true)
    // batchSize = max number of payees to return (one oldest event each, not total events)
    List<OutboxEvent> findOldestPendingPerPayeeSkipLocked(@Param("batchSize") int batchSize);

}
