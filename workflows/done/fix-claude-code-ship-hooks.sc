---
domain: claude-code-hooks
source_task: fix-claude-code-ship-hooks.md
date: 2026-03-15
keywords: cjs hooks ship build packaging glob settings.json
---

## Extracted Knowledge

**The bug pattern**: A build script that copies hooks using a glob (`*.js *.sh`) will silently
miss `.cjs` files. The ship ends up with a `settings.json` that references `.cjs` paths that
don't exist — hooks fail silently at runtime with no obvious error.

**The correct pattern** (from cursor/copilot build scripts): Copy hook files explicitly by name:
```bash
for hook in skill-detector.cjs block-secrets.cjs post-write.cjs post-stop.cjs; do
  src="${CLAUDE_DIR}/hooks/${hook}"
  [ -f "${src}" ] && cp "${src}" "${DEST}/.claude/hooks/${hook}"
done
```

**`.sh` shims are dev artifacts, not ship output**: The `.sh` shims in `.claude/hooks/` exist
to let developers invoke hooks from the shell during development. They should never appear in
the ship directory — only the `.cjs` implementations belong there.

**`settings.json` is the ground truth**: Whatever path format appears in `settings.json`
hook commands (`node .claude/hooks/xxx.cjs`) must exactly match what the build script copies.
If these diverge, hooks are silently broken.

## Proposed Skill Content

### Claude Code Hook Packaging Rules

1. Hook files are `.cjs` (CommonJS) — always copy by explicit filename, never by glob
2. `settings.json` hook commands must match the filenames copied by the build script exactly
3. `.sh` shims belong in the development repo only — exclude them from ship output
4. After changing what the build script copies, re-run the build and verify `ship/` manually
5. Cross-reference with cursor/copilot build scripts when adding new hooks — all three ships
   share `block-secrets.cjs`, `post-write.cjs`, `post-stop.cjs`; only `skill-detector` varies
