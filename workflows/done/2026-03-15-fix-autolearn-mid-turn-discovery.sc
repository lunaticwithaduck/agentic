---
domain: copilot-infrastructure
source_task: 2026-03-15-fix-autolearn-mid-turn-discovery.md
date: 2026-03-23
keywords: copilot, autolearn, inline synthesis, mid-turn, prompt override
---

## Extracted Knowledge

**Copilot requires inline synthesis; cursor/claude use deferred hook injection:**
- Copilot's hook architecture (UserPromptSubmit via hooks.json) does not reliably inject
  synthesis instructions mid-turn the way `skill-detector.cjs` does for Claude Code.
- When `.github/autolearn-pending` is written during a `/complete` run, Copilot agents
  have no way to get triggered automatically for synthesis in the same session without
  explicit documentation in the always-on instructions.
- Fix: copilot's `/complete` override synthesizes the skill *inline* at threshold
  (count ≥ 3) rather than writing `autolearn-pending` and waiting.

**The copilot-commands override pattern:**
- `buildScripts/src/copilot-commands/` holds copilot-specific overrides for any command
  that diverges from the shared `.claude/commands/` source.
- Build step 9 converts all shared commands to `.prompt.md`; step 10 applies overrides
  (same filenames, different content). Override wins.
- Only override what diverges — shared behavior stays in `.claude/commands/`.

**autolearn-pending must be documented in always-on context:**
- For copilot: `copilot-instructions.md` is always in context. This is the only reliable
  place to document mid-turn autolearn behavior. Prompt files (`.prompt.md`) are only
  active when invoked.
- For cursor/claude: `agent-instructions.mdc` and `CLAUDE.md` serve the same always-on role.
  Neither currently documents `autolearn-pending` — gap exists.

## Proposed Skill Content

A `copilot-infrastructure` skill would cover:
- When to add command overrides vs. modify shared source
- The always-on context hierarchy (copilot-instructions.md > workflow-gate > prompts)
- Autolearn pipeline behavior differences per platform
- Build pipeline: step 9 (shared) vs step 10 (overrides) ordering guarantee
- Platform detection in hooks: `.github/` vs `.claude/` vs `.cursor/` `__dirname` check
