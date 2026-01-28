package io.github.ddmfuhrmann.ledgerpoc.infra.repository;

import io.github.ddmfuhrmann.ledgerpoc.domain.*;
import io.github.ddmfuhrmann.ledgerpoc.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class RepositorySmokeTest extends AbstractIntegrationTest {

    @Autowired
    private PayeeRepository payeeRepository;

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private LedgerRepository ledgerRepository;

    @Test
    void shouldPersistAndLoadCoreEntities() {
        // create payee
        Payee payee = new Payee(UUID.randomUUID());
        payeeRepository.save(payee);

        // create balance
        Balance balance = new Balance(payee);
        balanceRepository.save(balance);

        // create payment
        Payment payment = new Payment(
                UUID.randomUUID(),
                payee,
                PaymentType.CASH_IN,
                BigDecimal.valueOf(100)
        );
        paymentRepository.save(payment);

        // create ledger entry
        LedgerEntry entry = new LedgerEntry(
                payee,
                payment,
                LedgerType.CREDIT,
                BigDecimal.valueOf(100)
        );
        ledgerRepository.save(entry);

        // update balance
        balance.credit(BigDecimal.valueOf(100));
        balanceRepository.save(balance);

        // flush & reload
        balanceRepository.flush();

        Balance reloaded = balanceRepository.findById(payee.getId()).orElseThrow();

        assertThat(reloaded.getAvailableAmount())
                .isEqualByComparingTo("100.00");
    }
}