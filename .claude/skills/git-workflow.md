---
name: git-workflow
description: Git workflow guidance for branching, merging, rebasing, and conflict resolution
activation:
  keywords: ["git workflow", "merge conflict", "rebase", "cherry-pick", "branching strategy", "git help"]
  file_patterns: []
---

# Git Workflow

## Purpose
Provide context-dependent advice on git workflows, conflict resolution, and branch management strategies.

## Instructions

### 1. Assess the Context
- What is the team size? (solo, small team, large team)
- What is the release cadence? (continuous, scheduled, ad-hoc)
- What CI/CD is in place?

### 2. Branching Strategy Advice

**Trunk-Based Development** (recommended for most teams):
- Short-lived feature branches (1-2 days max)
- Merge to main frequently
- Use feature flags for incomplete work
- Best for: CI/CD, small teams, rapid iteration

**GitHub Flow** (simple and effective):
- Branch from main, PR back to main
- Deploy from main after merge
- Best for: SaaS, web apps, continuous deployment

**Git Flow** (structured releases):
- develop, feature/*, release/*, hotfix/* branches
- Best for: versioned releases, mobile apps, libraries

### 3. Merge vs. Rebase
- **Merge**: Preserves history, creates merge commits. Use for shared branches.
- **Rebase**: Linear history, cleaner log. Use for local feature branches before PR.
- **Squash merge**: Combines all branch commits into one. Use for small features.
- NEVER rebase commits that have been pushed to shared branches.

### 4. Conflict Resolution
- Run `git status` to identify conflicted files
- Open each conflicted file and understand both sides
- Choose the correct resolution (not always "ours" or "theirs")
- Test after resolving all conflicts
- Complete the merge/rebase with `git add` + `git commit` or `git rebase --continue`

### 5. Cherry-Pick Guidance
- Use for applying specific commits to other branches (e.g., hotfixes)
- Always use `git cherry-pick -x` to record the source commit
- Be aware of dependency chains: cherry-picking one commit may require others

### 6. Tag Management
- Use annotated tags for releases: `git tag -a v1.0.0 -m "Release 1.0.0"`
- Follow semantic versioning
- Push tags explicitly: `git push origin --tags`

## Output Format
Provide advice as clear, actionable steps specific to the user's situation. Include the exact git commands to run.
