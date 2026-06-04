# /f-sync-patterns

Scans the codebase and updates `.skills/patterns.md` with the latest canonical patterns.

## When to use

After implementing a new relevant pattern that other agents should follow. Prevents `.skills/patterns.md` from going stale.

## Procedure

1. Read all Java files in `src/main/java/` and `src/test/java/`.
2. Identify recurring patterns in:
   - JPA entities (protected constructor, public constructor, domain behavior, getters)
   - Domain behavior (state transitions, guard clauses, private helpers)
   - CommandService (@Service, @Transactional, outbox pattern)
   - Processors (@Component, idempotency, delegate to private methods)
   - Integration tests (AbstractIntegrationTest, given/when/then, fixtures)
3. Rewrite `.skills/patterns.md` with real, up-to-date snippets from the codebase.
4. Confirm: `patterns.md updated with X patterns.`
