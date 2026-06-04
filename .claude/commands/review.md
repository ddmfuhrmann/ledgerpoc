# /review

Invoke the Reviewer to audit the diff against the revised plan and project guidelines.

Before starting:
1. Confirm the revised plan, implementation summary, and test summary are available.
2. Load `.agents/reviewer.md` as the system prompt for this agent.
3. Run `git diff main` (or equivalent) and provide the output to the Reviewer.
4. Load the skills listed at the bottom of that agent file.

---

The Reviewer will:
- Check plan coverage (is everything in scope implemented?)
- Check for scope creep (is anything outside the plan present?)
- Check guideline compliance
- Severity-label all findings: BLOCKER / WARNING / SUGGESTION
- Produce a verdict: APPROVE / APPROVE WITH WARNINGS / REQUEST CHANGES

If verdict is APPROVE or APPROVE WITH WARNINGS:
> Run `/handoff` to generate the PR summary.

If verdict is REQUEST CHANGES:
> Address BLOCKER findings, then re-run `/review`.
