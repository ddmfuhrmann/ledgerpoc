# Development Workflow — LedgerPOC

> Portuguese version: `docs/workflow.pt-br.md`

## Overview

This workflow is **spec-driven and plan-first**: no code is written without a saved plan. Context is preserved across steps via local `.plans/` files and Notion, and responsibilities are distributed across isolated subagents to prevent context from one step contaminating another.

Key design decisions:

- **`f-` prefix** — avoids collision with native Claude Code skills (`/plan`, `/review`, `/test`).
- **Grill-me as a conversational loop** — integrated into `/f-plan` via `AskUserQuestion`, one question at a time, no separate command.
- **`/f-implement` orchestrates implement + test + correction loop** — no need to invoke `/test` separately.
- **`/f-ship` unifies review + ADR check + handoff** — one command to close the cycle.
- **Integrations isolated in Haiku 4.5** — `notion-agent` and `git-agent` are cheap and focused; domain agents never call Notion or git directly.
- **`caveman` and `karpathy-guidelines` always active** — all agents load these skills by default.

---

## Main Flow

```
[free conversation / ticket / PRD]
         │
    /f-prd  (optional — larger-scope features)
         │
    /f-plan  ──→  [grill-me loop via AskUserQuestion]  ──→  plan saved in .plans/
         │
    /f-implement <title>
         ├──→  [feature-implementer]  (writes production code)
         ├──→  [test-implementer]     (writes and runs tests)
         └──→  auto-correction loop (up to 3x, then checkpoint)
         │
    /f-ship
         ├──→  [git-agent]    (gets diff)
         ├──→  [reviewer]     (audits diff vs plan)
         ├──→  findings grill-me via AskUserQuestion
         ├──→  ADR check via AskUserQuestion
         ├──→  handoff grill-me via AskUserQuestion
         ├──→  [notion-agent] (saves review + ADRs + handoff)
         └──→  [git-agent]    (creates PR)
```

**Flow with optimize:**

```
    /f-implement <title>
         │ (on complete: AskUserQuestion — run /f-optimize?)
         ↓
    /f-optimize <title>
         ├──→  [optimizer]    (baseline → analysis → loop)
         └──→  [notion-agent] (saves report)
         ↓
    /f-ship
```

**Flow with PRD:**

```
    /f-prd
         ├──→  grill-me via AskUserQuestion
         ├──→  saves .plans/YYYY-MM-DD-<title>-prd.md
         └──→  [notion-agent] (saves to LedgerPOC/PRDs/)
         ↓
    /f-plan  (reads the PRD as input)
```

---

## Main Commands

### /f-plan

Entry point of the cycle. Accepts any input (conversation, ticket, PRD, vague idea).

1. Uses the native Plan agent to explore the codebase.
2. Spawns `notion-agent` to look up related ADRs and previous plans.
3. Produces a structured plan with 10 sections: Understanding, Assumptions, Scope, Out of scope, Approach, Files likely to change, Tests needed, Risks, Performance criteria, Blocking questions.
4. Starts automatic grill-me — one question at a time via `AskUserQuestion`.
5. Saves refined plan to `.plans/YYYY-MM-DD-<title>.md` and to Notion.

### /f-implement \<title\>

Orchestrates the full implementation. The argument is the kebab-case plan title.

1. Reads `.plans/YYYY-MM-DD-<title>.md`.
2. Spawns `feature-implementer` (production code).
3. Spawns `test-implementer` (tests).
4. If tests fail: automatic correction loop (up to 3x), then checkpoint via `AskUserQuestion`.
5. On success: asks whether to run `/f-optimize`.

### /f-ship

Closes the cycle. Can be used outside the pipeline to revisit previous deliveries.

1. Spawns `git-agent` to get the diff.
2. Spawns `reviewer` with plan + diff + summaries.
3. Interactive findings grill-me (BLOCKER → fix now, WARNING → defer, SUGGESTION → open issue).
4. ADR check: detects architectural decision candidates, asks whether to register.
5. Handoff grill-me: next step, pending decisions, production risks.
6. Spawns `notion-agent` to save artifacts.
7. Spawns `git-agent` to create PR.

