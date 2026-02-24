---
name: weekly-summary
description: Generate weekly activity summaries from git history and project activity
activation:
  keywords: ["weekly summary", "weekly report", "week recap", "activity summary", "weekly update"]
  file_patterns: []
---

# Weekly Activity Summary

## Purpose
Analyze git history and project activity for the past week to generate a structured summary report with metrics and highlights.

## Instructions

1. **Gather data**:
   - Run `git log --since="1 week ago" --oneline --all` for commit overview
   - Run `git log --since="1 week ago" --stat` for file change details
   - Run `git log --since="1 week ago" --format="%an" | sort | uniq -c | sort -rn` for contributor breakdown
   - Check for merged PRs: `git log --since="1 week ago" --merges`
   - Get line-level stats: `git diff --stat HEAD~$(git rev-list --count --since="1 week ago" HEAD)..HEAD`

2. **Categorize changes**:
   - **Features**: New functionality added (look for "feat", "add", "new" in commits)
   - **Bug fixes**: Issues resolved (look for "fix", "bug", "resolve" in commits)
   - **Refactoring**: Code improvements (look for "refactor", "cleanup", "improve")
   - **Documentation**: Doc updates (look for "doc", "readme", changes to .md files)
   - **Tests**: Test additions/changes (look for changes to test files)
   - **Infrastructure**: CI/CD, config, dependency changes

3. **Calculate metrics**:
   - Total commits
   - Total files changed
   - Lines added / removed
   - Number of contributors
   - PRs merged (if applicable)
   - Most active files/directories

4. **Identify highlights**:
   - Largest or most impactful changes
   - Notable milestones
   - Patterns (e.g., high bug-fix activity may indicate instability)

5. **Generate the report**:
   - Write in past tense
   - Be factual and concise
   - Include links to specific commits or PRs where relevant

## Output Format

```markdown
# Weekly Summary: [Date Range]

## Highlights
- Highlight 1
- Highlight 2

## Metrics
| Metric | Value |
|--------|-------|
| Commits | N |
| Files changed | N |
| Lines added | +N |
| Lines removed | -N |
| Contributors | N |

## Changes by Category

### Features
- Description of feature (commit hash)

### Bug Fixes
- Description of fix (commit hash)

### Other
- Other notable changes

## Contributors
- Name: N commits
```
