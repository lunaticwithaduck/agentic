---
name: e2e-evaluation
description: Patterns for designing and diagnosing LLM-judged E2E benchmark evaluations
activation:
  keywords: ["suite 04", "bench", "e2e benchmark", "LLM judge", "judge variance", "rubric", "benchmark task", "infra win", "vanilla win", "benchmark regression"]
  file_patterns: ["bench/suites/**", "bench/e2e/**", "bench/results/**"]
---

# E2E Evaluation Design

## Purpose
Inject concrete patterns for diagnosing LLM-judged benchmark results, designing rubrics,
and interpreting Suite 04 (or similar E2E) outcomes accurately.

## Diagnosing Losses — Verify Before Fixing

Single-run losses in LLM-judged benchmarks are unreliable signals. A −0.25 delta can
flip to +0.5 on the next run with no changes. Before acting on a loss:

1. **Read the skill** — does it actually cover the missing dimension?
2. **Read the cached infra response** — did Claude say it or not?
3. **If covered and still lost** → likely judge variance, not a skill gap
4. **Only update the skill** if the response genuinely missed something the skill covers

**Threshold for investigation:** delta ≤ −0.5 warrants examination. Delta between −0.5
and 0 should be confirmed across two independent runs before concluding there's a problem.

## Judge Variance Patterns

LLM judges exhibit systematic biases — these are repeatable patterns, not random noise:

**Behavior-preservation leniency:** On refactoring tasks, judges reward behavior changes
that appear to "fix latent bugs" even when the rubric explicitly forbids them. A response
that changes `if cat:` to `if category is not None:` will often receive credit despite
the behavior change. Infra responses that strictly preserve behavior lose to vanilla
responses that "improve" the code.

**Classification miscalibration:** Judges inherit Claude's intuitive severity/priority
ratings, which diverge from industry standards. Without explicit rubric guidance,
judges accept miscalibrated ratings (e.g., MEDIUM for hardcoded secrets, LOW for IDOR).

**Completeness vs. precision trade-off:** Judges frequently reward responses that find
more issues over responses that find the right issues with better analysis.

## Rubric Design Rules

**For behavior-preservation tasks:** Add explicit language: "any change to observable
behavior is a disqualifying failure, regardless of whether the change appears beneficial."
A high weight alone is insufficient — judges apply leniency unless the rubric explicitly
prohibits exceptions.

**For classification tasks (severity, priority, risk):** Include an enumerated rating
guide in the rubric itself. Do not assume the judge knows industry-standard classifications.
Example: "CRITICAL = auth bypass or RCE; HIGH = hardcoded secrets, IDOR, session fixation."

**For completeness dimensions:** Specify exactly which items must be found. Vague rubrics
("doesn't miss significant issues") allow judges to reward verbosity over accuracy.

## Skill Gap Diagnostic

If a skill-activated response lost on a **classification rubric dimension**:
→ The skill probably teaches *detection* but not *rating*. Find the rating step in the
  skill, add an explicit framework with concrete examples.

If a skill-activated response lost on a **completeness rubric dimension**:
→ Check if the skill covers all items the rubric expects. Add missing items explicitly.

If the loss is on a dimension where the skill has no relevant content:
→ It is judge variance or a task design issue. Do not update the skill.

## Judge Model Considerations

Haiku (default judge) applies more leniency than Sonnet on subjective dimensions.
For strict behavior-preservation or precise classification requirements, use a more
capable judge model (`--judge-model=claude-sonnet-4-6`) or add automated output
comparison alongside the LLM judge.

## Failure Modes

- **Over-diagnosing single-run losses** — updating skills based on one run's −0.25 delta wastes effort and can introduce unnecessary changes. Confirm losses across runs.
- **Fixing the skill when it's a rubric problem** — if infra did the right thing but the rubric is ambiguous, fixing the skill doesn't help. Fix the rubric.
- **Assuming judge variance is random** — most judge variance is systematic. Haiku applies consistent biases; understanding them lets you predict and account for them rather than re-running indefinitely.