**SonarQube integration (opt-in):** if `sonar-project.properties` exists at the project root, the `reviewer` runs a full SonarQube static analysis before producing the review summary. See [Optional integrations — SonarQube](#sonarqube-via-docker) below.

---

## Complementary Commands

### /f-optimize \<title\>

Plan-driven performance optimization. Behavior determined by the **Performance criteria** field in the plan.

- **With measurable criteria** (e.g. `p95 < 200ms`): optimizer runs autonomously — baseline → analysis → change → re-measurement → loop. Checkpoint every 3 attempts.
- **Without criteria**: optimizer collects baseline and produces findings without applying changes.

In both cases, the report is saved to Notion (`LedgerPOC/Optimizations/`).

### /f-prd

Creates a PRD via conversational grill-me. Use for larger-scope features before `/f-plan`. One question at a time: name, problem, measurable objective, requirements, out of scope, acceptance criteria, blockers.

### /f-sync-patterns

Scans all Java files and rewrites `.skills/patterns.md` with updated canonical snippets. Run after implementing a new pattern that other agents should follow.

---

## Quick Reference

| Command | When to use |
|---|---|
| `/f-prd` | Before plan, for larger-scope features |
| `/f-plan` | Always — entry point of the cycle |
| `/f-implement <title>` | After grill-me completes and plan is saved |
| `/f-ship` | After implement completes successfully |
| `/f-optimize <title>` | Performance — standalone or post-implement |
| `/f-sync-patterns` | After implementing a new relevant pattern |

---

## Agent Structure

| Type | Agent | Model | Responsibility |
|---|---|---|---|
| Orchestrator | `/f-plan`, `/f-implement`, `/f-ship`, `/f-optimize`, `/f-prd` | Sonnet 4.6 (inline) | Coordinates flow, uses AskUserQuestion, never writes code |
| Domain | `feature-implementer` | Sonnet 4.6 | Writes production code |
| Domain | `test-implementer` | Sonnet 4.6 | Writes and runs tests |
| Domain | `reviewer` | Opus 4.8 | Audits diff vs plan |
| Optimizer | `optimizer` | Opus 4.8 | Measurement + optimization loop, evidence-based |
| Integration | `notion-agent` | Haiku 4.5 | All Notion CRUD |
| Integration | `git-agent` | Haiku 4.5 | Branch, commit, PR |

**Isolation principle:** domain agents never call Notion or git directly — they delegate to `notion-agent` and `git-agent` via the orchestrator.

---

## Local Artifacts

| Path | Content |
|---|---|
| `.plans/YYYY-MM-DD-<title>.md` | Refined plan with frontmatter (`date`, `title`, `notion_url`) |
| `.plans/YYYY-MM-DD-<title>-prd.md` | PRD with frontmatter |
| `docs/architecture.md` | System architecture (mandatory reading for agents touching Balance, LedgerEntry, or Payment) |
| `docs/workflow.md` | This document (English) |
| `docs/workflow.pt-br.md` | This document (Português BR) |
| `.skills/patterns.md` | Canonical codebase snippets (updated via `/f-sync-patterns`) |

---

## Optional integrations

### SonarQube via Docker *(experimental)*

> Not yet validated across all stacks and CI configurations. Treat findings as supplementary signal.

The `reviewer` agent supports SonarQube static analysis as an opt-in integration. When active, findings (code smells, bugs, vulnerabilities) are included in the review summary with the same `BLOCKER | WARNING | SUGGESTION` severity labels.

**Activation:** create `sonar-project.properties` at the project root. Its presence is the opt-in signal — the reviewer runs analysis automatically on every `/f-ship`.

**`sonar-project.properties` — project identity only (safe to commit):**

```properties
sonar.projectKey=ledgerpoc
sonar.sources=src
sonar.exclusions=**/test/**
```

Do not add `sonar.host.url` or `sonar.token` to this file. The skill injects them as CLI overrides at runtime so the file is safe for CI pipelines that use their own host and token. A GitHub Actions SonarQube/SonarCloud step passes its own `-D` flags and will not be affected.

**Setup: Docker only.** No CLI installation, no manual token setup.

On first use the skill starts `bsdd-sonarqube`, auto-generates a token via the SonarQube API, and saves it to `.bsdd-sonar-token` (gitignored). Subsequent reviews reuse the stored token. The scanner runs as an ephemeral `sonarsource/sonar-scanner-cli` container on the shared `bsdd-sonar-net` network. If SonarQube fails to become healthy within 120s, the review is blocked with an explicit error.

**Severity mapping:**

| Sonar severity | Sonar type | Reviewer label |
|---|---|---|
| BLOCKER / CRITICAL | any | `BLOCKER` |
| MAJOR | BUG / VULNERABILITY | `BLOCKER` |
| MAJOR | CODE_SMELL | `WARNING` |
| MINOR / INFO | any | `SUGGESTION` |
| Security hotspot | any | `WARNING` |

Only issues in files present in the current diff are surfaced. Project-wide findings outside the changeset are ignored.
