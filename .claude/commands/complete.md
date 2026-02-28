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
   - Write the updated content to `workflows/done/[filename]`
   - Delete the original from `workflows/tasks/[filename]`
8. Skill Candidating:
   - Review the task outcome and the work that was done
   - Ask yourself: "Did this task involve domain-specific knowledge — concrete facts,
     patterns, standards, or anti-patterns — that Claude doesn't reliably know on its own?"
   - Apply the skill design principle: methodology (debugging, refactoring, testing) does NOT
     qualify. Only specialized domain knowledge (SQL patterns, security vulnerability classes,
     IaC syntax, WCAG criteria, etc.) qualifies.
   - If YES and the knowledge isn't already covered by an existing skill in `.claude/skills/`:
     - Generate a `.sc` file at `workflows/done/[same-base-filename].sc`
     - Frontmatter: `domain` (canonical lowercase name), `source_task` (the done filename),
       `date` (today), `keywords` (3-6 trigger words for this domain)
     - Body: `## Extracted Knowledge` with the specific patterns/facts learned,
       and `## Proposed Skill Content` with what a skill file would contain
   - If NO: skip — no `.sc` file needed. Most tasks won't generate one.
9. Confirm completion with:
   - Task title
   - Time from creation to completion (if dates are available)
   - Whether a `.sc` skill candidate was generated (and for which domain)
   - Suggest: "Use `/status` to see the current pipeline overview."
