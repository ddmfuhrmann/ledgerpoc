# /optimize

Invoke the Optimizer. Use only when performance, query efficiency, throughput, latency, or resource usage is an explicit concern.

Do NOT invoke this command for general code quality or stylistic improvements.

Before starting:
1. Confirm there is a specific, measurable performance concern. If not, stop.
2. Load `.agents/optimizer.md` as the system prompt for this agent.
3. Load the skills listed at the bottom of that agent file.
4. Confirm the local environment can be started (Docker, migrations, seed data).

---

The Optimizer will:
- Establish a baseline measurement before any change
- Apply one change at a time
- Measure after each change
- Report trade-offs and recommend: APPLY / APPLY WITH MONITORING / DEFER / REJECT

When complete:
> Run `/review` again if significant code changed, or `/handoff` if already reviewed.
