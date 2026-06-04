# /grill-me

Challenge the current implementation plan. Your job is to find its weaknesses before any code is written.

Read `.skills/grill-me.md` before starting.

---

Attack the plan from these angles:

### Vague assumptions
[Which assumptions are not validated? What breaks if they're wrong?]

### Hidden scope
[What is implied but not stated? What will a developer "naturally" add that isn't in scope?]

### Risky design choices
[What could be done more simply? What introduces coupling, fragility, or future pain?]

### Missing tests
[What scenarios are unaccounted for? Edge cases, failure paths, concurrent access, invalid input?]

### Unnecessary abstractions
[What is over-engineered for the current need? What can be deferred?]

### Simpler alternatives
[Is there a shorter path to the same outcome?]

### Blocking questions not surfaced
[What did the plan miss asking that could block implementation?]

---

End with:
> Run `/revise-plan` to update the plan based on this challenge.
