# /optimize

Spawn the `optimizer` subagent. Use only when performance, query efficiency, throughput, latency, or resource usage is an explicit concern.

Do NOT invoke this command for general code quality or stylistic improvements.

Before spawning:
1. Confirm there is a specific, measurable performance concern. If not, stop.
2. Confirm the local environment can be started (Docker, migrations, seed data).
3. Spawn the `optimizer` subagent with a prompt that includes:
   - Full contents of `.current-plan.md`
   - Description of the specific performance concern (query, endpoint, component)
   - Instruction to read `CLAUDE.md` and the skills listed in the agent file before starting

The subagent will:
- Establish a baseline measurement before any change
- Apply one change at a time
- Measure after each change
- Report trade-offs and recommend: APPLY / APPLY WITH MONITORING / DEFER / REJECT

When complete:
> Run `/review` again if significant code changed, or `/handoff` if already reviewed.
