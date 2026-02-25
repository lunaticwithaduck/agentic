---
name: git-workflow
description: Git workflow guidance for branching, merging, rebasing, conflict resolution, bisect, and history rewriting
activation:
  keywords: ["git workflow", "merge conflict", "rebase", "cherry-pick", "branching strategy", "git help", "git bisect", "squash commits", "interactive rebase"]
  file_patterns: []
---

# Git Workflow

## Branching Strategies

### Trunk-Based Development (recommended for most teams)
```
main ←── feature/login (< 2 days)
main ←── fix/null-check (< 2 hours)
```
- Merge to `main` daily. Use feature flags for incomplete work.
- Best for: teams with CI/CD, rapid iteration.

### GitHub Flow
```
main ←── feature/X (PR) ←── fix/Y on feature/X
```
- Branch from `main`, always PR back to `main`, deploy from `main`.
- Best for: SaaS / web apps with continuous deployment.

### Git Flow
```
main ←── release/1.2 ←── develop ←── feature/X
                                  ←── feature/Y
hotfix/1.1.1 ──────────────────────────────────→ main + develop
```
- Best for: versioned libraries, mobile apps, scheduled releases.

---

## Merge vs Rebase vs Squash

| | Merge | Rebase | Squash |
|---|---|---|---|
| History | Preserves all commits + merge commit | Linear, no merge commit | One commit per branch |
| Use for | Shared / long-lived branches | Local feature branches before PR | Small features, cleanup |
| Rule | ✅ safe on shared branches | ❌ never on pushed shared branches | ✅ good for PR merges |

```bash
# Merge (preserves history)
git checkout main && git merge feature/login

# Rebase (linear history — local branches only)
git checkout feature/login && git rebase main

# Squash via merge
git merge --squash feature/login && git commit -m "feat: add login"

# Interactive rebase — rewrite last N commits
git rebase -i HEAD~3
# In the editor: pick / squash / reword / drop
```

---

## Conflict Resolution

When a merge or rebase conflicts, git marks the file like this:

```
<<<<<<< HEAD              ← your current branch's version
const timeout = 5000;
=======
const timeout = 3000;
>>>>>>> feature/perf       ← incoming branch's version
```

**Resolution workflow:**

```bash
# 1. See all conflicted files
git status
# both modified:   src/config.ts

# 2. For each file: open it, choose the correct resolution
#    Options:
#    a) Keep ours:    git checkout --ours   src/config.ts
#    b) Keep theirs:  git checkout --theirs src/config.ts
#    c) Manual edit:  edit the file to combine both changes correctly

# 3. After editing each file
git add src/config.ts

# 4. Complete the operation
git commit               # after git merge
git rebase --continue    # after git rebase
# or abort entirely:
git merge --abort
git rebase --abort
```

**Real-world example** — both branches added a function to the same file:
```ts
<<<<<<< HEAD
function validateEmail(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}
=======
function validateEmail(email: string): boolean {
  return email.includes('@') && email.includes('.');
}
>>>>>>> feature/auth
```
Correct resolution: keep the stricter regex from `HEAD`, discard the weaker check. Delete the markers and save.

---

## Interactive Rebase (Rewriting History)

Use before opening a PR to clean up "WIP" commits:

```bash
git rebase -i HEAD~4    # rewrite last 4 commits
```

The editor opens with:
```
pick a1b2c3 WIP: start login
pick d4e5f6 fix typo
pick 789abc WIP: finish login
pick 012def add tests
```

Change `pick` to:
- `squash` (or `s`) — fold into the commit above it
- `reword` (or `r`) — change the commit message
- `drop` (or `d`) — delete the commit entirely
- `fixup` (or `f`) — squash + discard this commit's message

Result after squash:
```
pick a1b2c3 feat: add login with email/password
pick 012def test: add login unit tests
```

**Rule**: Only rebase commits that haven't been pushed to a shared branch.

---

## Finding Bugs with `git bisect`

When you know "it worked at commit X, broken at HEAD," use bisect:

```bash
git bisect start
git bisect bad                  # current HEAD is broken
git bisect good v1.4.0          # last known-good tag or commit SHA

# Git checks out a midpoint commit automatically
# Test it, then mark:
git bisect good    # or:
git bisect bad

# Git narrows down (~log₂(n) steps for n commits)
# When done:
# "d3f1a9 is the first bad commit"
git bisect reset   # return to HEAD
```

Automate with a test script:
```bash
git bisect start HEAD v1.4.0
git bisect run npm test -- --testPathPattern=login
# git bisect runs npm test at each midpoint, marks good/bad automatically
```

---

## Cherry-Pick

Apply a specific commit to another branch (e.g., backporting a hotfix):

```bash
git checkout main
git cherry-pick abc1234 -x     # -x appends "cherry picked from commit abc1234" to message

# Range of commits
git cherry-pick abc1234..def5678

# Conflict during cherry-pick:
git cherry-pick --continue     # after resolving
git cherry-pick --abort        # abandon
```

---

## Tag Management

```bash
# Annotated tag for releases (includes tagger, date, message)
git tag -a v1.2.0 -m "Release 1.2.0 — adds OAuth login"

# Push tags (not pushed with git push by default)
git push origin v1.2.0
git push origin --tags         # push all tags

# List tags
git tag -l "v1.*"

# Delete a tag
git tag -d v1.2.0              # local
git push origin --delete v1.2.0  # remote
```

---

## Branch Naming & Lifecycle

### Naming convention
```
<type>/<short-description>
<type>/<TICKET-123>-short-description   ← include ticket number when available
```

| Type | Use for | Example |
|------|---------|---------|
| `feature/` | New functionality | `feature/user-authentication` |
| `fix/` | Bug fixes | `fix/login-timeout` |
| `chore/` | Maintenance, deps | `chore/update-node-20` |
| `docs/` | Documentation only | `docs/api-reference` |
| `refactor/` | Code restructuring | `refactor/payment-module` |
| `test/` | Test additions | `test/checkout-flow` |

Rules: lowercase + hyphens only, 3–5 words max.

### Creating a branch
```bash
git fetch origin
git checkout -b feature/my-feature origin/main
git push -u origin feature/my-feature   # push early, opens PR tracking
```

### Cleaning up stale branches
```bash
# Prune remote tracking refs for deleted remote branches
git fetch --prune

# Branches already merged into main (safe to delete)
git branch --merged main

# All branches by last commit date (find old ones)
git for-each-ref --sort=-committerdate refs/heads/ \
  --format='%(committerdate:short) %(refname:short)'

# Delete merged local branch
git branch -d feature/done
# Delete remote branch
git push origin --delete feature/done
```

Delete candidates: merged to main, no commits in 30+ days, no open PR.

---

## Useful Diagnostics

```bash
# Visualize branch graph
git log --oneline --graph --decorate --all | head -30

# Find which commit introduced a line
git log -S "functionName" --source --all

# Show what changed between two branches
git diff main...feature/login    # only changes on feature branch

# Find commits touching a file
git log --follow --oneline -- src/auth/login.ts

# Undo last commit but keep changes staged
git reset --soft HEAD~1

# Discard all local changes (destructive)
git checkout .
```
