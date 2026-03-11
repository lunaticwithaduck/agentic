---
title: Task pipeline not self-initiated by agents
created: 2026-03-07
status: problem
tags: [workflow, autolearn, pipeline, convention]
priority: high
---

# Task Pipeline Not Self-Initiated by Agents

## Description

The autolearn pipeline depends on agents creating tasks in `workflows/tasks/` before
implementing work, then running `/complete` after each task to generate `.sc` files.
In practice, agents bypass the pipeline entirely — they implement directly without ever
touching the workflow directories.

## Evidence

Tested three times in `temp/webhook-relay` (fresh agentic install, empty skills):

| Attempt | Fix applied | Result |
|---------|-------------|--------|
| Prompt 1 | Convention in CLAUDE.md | App built, zero pipeline activity |
| Prompt 2 | Convention reworded (create + complete) | App built, zero pipeline activity |
| Prompt 3 | Hook injects soft reminder (`[WORKFLOW]...`) | App built, zero pipeline activity |
| Prompt 4 | Hook injects hard instruction (`[REQUIRED]...`) | TBD |

Pattern: Claude reads the build request and satisfies it directly. Any instruction to
"create tasks first" loses to the pull of the explicit user request — regardless of
whether that instruction is in CLAUDE.md or injected by the hook.

## Root Cause

Claude optimises for fulfilling the user's request. A build request ("build a webhook
relay service") has a clear, immediate goal. A workflow instruction ("create tasks first")
is overhead that delays that goal. Without a hard blocker, the workflow instruction is
treated as optional process documentation.

The Convention 7 wording ("self-complete tasks") only covered the second half of the
loop — completing existing tasks. It never addressed the first half: creating tasks
before starting. Even after fixing the wording, the behaviour didn't change because the
pull of the build request was stronger than any advisory instruction.

## Why This Matters

Without task creation, `/complete` never runs → no `.sc` files → no autolearn signal →
skills never grow. The entire self-improvement loop is dead unless the human explicitly
manages the pipeline themselves — which defeats the purpose of agentic.

## Approaches Considered

**1. CLAUDE.md convention (done — failed)**
Low friction, easy to maintain. Doesn't work — conventions lose to user requests.

**2. Soft hook reminder (done — failed)**
Injected `[WORKFLOW]` message via `skill-detector.sh`. Claude reads it as informational
context and proceeds to implement anyway.

**3. Hard hook instruction (in progress — prompt 4)**
Injected `[REQUIRED — Before writing any code or files]` with explicit prohibition:
"Do not write any implementation files until at least one task file exists."
Stronger language, same channel. May work; may not — channel is the same problem.

**4. Hook blocks on empty tasks (not yet tried)**
`skill-detector.sh` outputs `{"decision": "block", "reason": "..."}` to block the
response entirely until tasks exist. True hard gate — Claude cannot proceed.
Risk: overly aggressive. Blocks simple conversational prompts on fresh projects too.
Could gate on prompt length or keywords to reduce false blocks.

**5. Starter task auto-creation (not yet tried)**
When the hook detects a new project (empty tasks/ AND empty done/), it auto-creates
a `workflows/tasks/YYYY-MM-DD-initial-planning.md` stub and injects a message:
"I've created a task stub for you — fill it out before implementing."
Reduces the friction of the first step. Still requires Claude to engage with it.

**6. Prompt wrapping (not yet tried)**
The first-prompt template explicitly includes "First, create tasks in workflows/tasks/
for each component." User must include this in every build request.
Works but requires human effort every time — not zero-intervention.

**7. Two-phase session design (architectural)**
Phase 1 prompt: "Plan this project and create task files only — no implementation."
Phase 2 prompt: "Now implement task by task, completing each before moving on."
Separates planning from implementation at the prompt level.
Most reliable but requires structured multi-prompt workflow.

**8. Synthesis priority fix (done — 2026-03-09)**
Root cause identified: synthesis instructions were injected alongside the `[REQUIRED]`
workflow reminder and skill injections in the same hook output, giving the agent three
competing imperatives. The user's explicit request won every time.

Fix: when `autolearn-pending` is set, synthesis takes absolute priority —
- Workflow reminder is suppressed
- Skill injections are suppressed
- Only synthesis instructions are output, in compact imperative form
- First line is `[AUTOLEARN — SYNTHESIS REQUIRED. Do this now, before anything else...]`

**9. post-write.sh relative path bug (found and fixed — 2026-03-09)**
Root cause identified: `post-write.sh` used the pattern `*/workflows/done/*.sc` which
only matches absolute paths. Agents often write `.sc` files using relative paths
(e.g. `workflows/done/18-bulk-replay.sc`). The pattern never matched → `autolearn-pending`
was never set automatically → synthesis never fired automatically.

Fix:
- Changed pattern from `*/workflows/done/*.sc` to `*workflows/done/*.sc` (matches both)
- Added path normalization in Python: converts relative paths to absolute using ROOT_DIR
- Verified with tests 1-4 in suite 07 (all pass)

**10. Synthesis auto-queue (done — 2026-03-09)**
When multiple domains hit the N=3 threshold simultaneously, only the last one would
set `autolearn-pending` (each write overwrites the previous). After synthesis, no
mechanism triggered synthesis for waiting domains.

Fix: synthesis step 4 now auto-queues instead of just deleting the flag —
- After synthesizing domain X, scans `workflows/done/` for other domains with ≥3 .sc files
  but no skill in `.claude/skills/` yet
- If found: overwrites `autolearn-pending` with next domain (chains synthesis automatically)
- If none: deletes the flag

## Status

**All three root causes fixed as of 2026-03-09.**

The next prompt sent to the webhook-relay session will:
1. Inject synthesis instructions for `fastapi` (3 .sc files queued)
2. After fastapi synthesis, auto-queue `sqlite` (3 .sc files also queued)
3. After sqlite synthesis, clear the flag

Zero manual intervention required going forward.

## Revisit Triggers

- If synthesis is still skipped after these fixes → synthesis must be moved out-of-band
- If block approach needed for other reasons → gate on synthesis pending, not on tasks/ empty
