Mark a task as complete and move it from workflows/tasks/ to workflows/done/.

## Instructions

1. The argument is the task filename: $ARGUMENTS
2. If no filename is provided:
   - List all files in `workflows/tasks/`
   - Ask the user which one to complete
3. Read the task file from `workflows/tasks/[filename]`
4. If the file does not exist, show an error and list available tasks
5. Review the acceptance criteria:
   - Show all criteria to the user
   - If any are unchecked, ask: "These criteria are not yet checked. Mark as complete anyway?"
   - If the user confirms, check all criteria
6. Update the file content:
   - Change `status: task` to `status: done`
   - Add `completed: [today's date YYYY-MM-DD]` to frontmatter
   - Add an `## Outcome` section at the bottom:
     ```
     ## Outcome

     Completed on [date]. [Ask user for a brief summary of what was done]
     ```
7. Move the file:
   - Keep the original task filename — use it as-is in `workflows/done/`
   - Write the updated content to `workflows/done/[original-filename].md`
   - Delete the original from `workflows/tasks/[filename]`
8. Skill Candidating:
   - Review the task outcome and the work that was done
   - Ask yourself: "Did this task involve domain-specific knowledge — concrete facts,
     patterns, standards, or anti-patterns — that Claude doesn't reliably know on its own?"
   - Apply the skill design principle: methodology (debugging, refactoring, testing) does NOT
     qualify. Only specialized domain knowledge (SQL patterns, security vulnerability classes,
     IaC syntax, WCAG criteria, etc.) qualifies.
   - If YES and the knowledge isn't already covered by an existing skill in `.claude/skills/`:
     - Generate a `.sc` file at `workflows/done/[original-filename].sc` (same stem as the done file)
     - Frontmatter: `domain` — choose the name as follows:
         1. Scan `workflows/done/` for existing `.sc` files and extract their `domain:` values
         2. If an existing domain covers the same technology area, use that **exact name**
         3. Only coin a new name if no existing domain overlaps — use the broad technology/standard,
            NOT a sub-topic: `graphql` not `graphql-schema`, `postgres` not `postgres-indexing`,
            `react` not `react-hooks`; hyphenate multi-word names: `rate-limiting`, `ci-cd`
       `source_task` (the done filename), `date` (today), `keywords` (3-6 trigger words for this domain)
     - Body: `## Extracted Knowledge` with the specific patterns/facts learned,
       and `## Proposed Skill Content` with what a skill file would contain
   - If NO: skip — no `.sc` file needed. Most tasks won't generate one.
   - Also ask: "Did this task reveal that an *existing* skill gave wrong, incomplete, or
     misleading guidance?" This is the negative signal question — symmetric to the one above.
     - If YES: add a `## Failure Modes Observed` section to the `.sc` file (or write a
       standalone note in the Outcome section of the done file) capturing:
       - Which skill fired (if known)
       - What the guidance said or implied
       - What was actually correct
       - The specific condition that made the skill wrong (version, scope, edge case, etc.)
     - Be specific — "the skill was wrong" is not useful. "The skill recommended X but
       this only applies when Y; in our case Z, so the correct approach was W" is useful.
     - This is captured for future skill amendment. It will not automatically update the
       skill today, but it builds the corpus that informs the next synthesis cycle.
9. Confirm completion with:
   - Task title
   - Time from creation to completion (if dates are available)
   - Whether a `.sc` skill candidate was generated (and for which domain)
   - Suggest: "Use `/status` to see the current pipeline overview."
