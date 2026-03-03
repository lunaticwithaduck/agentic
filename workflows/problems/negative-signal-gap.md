# Problem: Negative Signal Gap

**Date identified:** 2026-03-01
**Priority:** Must address post-PoC
**Status:** Open

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

## Approaches Worth Exploring

- Detect correction patterns in conversation: if a prompt follows a skill-activated response
  and contains "actually", "that's wrong", "you missed", flag the skill for review
- Add `.sc-negative` (skill candidate, negative) files: when completing a task that required
  correcting a previous mistake, capture the anti-pattern explicitly
- Include failure modes section in skills: not just "do X" but "avoid Y because Z"
- Stop hook: if a skill fired this session AND a correction occurred this session, log to
  `.claude/skill-feedback/` for later analysis
- Benchmark regression detection: if Suite 04 scores drop after a new skill is synthesized,
  flag the skill automatically

## Why This Is Post-PoC

Any real fix requires outcome scoring, benchmark regression detection, or human-in-the-loop
review. All of these add meaningful complexity. The PoC goal is to demonstrate that the
positive-signal loop works at all. But this problem must be addressed before the system is
used in production — a skill library that can silently degrade is a liability, not an asset.
