---
date: 2026-06-04
title: cashin-flow
notion_url: https://app.notion.com/p/Ledger-POC-2f54b4b484d1808680c1d722aa9e2f58
---

# Plan: CashIn flow

## Understanding
CashOut é o template estrutural. CashIn é mais simples: `balance.credit()` não tem caminho de falha de negócio (sem "fundos insuficientes"). Todos os tipos necessários já existem no codebase (`PaymentType.CASH_IN`, `LedgerType.CREDIT`, `OutboxEventType.CASH_IN_*`, todos os três payloads CashIn). Um bug pré-existente em `CashInFailedPayload` é corrigido neste incremento.

## Assumptions
- Balance **deve pré-existir** — mesma regra do CashOut. Sem auto-criação. Payee sem Balance recebe `IllegalStateException("Balance not found")`.
- `balance.credit()` sem caminho de falha de negócio. `IllegalArgumentException` (amount inválido) propagado sem catch, deixando payment preso em `REQUESTED`. Aceitável para POC sem dispatcher.
- `CASH_IN_FAILED` event type existe para uso futuro — não emitido pelo processor neste incremento.
- `OptimisticLockException` (de `@Version`) não tratado — rollback em conflito concorrente. Aceitável para POC.

## Scope
| Arquivo | Ação |
|---|---|
| `application/event/payload/CashInFailedPayload.java` | Fix: `eventType()` retorna `CASH_OUT_FAILED` → deve ser `CASH_IN_FAILED` |
| `application/command/RequestCashInCommand.java` | Criar — record DTO: payeeExternalId, paymentExternalId, amount |
| `application/CashInCommandService.java` | Criar — mirror do CashOutCommandService, tipos CASH_IN |
| `application/CashInRequestedProcessor.java` | Criar — lógica inline no `process()`, sem helpers privados |
| `test/.../CashInCommandServiceIntegrationTest.java` | Criar |
| `test/.../CashInRequestedProcessorIntegrationTest.java` | Criar |

## Out of scope
- Auto-criação de Balance
- Emissão de `CASH_IN_FAILED`
- SQS dispatcher, BalanceReplica
- Testes de concorrência / serialização por payee
- Casos "Payee not found" e "Payment not found" (consistente com padrão CashOut)

## Approach

### CashInCommandService.request() — @Transactional
1. `payeeRepository.findByExternalId(command.payeeExternalId())` → `IllegalArgumentException("Payee not found")` se ausente
2. `new Payment(command.paymentExternalId(), payee, PaymentType.CASH_IN, command.amount())`
3. `paymentRepository.save(payment)`
4. `CashInRequestedPayload(payment.getId(), payee.getId(), command.amount())`
5. `OutboxEventType.CASH_IN_REQUESTED.toEvent(payment.getId(), jsonSerializer.serialize(payload))`
6. `outboxRepository.save(event)`

### CashInRequestedProcessor.process() — @Transactional (lógica inline)
```java
Payment payment = paymentRepository.findById(event.getAggregateId())
        .orElseThrow(() -> new IllegalStateException("Payment not found"));
if (payment.getStatus() != PaymentStatus.REQUESTED) { return; }
CashInRequestedPayload payload = jsonSerializer.deserialize(event.getPayload(), CashInRequestedPayload.class);
Balance balance = balanceRepository.findById(payload.payeeId())
        .orElseThrow(() -> new IllegalStateException("Balance not found"));
balance.credit(payload.amount());
ledgerRepository.save(new LedgerEntry(payment.getPayee(), payment, LedgerType.CREDIT, payload.amount()));
payment.confirm();
outboxRepository.save(OutboxEventType.CASH_IN_CONFIRMED.toEvent(
        payment.getId(),
        jsonSerializer.serialize(new CashInConfirmedPayload(payload.paymentId(), payload.payeeId(), payload.amount()))
));
```

## Files likely to change
- `src/main/java/io/github/ddmfuhrmann/ledgerpoc/application/event/payload/CashInFailedPayload.java`
- `src/main/java/io/github/ddmfuhrmann/ledgerpoc/application/command/RequestCashInCommand.java`
- `src/main/java/io/github/ddmfuhrmann/ledgerpoc/application/CashInCommandService.java`
- `src/main/java/io/github/ddmfuhrmann/ledgerpoc/application/CashInRequestedProcessor.java`
- `src/test/java/io/github/ddmfuhrmann/ledgerpoc/application/CashInCommandServiceIntegrationTest.java`
- `src/test/java/io/github/ddmfuhrmann/ledgerpoc/application/CashInRequestedProcessorIntegrationTest.java`

## Tests needed
5 integration tests — Testcontainers + Postgres real, fixture setup explícito:

1. **[CommandService] Happy path** — `request()` persiste `Payment(CASH_IN, REQUESTED)` + outbox `CASH_IN_REQUESTED` com payload correto (paymentId, payeeId, amount)
2. **[Processor] Happy path** — `Balance(0)` → payment=`CONFIRMED`, `availableAmount=100`, `LedgerEntry(CREDIT, 100)`, outbox `CASH_IN_CONFIRMED` com payload verificado
3. **[Processor] Idempotência CONFIRMED** — nenhum evento novo criado, balance inalterado, sem nova LedgerEntry
4. **[Processor] Idempotência FAILED** — nenhum evento novo criado, balance inalterado, sem nova LedgerEntry
5. **[Processor] Balance not found** → `IllegalStateException("Balance not found")`

## Risks
- Payment preso em `REQUESTED` se `credit()` lançar `IllegalArgumentException` — rollback, sem estado terminal, sem `CASH_IN_FAILED`, sem retry. Aceitável para POC.
- Balance como pré-condição hard — novo payee sem Balance terá CashIn rejeitado. Provisioning é feature separada.
- Bug em `CashInFailedPayload` sem impacto runtime hoje — mencionar explicitamente no PR description.

## Performance criteria
None.

## Blocking questions
Nenhuma — todas resolvidas.
