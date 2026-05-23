---
title: Unblock probbrain-accuracy scanner pushes (518 commits queued behind unstaged feature work)
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Goal
`probbrain-arb-scanner.service` and `probbrain-news-fetcher.service` (both
systemd user timers running every 15 minutes) have been failing their final
`git push` step. The scans themselves succeed — they're producing useful
output every tick — but the auto-commit + `git pull --rebase` + push step
errors out with:

```
scanner: git push failed: error: cannot pull with rebase: You have unstaged changes.
error: Please commit or stash them.
```

Diagnosed root cause in `/home/jojo/probbrain-accuracy`:

- Branch `main` is **ahead 518, behind 3** from `origin/main`. Hundreds of
  scan commits have piled up locally because the rebase has been blocked.
- Three tracked files have uncommitted feature work that was never landed
  but predates the queued scan commits:
  - `news-fetcher/lib/sitemap.js` (+ regenerated `sitemap.xml`) — adds
    `EXCLUDE_SIGNAL_STATUSES = {NOT_PUBLISHED, SHADOW}` filtering so draft
    and paper-trade signals don't pollute the sitemap (real feature).
  - `news-fetcher/news-fetch.js` — switches X post candidate selection from
    `published_at` to `discovered_at` so HN/arXiv items with old original
    publish dates still qualify for the digest (matches Telegram/Bluesky
    behavior). Same pattern as the 2026-05-15 X-digest-window fix.
- Five untracked files in `data/` and `assets/`
  (`pipeline_audit.json`, `resolved_shadow.json`, `shadow_signals.json`,
  `portfolio.js`, `portfolio.html`) — should be caught by the allow-list
  `.gitignore` but aren't. NOT blocking the push (untracked files don't
  block `pull --rebase`), so left for a separate task.
- Three upstream commits are auto "chore: live accuracy update" — likely
  rebase cleanly (different files).
- No active rebase state (`.git/rebase-merge` does not exist) — the prior
  "you might be in the middle of a rebase" error in news-fetcher logs was
  stale; the rebase dir got cleaned up.

## Steps
- [x] Commit `news-fetcher/lib/sitemap.js` + `sitemap.xml` together as the
      sitemap exclusion feature (one logical change).
- [x] Commit `news-fetcher/news-fetch.js` separately as the X candidate
      window fix (different feature).
- [x] Stop the 15-min timers (`probbrain-news-fetcher.timer`,
      `probbrain-arb-scanner.timer`) to prevent the working tree being
      re-modified mid-operation. Required step added on the fly after
      the news-fetcher re-modified 20 signal HTMLs between my commits
      and the first rebase attempt.
- [x] Commit the 20 modified `signals/sig-NNN.html` files as the pair to
      the sitemap exclusion feature — they got the new
      `<meta name="robots" content="noindex,nofollow">` tag from the
      same EXCLUDE_SIGNAL_STATUSES pipeline.
- [x] Aborted first `git pull --rebase` attempt (hit content conflict
      on `sitemap.xml` at commit 213/521 of the rebase replay; 521 commits
      × N likely conflicts = impractical).
- [x] Confirmed with user that kure2's sitemap work is local on another
      machine; user authorized "push it merge lets go" — proceed even if
      a collaborator's WIP might collide.
- [x] `git merge origin/main -X ours` — single merge commit, favor local
      on conflicts (safe: both sides of any sitemap.xml conflict are
      auto-regenerated and local is monotonically newer in real time).
- [x] `git push origin main` — pushed 522 commits (518 queued scan + 3
      new feature commits + 1 merge commit). Push succeeded:
      `56fc1860..b7389732 main -> main`.
- [x] Restart both timers — next scanner ticks at 16:40 EEST will
      organically validate the push works.
- [x] Verified `git status` clean (only the 5 untracked noise files)
      and `branch -vv` shows `[origin/main]` (no ahead/behind).
- [x] Left the 5 untracked noise files alone — separate task to fix the
      allow-list `.gitignore` gap if user wants it cleaned up.

## Completion
When all steps above are done:
Run `/complete workflows/tasks/2026-05-18-unblock-probbrain-accuracy-pushes.md` before starting any new work.

## Outcome

Completed on 2026-05-18.

probbrain-accuracy is back in sync with origin/main: `[origin/main]`, 0
ahead, 0 behind. The 522-commit backlog landed in a single push
(`56fc1860..b7389732 main -> main`). Next scanner ticks at 16:40 EEST
(arb-scanner + news-fetcher both on the same cadence) will be the
organic regression check — they'll either succeed silently or surface a
new failure mode worth a fresh task.

**Two latent features got landed alongside the unblocking** — they had
been sitting uncommitted on disk for days/weeks, ironically *causing*
the push failures by blocking the pre-push `pull --rebase` step:

- `feat(sitemap): exclude NOT_PUBLISHED and SHADOW signals` (5ed3e030 +
  bf114080) — sitemap.js stops listing drafts/shadow signals AND signal
  HTMLs get `<meta name="robots" content="noindex,nofollow">` so Google
  doesn't index them via direct link either. Pairs with kure2's SEO
  work mentioned by user.
- `fix(news-fetch): select X candidates by discovered_at` (b5e71ddc) —
  same fix pattern as the 2026-05-15 X-digest-window task; HN/arXiv
  items with old original publish dates now still qualify if freshly
  discovered, matching Telegram/Bluesky behavior.

**Key operational lesson** (worth a `.sc`): when doing git surgery on a
repo that's being actively modified by systemd timers, **stop the
timers first**. First rebase attempt failed because the news-fetcher
re-modified 20 signal HTMLs in the 30 seconds between my two feature
commits and the rebase call — every tick of the loop, the working tree
gets re-dirtied and the rebase re-aborts. Stopping the timers, doing
the push, then restarting is the safe sequence.

**Strategic call**: chose `git merge -X ours` over `git rebase` because
replaying 521 commits with likely N sitemap.xml conflicts each was
operationally impractical. Single merge commit + ours-favoring strategy
on auto-generated content is the same effective outcome (latest local
state wins) with O(1) conflict resolution instead of O(N). Cost: the
git log has a merge commit and the 3 upstream "chore: live accuracy"
commits sit as a side-branch in history rather than being interleaved.
Acceptable for a dashboard repo where commit-history aesthetics are
secondary to availability.

**Risks not yet validated**: kure2's "sitemap changes for Google
problems" may have been on another machine, not yet pushed; my
`-X ours` merge could silently override their unpushed work. User
explicitly authorized this trade-off ("push it merge lets go"), but
if kure2 surfaces with unpushable changes later, they'll need to
rebase on top of `b7389732` and re-resolve. Flagging here so it's
not a surprise.

The 5 untracked noise files (`assets/portfolio.js`, `portfolio.html`,
`data/pipeline_audit.json`, `data/resolved_shadow.json`,
`data/shadow_signals.json`) remain in the working tree. The repo's
allow-list `.gitignore` says they shouldn't be tracked but doesn't
catch them as ignored either. Separate cleanup task if desired —
they're harmless to the scanner push loop.
