---
title: Fix autolearn mid-turn discovery — copilot instructions + complete inline synthesis
created: 2026-03-15
completed: 2026-03-23
status: done
---

## Goal

When an agent writes a .sc file during /complete and threshold is met, post-write.cjs
sets autolearn-pending in the same turn. The agent may notice the file mid-turn but has
no context about what it means (not documented in copilot-instructions.md), leading to
investigation instead of synthesis.

Two fixes for copilot. Check cursor/claude for same issue — report only, no edits
without approval.

## Steps

- [x] Add autolearn-pending explanation + action to copilot-instructions.md source
- [x] Create copilot-specific complete.md override: synthesize inline when threshold met
- [x] Check cursor (agent-instructions.mdc) and claude (CLAUDE.md ship) for same gap
- [x] Rebuild ships
- [x] Report cursor/claude findings for approval

## Outcome

Completed on 2026-03-23. Steps 1-2 done in session 5 (2026-03-15); steps 3-5 done in session 6 (2026-03-23).

**Copilot fixes (done):**
- `buildScripts/src/copilot-rules/copilot-instructions.md`: Added full autolearn-pending section explaining mid-turn discovery and requiring immediate inline synthesis (not deferred).
- `buildScripts/src/copilot-commands/complete.md` (new override): Step 8 synthesizes the skill inline when count ≥ 3, creating `.github/skills/[domain]/SKILL.md` immediately rather than writing `autolearn-pending` and waiting.
- Build succeeded: all 3 ship targets regenerated (34 / 27 / 34 files).

**Cursor/claude findings (reported for approval):**

*Cursor (`buildScripts/src/cursor-rules/agent-instructions.mdc`):*
- `agent-instructions.mdc` has **no mention of `autolearn-pending`**.
- `cursor-commands/complete.md` uses the deferred approach (writes `.cursor/autolearn-pending`, relies on `cursor-skill-injector.cjs` to inject synthesis on next prompt). This is the intended cursor architecture.
- Gap: if an agent encounters `.cursor/autolearn-pending` mid-turn, it has no guidance — it will investigate rather than ignore/proceed.
- Suggested fix: 3-line note in `agent-instructions.mdc`: "if you see `.cursor/autolearn-pending`, the synthesis hook will fire on your next prompt — do not investigate it, continue your current task."

*Claude Code (`ship/claude-code/CLAUDE.md` / source `CLAUDE.md`):*
- CLAUDE.md (shipped) has **no mention of `autolearn-pending`**.
- Source `.claude/commands/complete.md` also uses deferred approach (write `autolearn-pending`, wait for `skill-detector.cjs` to inject on next UserPromptSubmit).
- Gap: same mid-turn discovery confusion.
- Suggested fix: same 3-line note — "if you encounter `.claude/autolearn-pending`, synthesis instructions will be injected by the hook on your next prompt."

**Decision needed:** Apply the cursor/claude notes? They're small (3 lines each) and close a real gap.
