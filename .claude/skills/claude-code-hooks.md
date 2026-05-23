---
name: claude-code-hooks
description: Configure, debug, and package Claude Code hooks — settings.json, lifecycle constraints, and common failure modes
activation:
  keywords: ["claude code hook", "settings.json hook", "post-stop", "pre-tool-use", "user-prompt-submit", "post-write", "MODULE_NOT_FOUND hook", ".cjs hook", "stop hook"]
---

## Purpose

Help author, debug, and package Claude Code hooks correctly. Hooks are shell
commands or Node scripts that Claude Code invokes at lifecycle events
(`UserPromptSubmit`, `PreToolUse`, `PostToolUse`, `Stop`, `SessionStart`).
They are configured in `.claude/settings.json` and are easy to break in
silent ways — this skill captures the gotchas that have actually bitten in
practice.

## Hook lifecycle and the cwd trap

Hook commands in `.claude/settings.json` are resolved against the **hook
runner's cwd**, not the directory that contains the `settings.json`.
Claude Code's notion of cwd can drift to a different project during a
session that does Bash work or file edits in another directory.

**Always use absolute paths** in hook commands:

```json
"hooks": {
  "Stop": [
    {
      "hooks": [
        {
          "type": "command",
          "command": "node /absolute/path/to/.claude/hooks/post-stop.cjs"
        }
      ]
    }
  ]
}
```

For templates/repos meant to be cloned, prefer
`node ${CLAUDE_PROJECT_DIR}/.claude/hooks/post-stop.cjs` if your Claude Code
version expands that variable in hook commands.

Symptom of a relative-path bug: `Error: Cannot find module '<some other
project's path>/.claude/hooks/post-stop.cjs'` with `MODULE_NOT_FOUND`, fired
on every turn after a project switch.

## Stop hook constraints (read carefully)

- Runs **after** Claude's final response — it cannot re-invoke Claude or
  inject new context into the same turn.
- Has full filesystem access (read/write) but **no conversation history**.
- Output goes to the user as a notification, not back into Claude's context.
- Cannot generate a "rich reflection" of the session — only mechanical state
  from files (open tasks, recent completions, version, timestamp).

Anything that requires understanding the conversation (key decisions,
"what to pick up next", architecture nuance) must be written manually by
Claude *before* Stop fires, not by the hook.

## Preserving Claude-authored notes across Stop hook runs

If your Stop hook regenerates a file (like `SESSION.md`) on every turn, you
must explicitly preserve any sections Claude wrote manually. Match the
section, check if it's still the placeholder, keep it if not:

```js
const m = existing.match(/## Session Notes\n\n([\s\S]*?)(?=\n---|\n## |\s*$)/);
if (m && m[1].trim() && !m[1].trim().startsWith('_Not yet written')) {
  return m[1].trim(); // preserve
}
return null; // use placeholder
```

Without this, every Stop hook run wipes the manually-written content.

## Stable vs volatile context split

When designing files the Stop hook writes (or any always-loaded context):

- **Stable** content (architecture, conventions, file paths): rarely
  changes, load once at session start, keep under ~150 lines.
- **Volatile** content (in-flight tasks, recent completions): regenerated
  every session by the hook, keep under ~50–80 lines.

Mixing them inflates a single file past Claude Code's effective 200-line
attention limit (start+end bias breaks down beyond that). Split into two
files (e.g. `CONTEXT.md` stable + `SESSION.md` volatile).

## Hook file packaging — copy by explicit filename

Hook implementations are `.cjs` (CommonJS, because `package.json` may set
`type: module` and break `.js` requires). When packaging hooks for
distribution (cursor / copilot ship builds), **never glob-copy `.js`** —
`.cjs` files will be silently missed:

```bash
# WRONG — misses .cjs files
cp .claude/hooks/*.js dist/.claude/hooks/

# RIGHT — explicit names
for hook in skill-detector.cjs block-secrets.cjs post-write.cjs post-stop.cjs; do
  src=".claude/hooks/${hook}"
  [ -f "${src}" ] && cp "${src}" "dist/.claude/hooks/${hook}"
done
```

The ship-time `settings.json` must reference the exact filenames the build
script copies — any drift means `settings.json` points at files that don't
exist and the hook fails silently at runtime.

`.sh` shims in `.claude/hooks/` are dev artifacts (so you can invoke a hook
from the shell during testing) and should be excluded from ship output.

## Validation pattern after every settings.json edit

```bash
python3 -m json.tool /path/to/.claude/settings.json > /dev/null && echo "JSON OK"
grep -E '"command":' /path/to/.claude/settings.json
for f in /path/to/.claude/hooks/*.cjs; do test -f "$f" && echo "OK  $f"; done
```

A typo in a hook command path causes silent failure for whichever event
type uses it — you may not notice until that event fires (often the Stop
event, which is only visible at end-of-turn).

## Failure Modes

These have all actually fired in practice:

1. **Relative path + cross-project session → `MODULE_NOT_FOUND`.** Hook
   command `node .claude/hooks/post-stop.cjs` resolves against the wrong
   project's directory after you `cd`/edit in another repo. Fix: absolute
   paths. Don't infer health from other hook events working — Stop is
   especially sensitive to cwd drift.

2. **Glob-copy misses `.cjs`.** Build script uses `cp *.js` or `cp *.sh`;
   the shipped `.claude/hooks/` is missing the `.cjs` implementations.
   `settings.json` references them anyway. All hooks silently no-op at the
   user's machine. Fix: copy by explicit filename.

3. **Stop hook regenerates SESSION.md and erases Claude's notes.** Without
   a preserve-existing-section guard, the hook overwrites the manually
   written `## Session Notes` content every turn. Fix: regex-match,
   detect placeholder, keep non-placeholder content.

4. **Always-loaded context inflates past 200 lines.** Hook keeps appending
   to `SESSION.md` without bounding it; eventually Claude's effective
   attention degrades on it. Fix: cap volatile sections; move long
   reference material to a separately-loaded file.
