# Agentic — Project Evaluation

**Date:** 2026-02-25  
**Scope:** Idea, structure, execution, progress, and improvement opportunities

---

## Executive Summary

**agentic** is a reusable, AI-first project infrastructure for Claude Code. It provides a
ready-to-clone template with 65 skills, 8 specialized agents, 10 slash commands, 4 hook
scripts, and a three-stage workflow pipeline — all backed by a custom benchmark suite.

**Verdict:** Strong idea, well-structured, and impressively executed for a tooling project.
The benchmark system is a standout differentiator. Key gaps are in self-documentation
(the project doesn't fully describe itself) and the unfinished E2E quality comparison.

---

## 1. Idea — 9/10

The concept of a standardized Claude Code infrastructure that you can clone into any project
is excellent. It targets a real need:

- **Problem it solves:** Every Claude Code user starts from scratch, building up skills, hooks,
  and workflows project by project. There's no shared foundation.
- **Value proposition:** Clone once, get a complete AI-first setup — auto-detected skills,
  security guardrails, a workflow pipeline, and specialized agents.
- **Target audience:** Teams and individuals using Claude Code regularly across multiple projects.

**Strengths:**
- The skill auto-detection concept (evaluate all skills, activate 1-3 most relevant) is
  genuinely novel and well-executed
- The workflow pipeline (ideas → tasks → done) is simple, practical, and well-integrated
  with the slash commands
- The multi-agent system with clear role separation (PM, architect, worker, auditor, etc.)
  is well-designed
- The benchmark suite is a rare and valuable addition — most tooling projects don't measure
  whether they actually work

**One concern:** The value proposition rests on an unproven assumption — that activating
specialized skills *meaningfully improves* Claude's output. Suite 04 was designed to test
this but hasn't been run yet. This is the single most important open question.

---

## 2. Structure — 8/10

The directory layout is clean and logical:

```
.claude/
  settings.json          # Hook configuration, permissions
  skills/                # 65 skill definitions + skill-rules.json
  hooks/                 # 4 hook scripts (security, skill detection, validation)
  agents/                # 8 agent role definitions
  commands/              # 10 slash command definitions
  scripts/               # Setup utilities (validate, MCP, Playwright)
workflows/
  ideas/ → tasks/ → done/   # Three-stage pipeline
bench/
  suites/                # 5 numbered test suites
  fixtures/              # Labeled test data (100 prompts, 85 commands)
  e2e/                   # E2E quality comparison framework
  lib/                   # Shared test helpers
  results/               # Metrics, changelog
docs/
  ARCHITECTURE.md        # (template — needs filling in)
```

**Strengths:**
- Clear separation of concerns: skills, hooks, agents, and commands each live in their own space
- The bench system follows a well-structured pattern: numbered suites, fixtures, shared libs
- Auto-discovery of suites (drop a numbered `.sh` file in `bench/suites/`)
- The gitignore correctly excludes generated metrics JSON while tracking human-readable `.md` results

**Areas for improvement:**
- `docs/ARCHITECTURE.md` is still a blank template — the project doesn't describe its own architecture
- `CLAUDE.md` has several unfilled `TODO:` sections for project-specific configuration
- No `CONTRIBUTING.md` or contributor guide
- No versioning strategy (no `VERSION` file, no release tags)

---

## 3. Execution — 8/10

The implementation quality is consistently high across the codebase.

### Skills (65 files)
- All have proper YAML frontmatter
- Content is detailed and structured (checklists, output formats, categories)
- The `code-review` skill alone has 90 lines covering security, performance, readability,
  test coverage, and architecture
- The `system-design` skill follows a proper design methodology (requirements → components →
  interfaces → data → cross-cutting concerns → trade-offs)

### Hooks (4 scripts)
- `block-secrets.sh`: Comprehensive — 21 blocked patterns + 8 commit-secret patterns, with a
  safe-variant allowlist for `.env.example` etc. Well-tested via bench suite 03.
- `skill-detector.sh`: Lightweight and fast — pure bash, reads stdin, emits the skill
  evaluation protocol. No parsing overhead.
- `post-write.sh`: Checks for hardcoded API keys (AWS, Anthropic, GitHub PATs). The
  language-specific linting is appropriately left as TODO for per-project customization.
- `post-stop.sh`: Placeholder — no-op.

### Benchmark Suite
This is the crown jewel. Five suites covering:

| Suite | Tests | Score | Notes |
|-------|-------|-------|-------|
| 01-infrastructure | 24 | 100% | Validates structure, configs, coverage |
| 02-skill-detection | 4 thresholds | 93.3% F1 | 100 labeled prompts, per-skill breakdown |
| 03-hook-security | 89 | 100% | 40 blocked + 45 allowed, FP/FN rate tracking |
| 04-task-quality | — | Skipped | E2E comparison (needs API key) |
| 05-keyword-overlap | 4 thresholds | Passing | Cross-contamination analysis |

The benchmark has clear evidence of iteration:
- Precision improved from 49.3% → 94.7% (documented in changelog)
- 5 shallow skills were deleted to reduce noise
- 17 skills had keyword surgery to fix false positives
- Changelog entries document what was tried and what worked

### E2E Comparison Framework
The `compare.py` script is well-designed:
- Zero external dependencies (uses only `urllib`)
- Response caching to avoid re-running expensive API calls
- Separate judge model from response model
- Rubric-based weighted scoring with per-dimension granularity
- 5 concrete tasks with realistic prompts and detailed rubrics

### Setup Scripts
- `setup.sh`: Simple and correct — creates directories, checks for Claude, suggests MCP setup
- `validate.sh`: 9 checks covering structure, JSON validity, frontmatter, coverage, executability
- `mcp-setup.sh`: Interactive, supports 4 MCP servers including both Figma options
- `playwright-setup.sh`: Impressively complete — detects package manager, supports 4 PM choices,
  generates config, example test, CI workflow, and gitignore entries

---

## 4. Progress — 7/10

### What's Done
- Core infrastructure: complete and passing all tests
- Skill library: 65 skills with full coverage in rules and detector
- Security hook: 100% accuracy on 85-command corpus
- Benchmark system: 5 suites, iterative improvement documented
- Setup tooling: setup, validate, MCP, Playwright scripts all functional

### What's Not Done
- `docs/ARCHITECTURE.md` — blank template
- `CLAUDE.md` project config — TODO placeholders unfilled
- `post-stop.sh` — no-op placeholder
- Suite 04 — never run (the actual value proposition test)
- Workflow pipeline — all three directories empty (`.gitkeep` only)
- No CI/CD — no GitHub Actions workflow for running the bench on PRs
- No version tagging or release strategy
- `bench/lubo_TODO.md` — aspirations not yet realized

### Iteration Evidence
The changelog shows active development with clear experimentation notes:
```
Score: 100.0%  (prev: 75.0%, +25.0% ↑)
P 61.7% → 94.7% (+33pp), R 85.7% → 92.0% (+6.3pp), F1 71.8% → 93.3% (+21.5pp)
```
This is exactly the kind of data-driven improvement that makes the bench system valuable.

---

## 5. Improvement Ideas

### High Impact

**A. Run Suite 04 and Document Results**  
The single most important open question is: *does activating skills actually improve
Claude's output?* The infrastructure for answering this exists (`compare.py`, `tasks.json`,
suite 04). Running it and documenting the results would either validate the entire project
or surface what needs to change.

