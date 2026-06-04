# /f-implement

Orchestrates the full implementation loop: code + tests, with automatic correction retries.

## Inputs

- Plan title as argument: `/f-implement cash-out-processor`
- No argument: infer title from conversation context (most recently saved plan).

## Procedure

1. Locate `.plans/YYYY-MM-DD-<title>.md` and read it.
2. Spawn the `feature-implementer` with the full plan content.
3. Read the implementation summary produced by the `feature-implementer`:
   - If **Deviations** is non-empty, note the delta explicitly.
   - Spawn the `test-implementer` with the plan + implementation summary, highlighting deviations so tests verify the actual implementation, not the original plan.
4. If tests pass: go to step 8.
5. If tests fail, record the error signature (first error message + location). Then:
   - **Circuit breaker:** if the error signature matches the previous attempt, abort immediately — do not consume remaining retries. Go to step 6.
   - Spawn the `feature-implementer` again with the plan + test failure output to fix it.
   - Spawn the `test-implementer` again.
   - Repeat up to 3 automatic retries.
6. If tests fail on the 4th attempt or the circuit breaker fires: checkpoint via `AskUserQuestion`:
   - "Try 3 more times" (Recommended)
   - "I want to intervene now"
   - "Abandon and show current state"
   - If "Try 3 more times": reset counter and go back to step 5.
   - If "I want to intervene now": stop and present current state (last failure output).
   - If "Abandon": present current state and stop.
8. On success: use `AskUserQuestion` to ask:
   - "Run /f-optimize now?" (Recommended: no, go straight to /f-ship)
   - "Yes, run /f-optimize"
   - "No, go to /f-ship"
9. Suggest: `Implementation complete. Run /f-ship to review and hand off.`
