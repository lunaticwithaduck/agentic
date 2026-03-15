# Problem: No External Benchmark Service for Skill Quality Validation

**Date identified:** 2026-03-15
**Status:** Open — not actionable yet, requires external infrastructure

---

## The Problem

All skill quality validation in agentic is closed-loop: the system tests itself, on its own
tasks, using its own judge. Suite 02 tells you a skill *fires* on the right prompts. Suite 04
tells you it *helps* on agentic's 10 internal E2E tasks. Neither tells you whether the skill
improves output quality in the context where a user actually installed agentic.

A generated skill that scores well internally can still be wrong, over-scoped, or actively
harmful in a caller's domain — and nothing currently detects this.

## Why It Matters

agentic's value proposition is self-improvement. A skill library that grows but can't verify
its own quality will accumulate debt silently. Users observe degraded output quality but have
no feedback path back to the system. The system has no mechanism to correct itself.

This is the deeper version of `autolearn-quality-verification.md` — that problem asks "is a
generated skill good?" This problem asks "good *for whom*, and how would we know?"

## What the Current Validation Covers (and Doesn't)

| What | Suite | Gap |
|------|-------|-----|
| Skill fires on matching prompts | Suite 02 (precision/recall) | Doesn't test output quality |
| Autolearn pipeline runs correctly | Suite 07 (deterministic) | Doesn't test skill content |
| Skill improves agentic's own tasks | Suite 04 (10 tasks, LLM judge) | agentic-domain only, not caller-domain |
| Skill content is accurate | Nothing | Completely unverified |
| Skill helps in caller's project context | Nothing | Completely unverified |

## The Required Architecture

An **external benchmark service** that agentic can call after shipping a new skill:

```
agentic (post-ship)
  → POST /bench
      body: {
        skill_package: { content, rules, fixtures },
        prompt_corpus: [...caller-domain prompts],
        baseline: "vanilla" | "previous_skill_version"
      }
  ← {
      delta: +0.34,
      breakdown: { dimension: score, ... },
      corrections: ["skill covers detection but not severity rating", ...],
      confidence: "low | medium | high"  // based on corpus size
    }
```

The service runs before/after comparisons using an LLM judge, returns a quality delta and
actionable correction feedback. agentic uses the corrections to refine the skill, then
re-ships and re-benches until the delta is positive and confidence is acceptable.

## Why It Can't Close Now

1. **No external infrastructure** — Suite 04 is a local shell script. A service requires
   hosting, API design, auth, and a persistent judge environment.

2. **No caller-supplied corpus** — the service needs domain-specific prompts from the caller's
   actual use case. agentic can't generate these; users must supply them. No mechanism exists
   for users to contribute prompt corpora.

3. **Statistical significance** — even 50 diverse skills is not enough to establish production
   quality baselines. Meaningful signal requires hundreds of tasks across varied domains with
   multiple independent judge runs per task.

4. **Corpus contamination risk** — if the judge and the synthesis model are the same (both
   Claude), the benchmark is not independent. The service needs a deliberate judge/model
   separation strategy.

5. **Feedback loop is bidirectional** — the service must not just score but return structured
   correction feedback that agentic can act on autonomously. This requires a defined correction
   protocol that doesn't yet exist.

## What "Solved" Looks Like

- A hosted `/bench` endpoint callable from `build.sh` or a new `/bench-skill` command
- Users opt-in to contributing anonymized prompt corpora from their domains
- Minimum corpus size threshold before confidence is reported (e.g., 30 prompts)
- Corrections feed directly into skill refinement loop (re-synthesize → re-bench → converge)
- Suite 04 becomes a local smoke test only; the service is the quality gate for shipping

## Relationship to Other Problems

- `autolearn-quality-verification.md` — immediate/local version of this problem (shadow mode,
  lightweight proxies); this problem is the long-term external solution
- `negative-signal-gap.md` — the correction feedback from the service would close the negative
  signal gap more completely than the current `.sc-negative` approach
- `bench-coupling-drift.md` — the service decouples quality validation from the repo entirely
