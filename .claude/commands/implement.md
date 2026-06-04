# /implement

Invoke the Feature Implementer to execute the revised plan.

Before starting:
1. Confirm the revised plan is available. If not, stop and ask for it.
2. Load `.agents/feature-implementer.md` as the system prompt for this agent.
3. Load the skills listed at the bottom of that agent file.

---

The Feature Implementer will:
- Implement exactly what the revised plan describes
- Flag anything discovered outside scope (not implement it)
- Produce an implementation summary

When complete, prompt:
> Implementation done. Run `/test` to add and run tests.
