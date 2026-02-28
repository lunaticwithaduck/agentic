# Problem: Autolearn Quality Verification

**Date identified:** 2026-03-01
**Status:** Open

---

## The Problem

If a skill is auto-generated from done/ history, there's no mechanism to verify it's actually
good before it starts firing on every matching prompt. A bad skill is actively worse than no
skill — it injects false confidence, wastes tokens, and can distort responses in subtle ways.

The only objective quality signal we have is suite 04 (E2E task quality). But running suite 04
after every skill generation is expensive (~10 claude subprocess calls) and too slow to be
part of any automatic loop.

## Why It Matters

The self-improving skill library assumes improvement. If generated skills degrade quality, the
system gets worse over time — and because degradation is subtle (not a crash), it's hard to
detect. We'd be confidently wrong.

## Dimensions of the Problem

1. **No cheap proxy for quality** — suite 04 is the only objective measure, and it's expensive
2. **No shadow period** — a generated skill goes live immediately with no observation window
3. **No rollback** — if a skill makes things worse, nothing detects or reverts it
4. **Confirmation bias** — the system that generated the skill is also the one using it;
   it may not recognize when it's wrong

## Approaches Worth Exploring

- Shadow mode: write the skill but don't add it to skill-rules.json yet; observe N prompts
  where it would have fired and check if the response quality was good before promoting
- Lightweight scoring proxy: score the generated skill against the skill-design-principle
  filter (does it inject facts Claude lacks, or just methodology?) as a first gate
- Human-in-the-loop for first generation: auto-generate but require confirmation for the
  first time a domain skill is written; subsequent updates can be fully automatic
