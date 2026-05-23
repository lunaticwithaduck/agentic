---
title: Commit pending work, clean tree, check GitHub CI, fix any failures
created: 2026-05-23
completed: 2026-05-23
status: done
---

## Goal
Two repos had local changes that needed to be committed and the trees made clean:
- `/home/jojo/agentic` — agentic infrastructure changes (skills, fixtures, workflow files from today's autolearn + polymarket fix task)
- `/home/jojo/Documents/ProbBrain` — polymarket fetcher fix + accumulated in-progress work (quote-tweet chaining, Blogger canonical patch, publication-rule audit pipeline, routine pipeline state)

Push, then check GitHub Actions / CI on both. Investigate and fix any failures.

## Steps
- [x] Inspect `git status` + `git diff` in both repos; group changes into sensible commits
- [x] Commit agentic changes (workflow files, autolearn-generated skills, metrics, fixtures)
- [x] Commit ProbBrain polymarket fetcher fix
- [x] Push both branches (only if branches track a remote; ask user otherwise)
- [x] `gh run list` on each repo to surface CI status; fetch logs for any failure
- [x] If failures exist, root-cause and fix; otherwise report clean

## Outcome

Completed on 2026-05-23.

**Commits (5 total):**
- agentic `cce7e80` — autolearn skill generation (7 new skills) + workflow inbox flush
- ProbBrain `43a4360d` — `fix(polymarket): use CLOB mid for YES price; drop dead builder header`
- ProbBrain `474683b5` — `feat(publish): quote-tweet chaining + Blogger canonical patch`
- ProbBrain `(commit)` — `docs+tools: publication rule + audit pipeline` (PUBLICATION_RULE.md + build_pipeline_audit.py + audit data)
- ProbBrain `36ddbf9a` — `chore: pipeline state snapshot` (8 data files, +47k rows in price_history.json)

**Tree cleanup:**
- Added `.claude/scheduled_tasks.lock` to agentic `.gitignore` (runtime lockfile)
- Moved absolute-path hook overrides from agentic `.claude/settings.json` into `.claude/settings.local.json` (globally gitignored at `~/.config/git/ignore`), then `git restore`'d settings.json so the public file keeps relative paths and doesn't break other clones
- Deleted stray `data/shadow_signals.json.bak` in ProbBrain (2026-05-07 backup, no longer needed)

**Git identity:** set repo-local (not --global) on both repos to `tupac_forever_@abv.bg / Lubomir Pacheliev` per user choice.

**Push setup:** agentic remote was on HTTPS without stored credentials, so push failed; switched to SSH (`git@github.com:lunaticwithaduck/agentic.git`) to match the `gh auth status` config and the ProbBrain pattern.

**GitHub CI:**
- agentic — no `.github/workflows/` exist; nothing to check
- ProbBrain — both workflows (`deploy-pages.yml`, `update-dashboard.yml`) are intentionally disabled (`on: workflow_dispatch` only) with explanatory comments. The historical "failure" entries in `gh run list` are pre-disable noise from 2026-05-02/05. Nothing to fix.

**Final state:** both trees clean (`git status` empty), both pushed to origin.
