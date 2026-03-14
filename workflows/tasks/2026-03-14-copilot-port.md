---
title: Copilot Port (ship/copilot/)
created: 2026-03-14
status: backlog
---

## Goal

Ship a third distribution target `ship/copilot/` for GitHub Copilot in VS Code (agent mode).

## Context

Parity analysis (2026-03-14) shows ~80% feature parity with Claude Code is achievable via
file-based config — no GitHub App, no VS Code extension required.

Key finding: VS Code natively reads `.claude/settings.json` as a hook source and `CLAUDE.md`
as project instructions. The hook event model is near-identical to Claude Code.

## What Maps Over

| Feature | Source | Target |
|---------|--------|--------|
| CLAUDE.md | `.claude/` | `.github/copilot-instructions.md` — but VS Code reads CLAUDE.md natively |
| Hooks | `.claude/settings.json` | `.github/hooks/*.json` — VS Code also reads `.claude/settings.json` |
| Commands | `.claude/commands/*.md` | `.github/prompts/*.prompt.md` (frontmatter needed) |
| Agents | `.claude/agents/*.md` | `.github/agents/*.agent.md` — VS Code reads `.claude/agents/` directly |
| Skills | `.claude/skills/*.md` | `.github/skills/<name>/SKILL.md` (per-skill directories) |
| Workflow pipeline | `workflows/` | Same — just files |

## Engineering Work

1. **Skill format conversion** — flat `.md` → per-skill directory `<name>/SKILL.md`
   (analogous to `buildScripts/lib/convert-skill.js` for Cursor)
2. **Command conversion** — add frontmatter (`description`, `agent`, `tools`, `model`)
3. **Hook adaptation** — `UserPromptSubmit` skill injection via `additionalContext`;
   add `.github/` path variant to `autolearn-pending` flag in platform-aware hooks
4. **`build-copilot.sh`** — new build script in `buildScripts/lib/`
5. **`build.sh` update** — add copilot target
6. **`setup.sh`** — Copilot-specific setup instructions
7. **PLATFORM-PARITY.md** — add Copilot column
8. **Suite 01** — add copilot structure checks

## Open Questions

- Deterministic skill injection (port `skill-detector.cjs` as a `UserPromptSubmit` hook)
  vs relying on Copilot's probabilistic model-selection of `.github/skills/`? Recommend
  deterministic — same approach as Claude Code, validated by benchmark.
- `skill-index` equivalent for Copilot — likely a `.github/copilot-instructions.md`
  section listing available skills + keywords.

## Estimated Effort

3–5 days. Comparable to the Cursor port.

## Acceptance Criteria

- [ ] `ship/copilot/` directory builds cleanly via `bash buildScripts/build.sh`
- [ ] All hooks fire correctly in VS Code agent mode
- [ ] Skill detection works (deterministic path)
- [ ] Commands available as `/` prompt files
- [ ] Suite 01 infrastructure checks pass for copilot target
- [ ] PLATFORM-PARITY.md updated with Copilot column
