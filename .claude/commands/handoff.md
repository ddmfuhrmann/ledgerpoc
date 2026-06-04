# /handoff

Generate a PR summary based on all previous step outputs.

Collect:
- Revised plan
- Implementation summary (Feature Implementer)
- Test summary (Test Implementer)
- Review verdict (Reviewer)
- Optimization report (Optimizer, if run)

---

Produce:

## PR Summary

**Title:** [short, imperative — what this PR does]

### What changed
[Plain language description of what was implemented. No jargon.]

### Why
[The problem or need this addresses, from the original input.]

### How to test
[Steps to manually verify the change works. Be specific.]

### Tests
- [ ] [Test case 1]
- [ ] [Test case 2]
- Passed: [X] / Failed: [Y]

### Risks
[Anything that could go wrong in production. Include rollback notes if applicable.]

### Performance notes
[If Optimizer ran: summary of changes and measurements. If not: "No performance work done."]

### Rollback
[How to revert this change safely if needed.]

### Out of scope (deferred)
[Items from grill-me or review flagged as future considerations.]

---

Após produzir o PR Summary, salvar no Notion:
- Criar sub-página em `LedgerPOC/Handoffs/` usando o template Handoff de `.skills/notion-docs.md`.
- Confirmar com o link da página criada.
