# /plan

Transform the raw input into a short, structured implementation plan.

The input may be a PM ticket, a GitHub issue, a Slack message, a vague description, or a conversation. Format does not matter — extract the intent.

Read `.skills/plan-first-development.md` before producing the plan.

---

## Antes de produzir o plano

Consultar o Notion para evitar conflitos com decisões e trabalho já feito:

1. **ADRs** — buscar em `LedgerPOC/ADRs/` (page_id: `3754b4b4-84d1-81a3-a77d-e8ffd5dba920`) decisões arquiteturais que possam afetar o escopo ou a abordagem desta tarefa. Se houver ADR relevante, citá-lo na seção **Risks** ou **Assumptions**.

2. **Plans anteriores** — buscar em `LedgerPOC/Plans/` (page_id: `3754b4b4-84d1-81a2-b65f-c009a31d5e18`) planos com escopo sobreposto ao desta tarefa. Se houver sobreposição, nota-la explicitamente na seção **Understanding**.

Se não houver ADRs ou Plans ainda, prosseguir normalmente sem bloquear.

---

Produce the plan in this exact structure:

## Implementation Plan

**Input summary:** [one sentence describing what was given]

### Understanding
[What this task is actually asking for, in your own words. Be specific.]

### Assumptions
[Things you're treating as true that weren't explicitly stated. Each assumption is a risk.]

### Scope
[What will change. Be specific: files, layers, behaviors.]

### Out of scope
[What is explicitly not included in this task. Be specific.]

### Approach
[How you'll implement it. Be concrete — not "we'll add a service" but "we'll add X method to Y class that does Z".]

### Files likely to change
[List of files or directories. If unknown, say so.]

### Tests needed
[What test cases will be required. Which test type (unit/integration/contract/e2e) and why.]

### Risks
[What could go wrong. Include integration points, data migration risk, breaking changes.]

### Blocking questions
[Questions that must be answered before implementation can start. If none, say "None — ready to proceed."]

---

After producing the plan, prompt:
> Ready for `/grill-me` to challenge this plan, or do you want to adjust anything first?
