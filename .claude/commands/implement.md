# /implement

Spawn the `feature-implementer` subagent to execute the revised plan.

Before spawning:
1. Confirm `.current-plan.md` exists. If not, stop and ask for it.
2. Spawn the `feature-implementer` subagent with a prompt that includes:
   - Full contents of `.current-plan.md`
   - Instruction to read `CLAUDE.md` and the skills listed in the agent file before starting

The subagent will:
- Implement exactly what the revised plan describes
- Flag anything discovered outside scope (not implement it)
- Compile to verify no errors
- Produce an implementation summary

When complete, prompt:
> Implementation done. Run `/test` to add and run tests.
