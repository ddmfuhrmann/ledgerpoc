package io.github.ddmfuhrmann.ledgerpoc.infra.repository;

import io.github.ddmfuhrmann.ledgerpoc.domain.Payee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PayeeRepository extends JpaRepository<Payee, Long> {
    Optional<Payee> findByExternalId(UUID externalId);
}