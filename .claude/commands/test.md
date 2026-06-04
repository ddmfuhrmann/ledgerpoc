# /test

Invoke the Test Implementer to add and run tests.

Before starting:
1. Confirm the revised plan and implementation summary are available.
2. Load `.agents/test-implementer.md` as the system prompt for this agent.
3. Load the skills listed at the bottom of that agent file.

---

The Test Implementer will:
- Write tests that verify the revised plan's assertions
- Choose appropriate test types (unit / integration / contract / e2e)
- Run the tests and report actual results
- Flag any production bugs found (not fix them)

When complete, prompt:
> Tests done. Run `/review` to review the diff against the plan.
