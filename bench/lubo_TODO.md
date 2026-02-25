# Bench TODO

## Real A/B Comparison (the actual point of the bench)

The original intent was a proper control vs. treatment test:

- **Control**: Claude + bare `.claude/` folder (no skills, no hooks, no agents)
- **Treatment**: Claude + full agentic tooling (skills, hooks, auto-detection)
- **Same prompts** fed to both setups
- **Measure**: Does agentic actually produce better output?

What currently exists (suite 02) only tests routing accuracy — whether keyword
patterns in `skill-rules.json` correctly identify relevant skills. That's plumbing.
The real question — *does activating a skill meaningfully improve Claude's response?* —
is unanswered.

Suite `04-task-quality` is the placeholder for this. It's been `skipped` in every run.

### Design sketch

1. Curate a set of task prompts with clear acceptance criteria (e.g. "write a SQL
   migration", "review this PR diff for security issues", "design a rate limiter API")
2. Run each prompt twice: once with agentic `.claude/`, once without
3. LLM-as-judge scores both outputs on a rubric (correctness, depth, structure, etc.)
4. Report mean score delta: treatment − control

Cost: real API calls per prompt × 2 + judge calls. Cache responses so reruns are free.
