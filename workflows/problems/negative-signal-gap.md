# Problem: Negative Signal Gap

**Date identified:** 2026-03-01
**Priority:** Must address post-PoC
**Status:** Partially addressed — see Decision Log below

---

## The Problem

The autolearn loop only captures positive signal: things that worked, patterns that succeeded.
There's no mechanism to detect when a skill fires but produces a bad outcome — or when the user
corrects Claude's response immediately after a skill-activated prompt.

The system can only add and reinforce. It cannot detect failure or self-correct.

## Why It Matters

A skill that consistently leads to wrong outputs is invisible to the current design. The
system would keep generating variants of the same flawed pattern, making the skill more
confident and more wrong over time.

Negative signal is also the richer signal: knowing what NOT to do (anti-patterns, pitfalls,
edge cases) is often more valuable than knowing what to do. Currently none of this is captured.

## The Decay Blind Spot

The skill decay mechanism tracks usage frequency — not quality. A skill that fires constantly
but produces bad output looks *healthy* by every metric the system has. Decay only removes
skills that are forgotten, not skills that are actively harmful. This is the sharpest version
of the problem: a high-frequency bad skill is the worst possible outcome, and the current
design has no way to detect it.

## Dimensions of the Problem

1. **No correction detection** — if the user says "actually, that's wrong because X", that X
   is lost; it's not associated with the skill that fired
2. **No skill attribution** — when a response is good or bad, we don't know which skill
   contributed (or whether skill injection helped at all)
3. **No failure modes encoded** — done/ outcome sections record successes; failures are
   underrepresented in the corpus that feeds synthesis
4. **Confirmation bias in extraction** — an LLM extracting learnings from its own successful
   outputs will naturally extract what made it confident, not what it got wrong
5. **Decay doesn't help** — a harmful skill that fires often is immune to decay; frequency
   and quality are orthogonal signals

---

## Analysis (2026-03-07)

Five approaches were evaluated using four independent subagents (architect, researcher,
implementer, auditor). Key findings:

### Approaches Evaluated

| Approach | Signal Quality | Cost | Verdict |
|---|---|---|---|
| Keyword correction detection ("actually", "that's wrong") | Very low — 30–50% false positive rate | ~35 lines | **Rejected** |
| Stop hook for correction logging | None — Stop hook has no conversation access | ~40 lines | **Rejected** |
| Failure modes section in skills | N/A (format only, no capture mechanism) | ~10 lines | **Built** |
| Update `/complete` to capture failures | High — human-annotated at highest-fidelity moment | ~15 lines | **Built** |
| `.sc-negative` full pipeline | Medium — voluntary capture, missing synthesis path | ~40 lines + design | **Deferred** |
| Suite 02 precision regression after synthesis | High — deterministic, no LLM, works in-session | ~20 lines | **Built** |
| Suite 04 E2E regression after synthesis | High but expensive — blocked by `CLAUDECODE` session restriction | High | **Rejected** |

### Why Correction Detection Was Rejected

The keyword approach ("actually", "you missed", "that's wrong") produces false positives
on ordinary technical conversation at a rate that makes the signal untrustworthy as an
automated trigger. A `flagged_count` driven by false positives would suppress good skills —
the exact failure mode the system is trying to prevent. The Stop hook approach was also
rejected because the Stop hook physically has no access to conversation content.

### Why `.sc-negative` Was Deferred

The file format is the right shape. The problem is the feedback path: accumulating negative
files without a synthesis path that routes them back into skill updates delivers no value.
The positive `.sc` path isn't yet validated at production scale (no auto-generated skills
exist in the library yet). Build the negative path once the positive path is proven and
there is a corpus of negative files to route.

### Key Insight from Analysis

The highest-fidelity moment to capture negative signal is at `/complete` time — when a human
is actively reviewing what happened and can attribute which skill caused what outcome. Every
automated approach is an imperfect proxy for what a human can tell you directly at that
moment for free.

Suite 02 precision regression is the best *automated* signal: if a synthesized skill adds bad
keywords, precision drops immediately, deterministically, with no LLM calls and no session
restrictions.

---

## Decision Log (2026-03-07)

### Built

**1. Failure modes section in skill format**
Added `## Failure Modes` as a required section in `skill-creator.md` and in the synthesis
instruction in `skill-detector.sh`. Anti-patterns are encoded in skills from this point
forward. Content quality depends on the capture mechanism below.

**2. `/complete` captures failures**
Added a symmetric question to step 8 of `complete.md`: when completing a task, Claude now
also asks whether the work revealed an anti-pattern or failure from an existing skill, and
if so, writes a failure modes observation into the `.sc` file (or a standalone note).
This is the highest-fidelity capture point in the system.

**3. Suite 02 precision regression after synthesis**
Added a step to the synthesis instruction in `skill-detector.sh`: after synthesizing a new
skill, run Suite 02 and compare precision to the previous benchmark run. If precision drops
more than 5 percentage points, warn the user of potential keyword pollution.

### Deferred

**`.sc-negative` full pipeline** — revisit when:
- At least one auto-generated skill exists in `.claude/skills/`
- `/complete` has produced at least a few failure-mode observations
- A clear synthesis path from negative files to skill amendments is designed

### Still Open

Dimensions 1 and 2 (correction detection and skill attribution) remain unsolved.
These require either LLM inference (expensive, breaks the zero-overhead design) or
conversation history access (not available to hooks). They are acknowledged as permanent
blind spots until the hook model exposes richer session data.

---

## Revisit Triggers

Come back to this problem when any of the following happen:

- First auto-generated skill lands in `.claude/skills/` → check if Suite 02 regression
  detection caught anything
- `/complete` has generated 5+ failure-mode observations → enough corpus to design the
  `.sc-negative` synthesis path
- Suite 04 win rate drops after a synthesis event → investigate which skill caused it
- A skill fires and demonstrably produces bad output in a real session → manual `.sc-negative`
  file as a one-off, then re-evaluate the pipeline
