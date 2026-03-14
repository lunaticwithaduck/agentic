---
domain: copilot-infrastructure
task: 2026-03-14-copilot-build-infrastructure
date: 2026-03-14
---

## Extracted Knowledge

### Copilot hook protocol matches Claude Code, not Cursor
GitHub Copilot uses the same stdin/stdout JSON protocol as Claude Code:
- Input: `{"prompt": "..."}` for UserPromptSubmit
- Event names: `UserPromptSubmit`, `PreToolUse`, `PostToolUse`, `Stop`
- Hooks registered in `.github/hooks/hooks.json` with `{"hooks": {"UserPromptSubmit": [{"type": "command", "command": "..."}]}}`
- Cursor uses different event names: `sessionStart`, `afterFileEdit`, `beforeShellExecution`, `stop`
- Copilot error output format: plain text (same as Claude Code), NOT JSON `{permission: "deny"}` (Cursor format)

### Copilot skill file layout is directory-based
Unlike Claude Code (flat `.claude/skills/<name>.md`) and Cursor (flat `.cursor/rules/<name>.mdc`),
Copilot skills use a directory structure: `.github/skills/<name>/SKILL.md`.
The extra directory level allows co-locating skill metadata (fixtures, tests) in future.

### Copilot instruction file formats
- `.instructions.md` files require `applyTo:` frontmatter (e.g., `applyTo: "**/*"` for always-on)
- `.prompt.md` files require `description:` and `mode: agent` frontmatter
- `.agent.md` files require `description:` and `tools: []` frontmatter
- None of these files use Cursor's `alwaysApply:` or `globs:` fields

### Platform detection pattern for shared hooks
Shared hooks installed across platforms detect their runtime environment via:
```js
const _hooksParent = path.basename(path.dirname(__dirname));
const isCursor = _hooksParent === '.cursor';
const isCopilot = _hooksParent === '.github';
```
This works because hooks are installed at `.<platform>/hooks/<hook>.cjs` and `__dirname`
is `.<platform>/hooks/`. No environment variables needed.

### autolearn-pending path is platform-specific
The `autolearn-pending` flag file lives at `.<platform>/autolearn-pending`:
- Claude Code: `.claude/autolearn-pending`
- Cursor: `.cursor/autolearn-pending`
- Copilot: `.github/autolearn-pending`
post-write.cjs must resolve this path using the platform detection above.

## Proposed Skill Content

```markdown
---
name: copilot-infrastructure
description: GitHub Copilot hook protocol, file formats, and agentic integration patterns
activation:
  keywords: ["copilot", "github copilot", "copilot hook", "copilot instructions", "agent.md", "prompt.md", "instructions.md"]
---

## Hook Protocol

Copilot uses the same stdin/stdout JSON protocol as Claude Code (not Cursor):
- Event names: UserPromptSubmit, PreToolUse, PostToolUse, Stop
- Config: `.github/hooks/hooks.json` — `{"hooks": {"UserPromptSubmit": [{"type": "command", "command": "node .github/hooks/foo.cjs"}]}}`
- UserPromptSubmit input: `{"prompt": "..."}` — same as Claude Code
- Error output: plain text to stdout, exit 2 (NOT Cursor JSON format)

## File Format Reference

| File type | Location | Required frontmatter |
|-----------|----------|---------------------|
| Always-on instructions | `.github/copilot-instructions.md` | none (native) |
| Per-path instructions | `.github/instructions/*.instructions.md` | `applyTo: "**/*"` |
| Slash commands (prompts) | `.github/prompts/*.prompt.md` | `description:`, `mode: agent` |
| Agent roles | `.github/agents/*.agent.md` | `description:`, `tools: []` |
| Skills | `.github/skills/<name>/SKILL.md` | standard agentic frontmatter |
| Hook config | `.github/hooks/hooks.json` | N/A |

## Platform Detection in Shared Hooks

```js
const _hooksParent = path.basename(path.dirname(__dirname));
const isCursor  = _hooksParent === '.cursor';
const isCopilot = _hooksParent === '.github';
const platformDir = isCursor ? '.cursor' : isCopilot ? '.github' : '.claude';
```

Use `platformDir` to resolve platform-specific paths like `autolearn-pending`.
```
