---
domain: claude-code-hooks
source_task: 2026-05-12-fix-agentic-hook-paths.md
date: 2026-05-12
keywords: ["hook cwd", "absolute path", "settings.json", "module not found", "MODULE_NOT_FOUND", "cross-project sessions"]
---

## Extracted Knowledge

### Hook commands resolve against the hook runner's cwd, not the settings file's location

In `.claude/settings.json`, a hook entry like:

```json
"command": "node .claude/hooks/post-stop.cjs"
```

…uses a *relative* path. Claude Code resolves it against the cwd of the
process that invokes the hook — which is NOT guaranteed to be the directory
containing the `settings.json` that registered the hook.

**Observed in practice**: a session started in `/home/jojo/agentic/` (where
the hook is registered) but doing `cd /home/jojo/Documents/ProbBrain/` Bash
work and file edits will have its Stop hook fire with cwd
`/home/jojo/Documents/ProbBrain/`. The relative path then resolves to
`/home/jojo/Documents/ProbBrain/.claude/hooks/post-stop.cjs` — which doesn't
exist. Node throws:

```
Error: Cannot find module '/home/jojo/Documents/ProbBrain/.claude/hooks/post-stop.cjs'
code: 'MODULE_NOT_FOUND'
```

Claude Code surfaces this as a non-blocking Stop hook error notification on
every turn. The session keeps working; the hook just never runs.

### Failure is louder for Stop than for Pre/Post tool-use hooks

In the same session, the other three hook events (UserPromptSubmit,
PreToolUse, PostToolUse) appeared to still fire correctly with their
relative paths — only Stop failed. The cwd context Claude Code passes to
Stop hooks seems to drift further from the original project root than for
mid-turn hooks. Don't trust that "the other hooks work, so this one will" —
write defensively for the worst case.

### Fix: absolute paths

Replace every `node .claude/hooks/<name>.cjs` in `settings.json` with
`node /absolute/path/to/.claude/hooks/<name>.cjs`. Works regardless of cwd.
On a single-user / single-machine project, hardcoding the absolute path is
fine. For a project meant to be cloned by others, prefer
`node ${CLAUDE_PROJECT_DIR}/.claude/hooks/<name>.cjs` if your Claude Code
version supports that variable expansion in hook commands.

### Validation pattern after editing settings.json

Always re-verify after rewriting hook paths:

```bash
python3 -m json.tool /path/to/.claude/settings.json > /dev/null && echo OK
grep -E '"command": "node' /path/to/.claude/settings.json
for f in /path/to/.claude/hooks/*.cjs; do test -f "$f" && echo OK $f; done
```

A typo in the new absolute path causes the same silent-failure mode — and
because Claude Code only surfaces the error after the next Stop event, you
might not notice until much later.

### Multi-project sessions are the trigger

If a session only ever touches one project, this bug is dormant. It surfaces
when the operator switches projects mid-session via Bash `cd`, file edits in
other directories, etc. — any of which can shift Claude Code's notion of the
"current project root". Hook config in multi-project setups (operator
profile / "agentic" style) should default to absolute paths.

## Proposed Skill Content

Extend the `claude-code-hooks` skill with:

1. **Always use absolute paths in `settings.json` hook commands.** Relative
   paths resolve against the hook runner's cwd, which can drift to other
   projects during cross-project sessions.
2. **`${CLAUDE_PROJECT_DIR}`** (where supported) is the portable alternative
   for templates/repos meant to be cloned.
3. **Validate `settings.json` after every hook-config edit** with
   `python3 -m json.tool` + a spot check that every command's target file
   exists.
4. **Stop hook errors are non-blocking but visible**: each turn renders the
   error in the UI. If you see `MODULE_NOT_FOUND` repeatedly after switching
   projects mid-session, it's this bug.
5. **Don't infer hook health from one event type to another**: UserPrompt
   /Pre/Post hooks can fire with a cwd that still matches the project root,
   while Stop fires from somewhere else entirely.
