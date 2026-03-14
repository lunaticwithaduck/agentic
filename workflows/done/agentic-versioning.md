---
title: General agentic versioning — user-facing release tracking
status: idea
created: 2026-03-12
---

## Idea

Give agentic a public version number so users can tell what they installed, know
when updates are available, and reference a specific version in bug reports.

## Why Now Is the Right Time to Think About It

The Cursor port is live. There are now two platforms to keep in sync. The risk of
divergence between what `ship/claude-code/` and `ship/cursor/` contain is real.
A version number makes "are these two in sync?" answerable.

Also: the first real client (Cursor user) means there's now someone on the outside
who could install agentic, leave it in place for months, and need to know how old
their install is.

## Proposed Approach

**Minimal: a VERSION file**

```
0.1.0
```

Stored at the repo root. Bumped manually before each meaningful release.
`build.sh` reads it and injects it into the ship manifests.

Installed users would see it at `your-project/.claude/VERSION` or
`your-project/.cursor/VERSION`. A `/status` command could read and display it.

**Changelog**

A `CHANGELOG.md` in the repo root tracking breaking changes, new features, and
fixes per version. Maintained by humans (or by Claude via a `/release` command that
runs the `/complete` pattern against a milestone).

## Versioning Conventions

Proposed semantics for this project:
- **Major**: breaking change to the install structure (hook contracts, workflow format, etc.)
- **Minor**: new features (new suite, new skill system capability, new platform support)
- **Patch**: fixes and improvements that don't affect existing installs

v0.x = pre-stable. API and structure may change. v1.0 = stable install contract —
upgrading from v1.x to v1.y doesn't break existing projects.

## What We're At Right Now

Informally: `v0.1` — the core loop (skills, autolearn, benchmarks, dual-platform ship)
is working end-to-end. Not stable enough for v1.0 (Cursor port has known gaps,
command files aren't Cursor-adapted, `/agent` is a no-op on Cursor).

## Deferred Until

We have a second install outside the development repo that we need to update.
Until then, "git sha" is sufficient and the overhead of managing a version number
isn't justified.
