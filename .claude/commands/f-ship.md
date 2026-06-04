# /f-ship

Unifies review, ADR check, and handoff into a single conversational flow. Triggered manually after `/f-implement` completes.

## Procedure

1. Spawn the `git-agent` to get the diff (`git diff main`).
2. Identify the related plan (from conversation context or ask).
3. Spawn the `reviewer` with: plan content + diff + implementation summary + test summary.
4. For each relevant finding from the reviewer, use `AskUserQuestion`:
   - Present the finding and ask for action: "Fix now" / "Defer" / "Open issue" (Recommended varies by severity: BLOCKER → fix, WARNING → defer, SUGGESTION → open issue).
   - If "Fix now": spawn the `feature-implementer` to apply the fix.
5. ADR check — analyze the diff for architectural decisions that deviate from the standard or are hard to reverse. If candidates are found:
   - Present via `AskUserQuestion`: "Register all" / "Choose which ones" / "None for now".
   - If registering: spawn the `notion-agent` to create ADR(s).
6. Handoff grill-me — use `AskUserQuestion` to collect context:
   - "What is the next step after this delivery?" (options based on context)
   - "Are there pending decisions that should be recorded?"
   - "Are there known risks for production?"
7. Spawn the `notion-agent` to save: review summary + ADRs (if any) + handoff doc in `LedgerPOC/Handoffs/`.
8. Spawn the `git-agent` to create the PR with the generated summary.
9. Confirm with the created links.
10. Write the file `.claude/.last-shipped` with the plan title in kebab-case (e.g.: `cashin-flow`). This triggers the automatic hook that updates the roadmap in Notion after stop.
