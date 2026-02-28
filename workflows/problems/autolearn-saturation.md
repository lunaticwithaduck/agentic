# Problem: Autolearn Saturation

**Date identified:** 2026-03-01
**Status:** Open — no known solution yet

---

## The Problem

The autolearn loop runs indefinitely. Eventually the skill library reaches a point where it
covers the team's stack well. At saturation, new `.sc` files mostly match existing skills
rather than introducing new knowledge. But the system has no way to detect this — it keeps
running synthesis, generating skill updates with diminishing marginal value, burning tokens
on a loop that produces noise.

Worse: past saturation, the loop could start *degrading* quality. Synthesizing 10 `.sc` files
that all say roughly the same thing as the existing skill might produce a blurred, less precise
version of the original. The system optimizes itself into mediocrity.

## Why It Matters

This is the long-tail failure mode. It doesn't show up during development or early use.
It surfaces 6 months in, when the skill library is mature and the team notices responses
getting slightly worse — but there's no obvious cause. By then, hundreds of synthesis cycles
may have run.

## Dimensions

1. **No saturation signal** — nothing measures what fraction of new `.sc` files introduce
   novel domains vs. matching existing skills
2. **No synthesis throttle** — the trigger fires the same whether the skill is new or its
   10th revision
3. **Skill averaging** — repeated synthesis over an already-good skill may regress it
   toward an average of all `.sc` inputs, losing precision
4. **Infinite loop risk** — if a synthesis cycle generates a skill that fires more, it
   generates more `.sc` files, which triggers more synthesis

## Approaches Worth Exploring

- Track novelty ratio: count what fraction of new `.sc` files map to existing skills.
  If ratio > 0.9 for 30 days, reduce synthesis frequency
- Freeze mature skills: after a skill has been synthesized N times with no significant
  content change, mark it `stable: true` and stop synthesizing it
- Diff-based synthesis gate: only run synthesis if the new `.sc` files introduce content
  not already in the existing skill (semantic diff before synthesis)
- Synthesis dampening: each successive synthesis of the same domain requires more `.sc`
  files to trigger (N=3 first time, N=5 second, N=8 third, etc.)
