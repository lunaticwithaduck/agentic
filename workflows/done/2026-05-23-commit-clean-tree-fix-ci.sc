---
domain: git-operations
source_task: 2026-05-23-commit-clean-tree-fix-ci.md
date: 2026-05-23
keywords: ["git push", "gh cli", "https remote", "ssh remote", "git config identity", "claude-code settings.local.json", "workflow_dispatch", "stale ci failures"]
---

## Extracted Knowledge

### `gh auth status` saying "ssh" does not rewrite existing HTTPS remotes

If `gh auth status` reports `Git operations protocol: ssh` but `git remote -v` shows an `https://github.com/...` URL, pushes will still fail with:

```
fatal: could not read Username for 'https://github.com': No such device or address
```

`gh` only sets the *preferred* protocol for new clones and `gh repo clone` operations. Existing remotes are untouched.

**Fix:** `git remote set-url origin git@github.com:<owner>/<repo>.git` — one-shot, no auth setup needed if the SSH key is already in the agent.

### `git commit` failing with auto-detected `jojo@jojo-os.(none)`

When git config has no `user.email`, it falls back to `$USER@$HOSTNAME` and refuses to commit with:

```
fatal: unable to auto-detect email address (got 'jojo@jojo-os.(none)')
```

**Set repo-local, not global:** `git config user.email ... && git config user.name ...` from inside the repo. `--global` is overreach when you only need this for one project, and the safety protocol forbids it without explicit user permission.

### Claude Code `settings.local.json` is gitignored by the user's global ignore

`~/.config/git/ignore` ships (in many setups) with the line `**/.claude/settings.local.json`. That's why the file never appears as untracked in `git status` even though the repo `.gitignore` doesn't mention it explicitly.

Implication: when moving machine-specific config out of the tracked `settings.json` and into `settings.local.json`, you don't need to add anything to the repo `.gitignore` — the global rule already covers it. `git check-ignore -v <path>` will show which rule fires.

### Workflows on `workflow_dispatch` only show stale failures in `gh run list`

When a `.github/workflows/*.yml` file has its triggers narrowed down to just `on: workflow_dispatch:` (manual-only), `gh run list` continues to surface the **last successful + failed runs from before the trigger was removed** as the most recent activity. That can look like ongoing CI breakage.

Distinguishing signal from noise:
1. Read the workflow file's `on:` block — if it's `workflow_dispatch:` only, no auto-trigger fires
2. Run `gh run list --commit <your-recent-sha>` — if it returns empty, your commit didn't trigger a run, confirming the workflow is dormant
3. Look for a "Disabled YYYY-MM-DD" comment at the top of the workflow file — common convention for intentional shutdowns

### Don't blindly commit unrelated in-progress changes

When `git status` shows files you didn't touch this session (modified code in `pipeline/*.py`, untracked tools, `.bak` backups, etc.), DO NOT bundle them into your commit. Instead:
1. `git diff <file>` — read each change to determine intent
2. If the diffs look coherent and intentional (descriptive comments, scoped change, no debug cruft) → ask the user before committing them as separate scoped commits
3. If they look like junk (`.bak` files, accidental edits) → ask before deleting

A "make the tree clean" request from the user does NOT authorize sweeping unrelated work into a single commit.

## Proposed Skill Content

For the `git-operations` skill (when synthesized):

- **`gh` protocol vs existing remotes**: section explaining the HTTPS-prompt-failure mode and the `git remote set-url` fix
- **Identity setup safety**: prefer repo-local `git config user.email/name`; treat `--global` as requiring explicit user consent
- **Reading `gh run list`**: how to tell active CI failures from stale-after-disable noise; the `--commit <sha>` trick
- **Multi-repo session hygiene**: when changes span repos (e.g. agentic + a sibling project), commit each separately with scoped messages; never `git add -A` across an unfamiliar tree
