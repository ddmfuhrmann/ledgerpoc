# /f-sync-patterns

Escaneia o codebase e atualiza `.skills/patterns.md` com os padrões canônicos mais recentes.

## Quando usar

Após implementar um padrão novo relevante que outros agentes devem seguir. Evita que `.skills/patterns.md` fique stale.

## Procedure

1. Ler todos os arquivos Java em `src/main/java/` e `src/test/java/`.
2. Identificar padrões recorrentes em:
   - Entities JPA (constructor protegido, constructor público, domain behavior, getters)
   - Domain behavior (state transitions, guard clauses, private helpers)
   - CommandService (@Service, @Transactional, outbox pattern)
   - Processors (@Component, idempotência, delegate para privados)
   - Integration tests (AbstractIntegrationTest, given/when/then, fixtures)
3. Reescrever `.skills/patterns.md` com snippets reais e atualizados do codebase.
4. Confirmar: `patterns.md atualizado com X padrões.`
