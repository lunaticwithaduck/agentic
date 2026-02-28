---
title: Deterministic skill pre-filtering in UserPromptSubmit hook
created: 2026-03-01
promoted: 2026-03-01
completed: 2026-03-01
status: done
author: user
tags: [skills, hooks, performance, architecture]
priority: high
complexity: medium
---

# Deterministic skill pre-filtering in UserPromptSubmit hook

## Description

Replace the current "have Claude evaluate all 27 skills" approach with deterministic
keyword matching in skill-detector.sh itself. The hook already receives the prompt via
stdin JSON — it just ignores it. Fix that.

## Acceptance Criteria

- [x] skill-detector.sh reads prompt text from stdin JSON (python3 one-liner)
- [x] Hook matches prompt (case-insensitive) against keywords in skill-rules.json
- [x] If no skills match: hook outputs nothing (completely silent — no overhead)
- [x] If skills match: hook reads and outputs matched skill .md content directly
- [x] Graceful fallback: if python3/skill-rules.json unavailable, hook outputs nothing silently
- [x] Suite 01 infrastructure check still passes (hook executable, named correctly)
- [x] Suite 02 F1 score improves vs. old AI-evaluation approach (run bench to verify)

## Outcome

Completed on 2026-03-01. Rewrote skill-detector.sh from a static protocol injector into
a real pre-filter: reads stdin JSON, extracts prompt, matches against skill-rules.json
keywords via python3, and either injects matched skill .md content directly or outputs
nothing. Verified: SQL optimization prompt correctly fires sql-optimization skill; generic
prompt outputs nothing; multi-keyword prompt fires both security-audit and dockerfile.
Removed the now-obsolete "Also Update skill-detector.sh" step from skill-creator.md —
adding a skill to skill-rules.json is now sufficient for auto-detection.
