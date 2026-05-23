---
domain: git-operations
source_task: 2026-05-18-unblock-probbrain-accuracy-pushes.md
date: 2026-05-18
keywords: ["git rebase", "git merge", "unstaged changes", "pull rebase failed", "systemd timer race", "auto-commit scanner", "merge -X ours"]
---

## Extracted Knowledge

### Stop systemd timers before git surgery on a repo they modify

When you're untangling a git state in a repo that has systemd user
timers running services which auto-modify files (scanners, fetchers,
generators), **stop the timers first**. The race is real:

```
git commit -m "..."          # OK
sleep 30                      # ← service tick fires during this gap
git pull --rebase             # FAILS: scanner re-modified 20 files
```

Symptom: every rebase attempt aborts with "cannot pull with rebase: You
have unstaged changes" even though you *just* committed everything. The
working tree gets re-dirtied between operations by the running service.

Recovery sequence:

```bash
systemctl --user stop foo.timer bar.timer
systemctl --user is-active foo.service bar.service  # confirm not mid-run
# do git work — commits, rebase/merge, push
systemctl --user start foo.timer bar.timer
```

Stopping the `.timer` units prevents new ticks; existing service runs
continue to completion. After git surgery is done, restart the timers
and the next tick validates the fix organically.

### `git merge -X ours` over `git rebase` for large local backlogs

When a branch is hundreds of commits ahead of origin and the upstream
has only a handful of commits behind, **prefer `git merge -X ours`
over `git rebase`**. Math:

| Operation | Conflict resolutions required |
|---|---|
| `git rebase origin/main` | Up to (N local commits) × (M overlapping files) — replays N commits, each can conflict |
| `git merge origin/main -X ours` | 1 merge commit, all conflicts auto-resolved in one pass |

For auto-generated content (sitemap.xml, regenerated JSON, dashboards)
where both sides of any conflict are regenerable scanner output and
the local side is monotonically newer in real time, `-X ours` is
strictly safe — the latest local generation wins, which is what the
next scanner tick would produce anyway.

Cost of merge vs rebase: a merge commit in history and a side-branch
of the upstream commits. Acceptable for dashboard repos where
commit-history aesthetics are secondary to availability. Don't pick
this for repos where linear history matters (release branches,
audit trails).

Critical caveat: `-X ours` will silently override real human work on
the upstream side if it touched the same files. Verify by
`git log origin/main..HEAD` and `git log HEAD..origin/main` plus
`--stat` on the upstream commits before merging; if upstream looks
like auto-bot churn ("chore: live accuracy update [timestamp]"), it's
probably safe.

### `-X ours` vs `-X theirs` semantics differ between merge and rebase

This bit me. The two operations swap semantics:

| Strategy | Merge | Rebase |
|---|---|---|
| `-X ours` | favor HEAD (current branch) | favor the branch being rebased ONTO (upstream) |
| `-X theirs` | favor MERGE_HEAD (branch being merged in) | favor the commit being replayed (local) |

So `git merge origin/main -X ours` favors local; `git rebase origin/main -X ours` favors UPSTREAM. Same flag, opposite meaning. Always sanity-check
by reading `git status` after the operation and inspecting a sample
conflicted file before pushing.

### Stale rebase-state warnings in service logs are not authoritative

A service log saying "I wonder if you are in the middle of another
rebase. Please try git rebase (--continue | --abort | --skip) or
rm -fr .git/rebase-merge" may be reporting a state that has since been
cleaned up. Don't trust the log message — check the filesystem:

```bash
ls /path/to/repo/.git/rebase-merge /path/to/repo/.git/rebase-apply
```

If both `ls` calls error with "No such file or directory", there is no
active rebase regardless of what the service logs say. The warning was
likely from a previous run where the directory did exist briefly.

### Diagnosing "ahead N, behind M" patterns

`git branch -vv` output like `* main 2d354343 [origin/main: ahead 518, behind 3]` carries diagnostic signal:

- **Large ahead, small behind, auto-bot upstream commits** → accumulated
  scanner/bot output that couldn't push (the case here). Safe to merge
  with `-X ours` or just resolve and push, since both sides are
  auto-regenerable.
- **Large ahead, large behind** → divergent histories, likely a missed
  sync over a long period. Manual review of upstream commits required;
  do not auto-resolve.
- **Small ahead, large behind** → you forgot to pull for a long time;
  standard `pull --rebase` should work if working tree is clean.
- **Small ahead, small behind** → normal collaborative state; standard
  rebase or merge.

The "ahead" count is the most useful incident signal: anything above
~50 unpushed commits on a scanner-driven repo means the push loop has
been broken for days/weeks and needs explicit unblocking, not just a
retry.

### Look at uncommitted diffs before deciding "discard or commit"

When a repo has uncommitted tracked changes blocking a rebase, the
diff often contains intent that's worth keeping. Read it before
deciding:

```bash
git diff --stat                    # see what files and how much
git diff path/to/key/file | head   # see actual changes
git log --oneline -5               # see what was committed locally
```

In this task: 3 modified files looked like auto-generated noise but
were actually two real, never-committed features (sitemap exclusion +
X candidate window fix) that had been blocking the push for weeks.
Stashing or discarding would have lost them. The right move was to
commit them with proper messages, then unblock the push.

Same diff-before-discard discipline applies to the "scanner re-modified
files between commits" scenario: read what the scanner actually changed
to decide whether to commit (real feature output) or discard (transient
intermediate state).

## Proposed Skill Content

A `git-operations` skill should activate on prompts mentioning git
rebase/merge conflicts, push failures, "ahead N behind M" debugging,
auto-commit pipelines, or repo state recovery. It should teach:

1. **Stop timers/services that modify the working tree before doing git
   surgery.** Restart after the push lands. The race is silent and
   self-perpetuating: every attempt re-dirties the tree.
2. **`git merge -X ours` beats `git rebase` for 100+ local commits**
   with auto-generated file conflicts, when both sides of conflicts are
   regenerable and local is monotonically newer.
3. **Memorize that `-X ours` flips meaning between merge and rebase.**
   In merge, favors local; in rebase, favors upstream.
4. **Trust the filesystem, not the service log** for rebase state.
   `ls .git/rebase-merge` is the source of truth.
5. **Read `git diff` on blocking unstaged changes before deciding** to
   commit/stash/discard. Often the "noise" is real never-committed
   feature work that's *causing* the push loop to fail.
6. **"ahead 100+, behind 3, upstream is bot churn" → scanner backlog
   pattern.** Single merge with `-X ours` is the standard recovery.
7. **Always verify with `git branch -vv` showing `[origin/branch]` with
   no ahead/behind** after the push completes. The scanner's next tick
   is the organic regression check; don't poll, just close the task
   and let the next tick validate.
8. **Confirm collaborator state before pushing large backlogs** that
   touch shared files. Other machines may have unpushed work that
   `-X ours` would silently clobber.
