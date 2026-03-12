---
title: Cursor port — sessionStart project stack detection
status: idea
created: 2026-03-12
---

## Idea

At `sessionStart`, scan the project structure to detect the tech stack and inject
relevant skills as `additional_context`. Deterministic, zero AI cost, fires once
per session.

Detection signals:
- `package.json` dependencies → js/ts framework skills (react, pixi, etc.)
- `requirements.txt` / `pyproject.toml` → python framework skills (fastapi, etc.)
- `*.db` files or sqlite pragmas in source → sqlite skill
- `Dockerfile` → dockerfile skill
- `.github/workflows/` → ci-cd skill
- etc.

## Why It's Interesting

Complements keyword matching rather than replacing it. Keyword matching answers
"what is the user asking about right now?" Stack detection answers "what does
this project use?" — a useful background layer even when the current prompt
has no keywords.

## Why It Doesn't Solve the Core Problem

Stack detection is presence-based, not intent-based. It injects skills because
a file exists, not because the user is working on a task related to that file.
In an active session the user might be doing 10 different things — stack detection
injects all project skills for all of them, which is noise.

The real value of skill injection is task-specificity. A project using FastAPI
doesn't need the FastAPI skill injected when the task is about CSS layout.
Keyword matching solves this. Stack detection does not.

## Potential Integration

Could work as an additional layer on top of keyword matching, not a replacement:
- `sessionStart` stack detection → inject a compact "this project uses X, Y, Z" note
- Per-prompt keyword matching (when available) → inject full skill content

On Cursor specifically, stack detection could serve as a fallback until Cursor
adds context injection to `beforeSubmitPrompt`. It won't replicate keyword matching
but it's better than nothing for project-scoped domains.

## Blocked On

Not blocked — implementable today. But the value proposition is limited until
it's combined with per-prompt keyword injection, which requires Cursor to fix
their `beforeSubmitPrompt` API gap.
