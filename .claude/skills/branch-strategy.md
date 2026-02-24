---
name: branch-strategy
description: Branch naming, creation, lifecycle management, and stale branch cleanup
activation:
  keywords: ["branch name", "create branch", "branch naming", "cleanup branches", "stale branches"]
  file_patterns: []
---

# Branch Strategy

## Purpose
Suggest consistent branch names, manage branch lifecycles, and help clean up stale branches.

## Instructions

### 1. Branch Naming Convention
Generate branch names from task descriptions using this format:
```
<type>/<short-description>
```

Types:
- **feature/** - New functionality (e.g., feature/user-authentication)
- **fix/** - Bug fixes (e.g., fix/login-timeout)
- **chore/** - Maintenance tasks (e.g., chore/update-dependencies)
- **docs/** - Documentation (e.g., docs/api-reference)
- **refactor/** - Code restructuring (e.g., refactor/payment-module)
- **test/** - Test additions (e.g., test/checkout-flow)

Rules:
- Use lowercase and hyphens (no spaces, underscores, or camelCase)
- Keep it short but descriptive (3-5 words max)
- Include ticket number if available (e.g., feature/PROJ-123-user-auth)

### 2. Check for Existing Branches
Before creating a new branch:
- Run `git branch -a` to list all local and remote branches
- Check if a branch for this work already exists
- Verify the base branch is up to date: `git fetch origin`

### 3. Create the Branch
```bash
git fetch origin
git checkout -b <branch-name> origin/main
```

### 4. Branch Lifecycle
- Create branch from up-to-date main/develop
- Push early with `-u` flag: `git push -u origin <branch-name>`
- Keep branch up to date with base: `git rebase origin/main`
- Delete after merge: `git branch -d <branch-name>`

### 5. Cleanup Stale Branches
To identify and clean up old branches:
```bash
# Prune remote tracking branches
git fetch --prune

# List merged branches (safe to delete)
git branch --merged main

# List branches by last commit date
git for-each-ref --sort=-committerdate refs/heads/ --format='%(committerdate:short) %(refname:short)'
```

Suggest deletion for branches that:
- Have been merged to main
- Have had no commits in 30+ days
- Have no open PR associated with them

## Output Format
```
Suggested branch name: <type>/<description>

Commands:
  git fetch origin
  git checkout -b <branch-name> origin/main
  git push -u origin <branch-name>
```
