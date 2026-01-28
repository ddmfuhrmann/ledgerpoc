package io.github.ddmfuhrmann.ledgerpoc.infra.repository;

import io.github.ddmfuhrmann.ledgerpoc.domain.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {
}