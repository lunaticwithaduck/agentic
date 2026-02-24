---
name: git-commit
description: Craft well-structured conventional commit messages from staged changes
activation:
  keywords: ["commit", "commit message", "git commit", "stage and commit"]
  file_patterns: []
---

# Git Commit

## Purpose
Analyze staged changes and craft clear, conventional commit messages that explain the "why" behind changes.

## Instructions

### 1. Analyze Staged Changes
- Run `git status` to see what is staged
- Run `git diff --cached` to review the actual changes
- Run `git log --oneline -5` to see recent commit style

### 2. Categorize the Change
Use conventional commit types:
- **feat**: A new feature or capability
- **fix**: A bug fix
- **refactor**: Code restructuring without behavior change
- **docs**: Documentation only changes
- **test**: Adding or updating tests
- **chore**: Build, CI, tooling, or dependency changes
- **perf**: Performance improvement
- **style**: Formatting, whitespace, or cosmetic changes

### 3. Write the Commit Message
Follow this format:
```
type(scope): subject line under 50 chars

Body explaining WHY this change was made, not WHAT changed
(the diff shows what changed). Wrap at 72 characters.

Include context that will help future developers understand
the reasoning behind this decision.

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
```

### 4. Rules
- Subject line: imperative mood ("add" not "added"), no period, under 50 chars
- Scope: optional, indicates the module/area affected
- Body: explain motivation and contrast with previous behavior
- Always include the Co-Authored-By line when Claude helped
- Do NOT commit files that may contain secrets (.env, credentials, keys)
- If unsure about scope of changes, ask before committing

### 5. Execute
- Stage specific files (prefer `git add <file>` over `git add .`)
- Create the commit using a HEREDOC for proper formatting
- Run `git status` after to verify success

## Output Format
The commit is created directly via git. Show the user:
- The commit message used
- The `git status` output confirming the commit
