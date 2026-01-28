package io.github.ddmfuhrmann.ledgerpoc.infra.repository;

import io.github.ddmfuhrmann.ledgerpoc.domain.BalanceReplica;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BalanceReplicaRepository extends JpaRepository<BalanceReplica, Long> {
}