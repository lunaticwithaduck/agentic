---
name: create-pr
description: Create well-structured pull requests with summary, changes, and test plan
activation:
  keywords: ["create pr", "open pr", "pull request", "create pull request", "submit pr"]
  file_patterns: []
---

# Create PR

## Purpose
Create a well-documented pull request that clearly communicates the changes, their purpose, and how to verify them.

## Instructions

### 1. Analyze the Branch
- Run `git status` to check for uncommitted changes
- Run `git log main..HEAD --oneline` to see all commits on the branch
- Run `git diff main...HEAD` to see the full diff against the base branch
- Verify the branch is pushed to remote (`git branch -vv`)

### 2. Determine Base Branch
- Default to `main` unless the user specifies otherwise
- Check if the branch was created from a different base

### 3. Write PR Title
- Under 70 characters
- Use imperative mood: "Add feature" not "Added feature"
- Include ticket/issue number if applicable
- Be specific: "Add user email validation" not "Update user module"

### 4. Write PR Body
Structure the body with these sections:
- **Summary**: 1-3 bullet points explaining what and why
- **Changes**: List of specific changes made
- **Test Plan**: How to verify the changes work correctly

### 5. Push and Create
- Push the branch if not already pushed: `git push -u origin <branch>`
- Create the PR using `gh pr create`

### 6. Post-Creation
- Share the PR URL with the user
- Mention if any follow-up tasks are needed

## Output Format
Create the PR using this template:
```
gh pr create --title "the pr title" --body "$(cat <<'EOF'
## Summary
- [Key change 1]
- [Key change 2]

## Changes
- [Specific change with file/module context]

## Test plan
- [ ] [Verification step 1]
- [ ] [Verification step 2]

Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

Return the PR URL to the user when complete.
