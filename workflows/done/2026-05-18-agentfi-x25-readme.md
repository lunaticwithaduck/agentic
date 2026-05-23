---
title: AgentFi X25 — Project README for handoff
created: 2026-05-18
status: done
completed: 2026-05-18
---

## Goal
Concrete README at the agentfi-terminal repo root that lets a fresh contributor (or future you) get the project running, understand the data flow, and know where the key surfaces live. Replaces the empty/default Next-template README.

## Files
- `/home/jojo/agentfi-terminal/README.md` — new content

## Steps
- [x] Wrote README with: what/why, stack, quickstart, env vars, route inventory, ASCII data flow diagram, agent registry note, testing strategy, deploy hint, project conventions, workflow link
- [x] Kept under ~200 lines (140 lines)

## Outcome

Completed on 2026-05-18. Replaced the scaffold-era README with a handoff-quality one. The structure walks a fresh reader through: what it does (3 strategies in plain English) → stack → quickstart commands → exhaustive env-var table with required-vs-optional column → route inventory table → ASCII data-flow diagram showing cron → fetcher → store → consumers fan-out → agent registry walkthrough → testing strategy (unit / smoke / visual) → deploy notes → conventions → pipeline pointer.

ASCII diagram explicitly shows the three consumers of the snapshot store (agent page, /api/health, /og), making the architecture self-documenting. Env-var table marks which vars are required for prod vs only for tests (`PIN_CONSTITUTION_SNIPPET`, `SNAPSHOT_DATA_DIR`).

**Skill candidate evaluation:**
- Technologies/frameworks touched: README authoring
- Domain-specific knowledge: none — pure technical writing
- Verdict: SKIP
- Reason: Documentation task, no technology-specific knowledge.

## Completion
Run `/complete workflows/tasks/2026-05-18-agentfi-x25-readme.md`.