**B. Add a CI Workflow for Bench**  
A GitHub Actions workflow that runs `bench/run.sh` on every PR would:
- Catch regressions in skill detection or hook security
- Make the changelog self-maintaining
- Demonstrate the infrastructure working in practice

**C. Self-Dogfood the Workflow Pipeline**  
The workflow pipeline is empty. Using it to track the project's own work items would both
test the feature and demonstrate it to potential adopters. Each improvement idea here could
be an entry in `workflows/ideas/`.

### Medium Impact

**D. Add Negative/Exclusion Rules to Skill Detection**  
The current keyword matching is purely additive — a keyword either matches or it doesn't.
Adding exclusion rules ("activate debug, BUT NOT IF the prompt also mentions 'deployment'")
would reduce the remaining 7 false positives without complex logic.

**E. Structured Skill Frontmatter for Machine Parsing**  
Skills have YAML frontmatter, but the `activate` field is free-text. Making it structured
(e.g., `triggers: [keywords: [...], file_patterns: [...]]`) would allow the skill-detector
hook to read rules directly from skill files instead of requiring a separate `skill-rules.json`.
This eliminates the sync problem that suite 01 already tests for.

**F. Improve `post-stop.sh` to Track Skill Usage**  
The placeholder could compare which skills were detected vs. which were actually activated,
building a feedback loop for improving detection accuracy. This data could feed back into
the bench metrics.

### Lower Impact

**G. Add a `--watch` Mode to the Bench Runner**  
For development, a watch mode that re-runs affected suites when fixture files or skill
rules change would speed up the iteration cycle.

**H. Template Variables in Setup**  
The setup process could use template variables in `CLAUDE.md` and `post-write.sh` that
get replaced during `/setup`, instead of relying on Claude to edit files. This would make
the setup more deterministic.

**I. Skill Dependency Graph**  
Some skills naturally compose (e.g., `code-review` + `security-audit` for auth code,
`system-design` + `api-design` for API architecture). Documenting these relationships
could improve the skill detector's co-activation accuracy.

---

## 6. Summary Scores

| Dimension | Score | Notes |
|-----------|-------|-------|
| **Idea** | 9/10 | Novel, well-targeted, genuine need |
| **Structure** | 8/10 | Clean layout, good separation; self-docs incomplete |
| **Execution** | 8/10 | High quality across skills, hooks, and bench; E2E unfinished |
| **Progress** | 7/10 | Core complete, good iteration evidence; key gaps remain |
| **Overall** | 8/10 | Strong foundation ready for validation and polish |

The project's biggest strength is the benchmark system — it's rare for a tooling project
to have this level of self-measurement. The biggest risk is that the core value proposition
(skills improve output) remains unvalidated. Running Suite 04 should be the top priority.
