# /f-plan

Entry point of the development cycle. Transforms any input (conversation, ticket, PRD, vague idea) into a refined plan via grill-me.

## Procedure

1. Use the native Plan agent (subagent_type: Plan) to explore the codebase and understand the task's impact.
2. Spawn the `notion-agent` to:
   - Search ADRs in `LedgerPOC/ADRs/` (page_id: `3754b4b4-84d1-81a3-a77d-e8ffd5dba920`) that may affect the approach.
   - Search previous Plans in `LedgerPOC/Plans/` (page_id: `3754b4b4-84d1-81a2-b65f-c009a31d5e18`) with overlapping scope.
3. Produce the structured plan with sections:
   - **Understanding** — what the task asks for, in your own words
   - **Assumptions** — what is being treated as true without being explicit
   - **Scope** — what will change (files, layers, behaviors)
   - **Out of scope** — what will explicitly not be done
   - **Approach** — how to implement (concrete: "add method X in class Y that does Z")
   - **Files likely to change** — list of files or directories
   - **Tests needed** — test cases and type (unit/integration/contract/e2e)
   - **Risks** — what could go wrong
   - **Performance criteria** — measurable criteria if any, or "none" if not applicable
   - **Blocking questions** — questions that must be answered before implementing
4. Start the grill-me loop automatically:
   - For each relevant question about the plan, use `AskUserQuestion` with one question at a time.
   - Always include the recommended answer as the first option.
   - If the answer can be found in the codebase, explore the code and answer without asking.
   - Continue until there are no more relevant open questions.
5. When grill-me is complete:
   - Suggest a short title (kebab-case) for the plan and confirm with the user.
   - Save the refined plan locally in `.plans/YYYY-MM-DD-<title>.md` with frontmatter:
     ```
     ---
     date: YYYY-MM-DD
     title: <title>
     notion_url: <url after saving>
     ---
     ```
   - Spawn the `notion-agent` to save the plan in `LedgerPOC/Plans/`.
   - Suggest: `Plan saved. Run /f-implement <title> to continue.`
