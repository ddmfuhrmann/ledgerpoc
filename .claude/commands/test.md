# /test

Spawn the `test-implementer` subagent to write and run tests.

Before spawning:
1. Confirm `.current-plan.md` exists and the implementation summary from the Feature Implementer is available.
2. Spawn the `test-implementer` subagent with a prompt that includes:
   - Full contents of `.current-plan.md`
   - The implementation summary (files changed, plan coverage, deviations)
   - Instruction to read `CLAUDE.md` and the skills listed in the agent file before starting

The subagent will:
- Write tests that verify the revised plan's assertions
- Choose appropriate test types (unit / integration / contract / e2e)
- Run the tests and report actual results
- Flag any production bugs found (not fix them)

When complete, prompt:
> Tests done. Run `/review` to review the diff against the plan.
