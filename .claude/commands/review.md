# /review

Spawn the `reviewer` subagent to audit the diff against the revised plan and project guidelines.

Before spawning:
1. Confirm `.current-plan.md`, the implementation summary, and the test summary are available.
2. Run `git diff main` and capture the output.
3. Spawn the `reviewer` subagent with a prompt that includes:
   - Full contents of `.current-plan.md`
   - The git diff output
   - The implementation summary
   - The test summary
   - Instruction to read `CLAUDE.md` and the skills listed in the agent file before starting

The subagent will:
- Check plan coverage (is everything in scope implemented?)
- Check for scope creep (is anything outside the plan present?)
- Check guideline compliance
- Severity-label all findings: BLOCKER / WARNING / SUGGESTION
- Produce a verdict: APPROVE / APPROVE WITH WARNINGS / REQUEST CHANGES

If verdict is APPROVE or APPROVE WITH WARNINGS:
> Run `/handoff` to generate the PR summary.

If verdict is REQUEST CHANGES:
> Address BLOCKER findings, then re-run `/review`.
