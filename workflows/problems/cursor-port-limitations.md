---
title: Cursor port — skill injection gap and degraded parity
status: open
opened: 2026-03-12
---

## Context

Agentic is being ported to Cursor. Research conducted against Cursor docs (March 2026)
reveals a structural gap that cannot be worked around with current Cursor APIs.

## The Core Problem

Claude Code's `UserPromptSubmit` hook fires on every prompt and injects stdout directly
into the model's context. This is what powers skill auto-detection: keyword match →
inject skill content → model has expert knowledge for this prompt.

Cursor's `beforeSubmitPrompt` hook **cannot inject context**. It can only block or allow.
There is no per-prompt context injection hook in Cursor. This is a confirmed feature gap
with an open forum request (February 2026) and no timeline from the Cursor team.

The only Cursor hook that can inject `additional_context` is `sessionStart` — which fires
once when the session opens, not per prompt.

## What This Means

### Lost
- **Keyword-based skill auto-detection** — no equivalent. `agentRequested` rules are
  probabilistic (model decides based on description); this is the same failure mode as
  instructed behaviour, which we already know is unreliable.

### Degraded
- **Synthesis injection** — `sessionStart` can inject `additional_context` once per session.
  If the user ignores it, the next session re-injects (flag persists). Per-prompt retry
  behaviour is lost; synthesis is no longer guaranteed to complete.

### Full parity
- **block-secrets** → `beforeShellExecution` (block/allow, deterministic)
- **post-write `.sc` candidating** → `afterFileEdit` fires, JS writes `autolearn-pending`
  to disk (hook output is ignored but filesystem side-effects work)
- **Commands** (`/complete`, `/idea`, `/promote`) → `.cursor/commands/*.md` (added Cursor 1.6)
- **CLAUDE.md equivalent** → `.cursor/rules/agent-instructions.md` with `alwaysApply: true`
- **File-pattern skill detection** → `globs`-based rules fire deterministically when matching
  files are in context. Covers roughly half of skill activations (the file-scoped ones).

## Reliable vs Unreliable in Cursor Rules

| Mode | Mechanism | Reliable |
|---|---|---|
| `alwaysApply: true` | Always injected | ✓ |
| `globs` | File-pattern match by system | ✓ |
| `agentRequested` | Model reads description and decides | ✗ |

`agentRequested` is not a viable replacement for keyword matching. Same failure mode
as instructed behaviour — silent misses with no error signal.

## Additional Cursor Bugs / Limitations (as of March 2026)

- `beforeSubmitPrompt` blocking is broken: blocked messages persist in conversation
  history and are included in subsequent LLM context, defeating security guardrails.
- `afterFileEdit`, `afterShellExecution`, `afterAgentResponse` etc. are fire-and-forget.
  Output is never consumed — observation only.
- `agentRequested` and `globs` are mutually exclusive on a single rule.
- Rules only affect Agent (Chat). No effect on Tab completions or Cmd+K inline edit.
- Max recommended rule size: 500 lines. Larger rules may be silently truncated.

## Options

1. **Ship the port with honest limitations** — file-pattern skills work, keyword detection
   doesn't. Document clearly. Users get ~50% of skill activation coverage.

2. **Wait for Cursor to add context injection to `beforeSubmitPrompt`** — forum request
   exists. No timeline. Check back on Cursor changelog.

3. **Use `sessionStart` for all skills (always-on)** — inject all skills at session start.
   Bloats every prompt with irrelevant context. Probably worse than no skills.

4. **Skills as `globs` only** — only ship skills that have meaningful `file_patterns`.
   Skills like `regex-helper` or `mermaid-diagram` (no file pattern) are skipped.
   Clean but narrows the library further.

## Recommendation

Option 1 + 4 combined: ship the port, use `globs` for file-scoped skills, omit keyword-only
skills, document the gap clearly. Re-evaluate when Cursor adds per-prompt context injection.
