---
title: Copilot Port — Pre-Testing Findings
created: 2026-03-15
status: open
---

# Copilot Port — Pre-Testing Findings

Audit performed 2026-03-15 ahead of first VS Code / GitHub Copilot live test.
All 33 shipped files were read. No files were modified.

---

## Hook Output Format — The Critical Open Question

This is the highest-priority unknown and must be resolved first during testing.

**What the code currently does:**

`skill-detector.cjs` outputs **plain text** to stdout — not JSON. Example:

```
[REQUIRED — Before writing any code or files]
workflows/tasks/ is empty. You MUST do this first:
1. Break the work into logical units...
```

`block-secrets.cjs` also outputs plain text when running as `.github/` (Copilot):

```js
// Copilot behaves like claude-code for output format (plain text, not JSON)
// const isCopilot = _hooksParent === '.github';
function block(reason) {
  if (isCursor) {
    process.stdout.write(JSON.stringify({ permission: 'deny', user_message: reason }));
  } else {
    process.stdout.write(reason + '\n');  // ← plain text for copilot
  }
  process.exit(2);
}
```

**The open question:** VS Code's GitHub Copilot hook system may expect `{"systemMessage":"..."}` JSON format for UserPromptSubmit output (to inject into context), not raw plain text. If it expects JSON and receives plain text, skill injections will either be silently dropped or cause a parse error. This is **unverified** and is the first thing to confirm during testing.

**What to test:** Fire a prompt that matches a keyword (e.g. "help me with a dockerfile"). Check whether the injected skill content actually appears in Copilot's context window. If it doesn't appear, the output format is the likely culprit.

---

## Findings by File

### 1. `.github/hooks/hooks.json`

**Severity: NOTE**

The structure is a top-level `{"hooks": {...}}` object using Claude Code-style event names (`UserPromptSubmit`, `PreToolUse`, `PostToolUse`, `Stop`). This matches what the project intends. However, the VS Code Copilot hooks API may use different event names or a different registration schema than what's here. This has not been tested against a real VS Code install.

Commands use relative paths (`node .github/hooks/skill-detector.cjs`). This assumes the CWD is the project root when hooks fire. If VS Code runs hooks from a different working directory, the relative path will fail. No absolute path fallback exists.

---

### 2. `.github/hooks/skill-detector.cjs`

**Severity: BLOCKER (conditional — depends on hook output format answer)**

- Output format is plain text (see Critical Open Question above). If VS Code expects JSON for UserPromptSubmit injection, the skill system will be silently non-functional.
- Pipeline reminder text (the "workflows/tasks/ is empty" warning) is also plain text. Same concern.
- Reads `workflows/tasks/` to decide whether to inject the task-creation reminder. The tasks directory check fires on every single prompt, including casual questions. If the tasks dir is empty, it prefixes every prompt response with a multi-line gate warning. This may feel aggressive to first-time users during initial exploration.
- The autolearn synthesis block refers to `bench/fixtures/skill-prompts.json` in the synthesis instructions (step 3 of the inline synthesis guide injected at runtime). This is an internal agentic path that only exists in the source repo, not in a user's project after they copy `ship/copilot/` in.
- Comment on line 4 says "Copilot port of `.claude/hooks/skill-detector.cjs`" — innocuous developer comment, not user-facing.

---

### 3. `.github/hooks/block-secrets.cjs`

**Severity: FRICTION**

- Correctly uses plain text output for the Copilot/Claude path.
- Exit code 2 is used to block. Whether VS Code respects exit code 2 from a PreToolUse hook as a block signal (vs. zero/non-zero) is untested.
- The git-add blocking patterns include `git add -A` and `git add .` — these will fire and block normal staging workflows. A Copilot user trying to stage all files will hit this and may not understand why the AI refuses. The block message is reasonably clear but the breadth of the block may surprise users.
- Comment on line 8: "Detect platform: running from `.cursor/hooks/`, `.github/hooks/`, or `.claude/hooks/`" — this is a developer comment, not user-facing, but confirms the platform logic is present and correct.

---

### 4. `.github/hooks/post-write.cjs`

**Severity: NOTE**

- Platform detection is correct: `isCopilot` is true when `path.basename(path.dirname(__dirname)) === '.github'`, and `pendingFile` is correctly set to `.github/autolearn-pending`. No path leak here.
- The `filePath` it reads from stdin uses `d.tool_input || d` — this may not match the exact JSON schema VS Code sends for PostToolUse events. The field name VS Code uses for the written file path in its hook payload is unverified.
- If `filePath` is not resolved (because the field name doesn't match), the hook exits silently via `process.exit(0)`, so it fails gracefully. But the autolearn trigger would never fire.

---

### 5. `.github/hooks/post-stop.cjs`

**Severity: NOTE**

Placeholder only — exits 0 immediately. No issues. Whether VS Code fires a `Stop` event at all (analogous to Claude Code's Stop hook) is an open question but the hook handles this gracefully by doing nothing.

---

### 6. `.github/copilot-instructions.md`

**Severity: NOTE (minor cosmetic)**

The file is well-structured and Copilot-appropriate. Paths correctly reference `.github/`. Key observations:

- Layer 3 skill injection is described as "Automatic injection via UserPromptSubmit hook" — this is accurate but conditional on the hook output format working (see Critical Open Question). If hooks don't work, Layer 3 silently disappears and only Layers 1 and 2 remain.
- The "Getting Started" section says: "Copy `ship/copilot/` contents into your project root." This is slightly ambiguous — does it mean copy `.github/` and `workflows/` and `setup.sh` individually, or the entire `ship/copilot/` tree? Users may copy the `ship/copilot/` directory itself rather than its contents, which would break the `.github/` path resolution.
- No references to `.claude/` paths. Workflow pipeline description is correct.

---

### 7. `.github/instructions/workflow-gate.instructions.md`

**Severity: NOTE**

The file uses `applyTo: "**/*"` frontmatter, which should make it always-on in VS Code Copilot. The instructions are mechanical and numbered as required. No `.claude/` path leaks. The file is tight and functional.

One subtle issue: the `.sc` evaluation block in step 3 says to check whether `.cursor/rules/[domain].mdc` or `.claude/skills/[domain].md` already exists before triggering synthesis. This is correct multi-platform logic but a pure Copilot user would see references to `.cursor/` paths, which may cause confusion (the check is in the instructions shown to Copilot, not hidden in a comment).

---

### 8. `.github/prompts/agent.prompt.md`

**Severity: BLOCKER**

Every agent path reference points to `.claude/agents/` — the wrong directory for a Copilot install:

```
- `pm` → `.claude/agents/project-manager.md`
- `architect` → `.claude/agents/architect.md`
- `worker` → `.claude/agents/worker.md`
...
5. Read the corresponding agent definition from `.claude/agents/`
```

The correct paths are `.github/agents/project-manager.agent.md` etc. When a user runs `/agent pm do X`, Copilot will attempt to read from `.claude/agents/` which does not exist in a Copilot install, and will either fail or hallucinate the agent definition.

Additionally, step 6 says "Spawn a Task subagent with the agent definition as context" — `Task subagent` is Claude Code's specific multi-agent API. VS Code Copilot has no equivalent spawn mechanism. This instruction will cause Copilot to either attempt something it can't do or produce a confused response. The entire agent dispatch model needs Copilot-appropriate language (e.g., "read the agent definition and adopt that persona for this response").

---

### 9. `.github/prompts/clean.prompt.md`

**Severity: FRICTION**

Two path leaks under the "Stale Skills" section:

```
- Read `.claude/skill-usage.json` (if it exists)
- Read `.claude/skills/skill-rules.json`
...
- move to `.claude/skills/archived/`
```

Correct Copilot paths would be:
- `.github/skill-usage.json`
- `.github/skills/skill-rules.json`
- `.github/skills/archived/`

When a user runs `/clean`, Copilot will look in `.claude/` for usage data and skill rules, find nothing, and either error out or silently skip skill health reporting.

---

### 10. `.github/prompts/status.prompt.md`

**Severity: FRICTION**

Path leak in the skill health section:

```
- (Read `.claude/skill-usage.json` and compare against `.claude/skills/skill-rules.json`)
```

Same issue as clean.prompt.md — wrong base directory. Also, the `VERSION` file reference in the header (`v[version from VERSION file]`) assumes a `VERSION` file exists at project root, but the Copilot ship includes a `.version` file (hidden, dotfile). Whether VS Code Copilot will find `.version` versus `VERSION` depends on whether the model reads the exact path.

---

### 11. `.github/prompts/setup.prompt.md`

**Severity: FRICTION**

The setup prompt contains explicit Claude Code-specific branches that will confuse a Copilot user:

- Step 0 detects platform by checking for `CLAUDE.md` → "Claude Code mode". In a Copilot install, `CLAUDE.md` doesn't exist, so this branch is irrelevant — but the text is visible and names Claude Code directly.
- Step 3 says: "**Claude Code**: The hook to update is `.claude/hooks/post-write.sh`"
- Step 4 heading: "MCP Servers (Claude Code only)" — explicitly describes Claude's MCP system
- Step 5 "Claude Code" branch references `CLAUDE.md`, `.claude/hooks/post-write.sh`, `.claude/settings.json`

A Copilot user running `/setup` will be shown instructions about Claude Code mode, told to update files that don't exist, and asked about MCP servers. The Cursor branch is handled (it checks for `.cursor/rules/agent-instructions.mdc`), but there is no positive Copilot detection branch — the setup prompt has no awareness that it might be running inside a Copilot install.

---

### 12. `.github/prompts/review.prompt.md`

**Severity: FRICTION**

Step 3: "Activate the `code-review` skill" — there is no `code-review` skill in the skill library (it was intentionally excluded per the design philosophy). The skill-rules.json only has `skill-creator`. This instruction will cause Copilot to attempt to activate a non-existent skill, likely producing no effect or a confusing response. Same issue in `auditor.agent.md` (line: "Review all changed files using the `code-review` skill").

---

### 13. `.github/agents/worker.agent.md`

**Severity: FRICTION**

Line 26: `Follow the 'implementation' skill in '.claude/skills/implementation.md'`

- `.claude/` path leak — wrong directory
- The `implementation` skill does not exist in the skill library

---

### 14. `.github/agents/project-manager.agent.md`

**Severity: FRICTION**

References `TaskCreate`, `TaskUpdate`, `TaskList` tools multiple times. These are Claude Code-specific MCP tool names (or internal tool names from the Claude Code SDK). GitHub Copilot has no such tools. The PM agent instructions tell Copilot to "Break the task into subtasks using TaskCreate" and "Monitor progress using TaskCreate/TaskUpdate/TaskList tools" — Copilot has no way to fulfill these instructions.

The PM agent will either confabulate these tool calls or skip the steps, silently breaking the orchestration model.

---

### 15. `.github/agents/architect.agent.md`

**Severity: NOTE**

References non-existent skills: `system-design`, `api-design`, `adr`, `scenario-compare`, `dependency-graph`, `impact-analysis`. None of these exist in `.github/skills/skill-rules.json`. The mermaid-diagram skill is also referenced but not present. When the architect agent is dispatched and tries to "activate" these skills, nothing will happen — silent degradation, not a hard failure.

Also references `TaskCreate` tool (same issue as PM agent, lower impact since architect is less frequently dispatched).

---

### 16. `.github/agents/refactorer.agent.md` and `researcher-documenter.agent.md`

**Severity: NOTE**

Both reference non-existent skills (`code-smell-detector`, `find-related`, `refactor`, `explain-code`, `readme-generator`, `api-docs`, `onboarding-guide`, `technical-writing`, `changelog`, `code-walkthrough`, `summarize`, `weekly-summary`). Same silent-degradation pattern as architect. `TaskUpdate` references present.

---

### 17. `.github/skills/skill-creator/SKILL.md`

**Severity: FRICTION**

The skill-creator SKILL.md is the only skill shipped in the Copilot port. It contains three `.claude/` path leaks:

- `file_patterns: [".claude/skills/**"]` in the frontmatter activation section
- "Create the file at `.claude/skills/<skill-name>.md`" in the instructions
- "Add an entry to `.claude/skills/skill-rules.json`" in the skill-rules.json update section
- "Add the new skill to the appropriate category row in the Skills Library table in `CLAUDE.md`"

When a user creates a new skill by triggering the skill-creator, they will be told to write files to `.claude/skills/` (which doesn't exist) and update `CLAUDE.md` (which doesn't exist). The resulting skill would be invisible to the Copilot skill system.

The `file_patterns` activation entry points to `.claude/skills/**`, so even the auto-detection trigger for the skill-creator won't fire on `.github/skills/` paths.

---

### 18. `.github/skills/skill-rules.json`

**Severity: FRICTION**

Only one skill is registered: `skill-creator`. The `filePatterns` entry is `[".claude/skills/**"]` — wrong path for Copilot. Skills being created or edited under `.github/skills/` will not trigger the skill-creator via file pattern matching. The keyword matching (`create skill`, `new skill`, etc.) will still work, but the file pattern trigger will not.

The skill library has 26 skills in the Claude Code port but only 1 is included in the Copilot ship. This is presumably intentional (skills need to be copied separately), but there is no documentation in the shipped files explaining that skill files from `.claude/skills/` need to be manually copied to `.github/skills/<name>/SKILL.md`. The `copilot-instructions.md` mentions skills exist but a new user has no skills to reference and no clear path to add them.

---

### 19. `setup.sh`

**Severity: NOTE**

The script correctly creates workflow directories and verifies `.github/` infrastructure. No `.claude/` path references. The GitHub CLI check is a nice touch.

However, the script uses `SCRIPT_DIR` as its base (`"$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"`), which means it must be run from the `ship/copilot/` directory — `bash setup.sh` from the project root where the user has copied the files will work correctly only if they copied `setup.sh` to the root. If they forgot to copy it out of `ship/copilot/`, the mkdir commands will create directories inside `ship/copilot/` rather than the user's project root.

---

### 20. No PLATFORM-PARITY.md exists

**Severity: NOTE**

No parity comparison document was found anywhere in the repo (searched for `*PARITY*` globally — no results). The cursor-port-limitations and cursor-commands-not-adapted problem files partially serve this purpose for the Cursor port, but there is no equivalent for Copilot. There is no shipped document telling a Copilot user what features are unavailable vs. Claude Code or Cursor.

---

## Summary Table

| # | File | Issue | Severity |
|---|------|-------|----------|
| 1 | `hooks/skill-detector.cjs` | Output is plain text — unknown if VS Code expects JSON for context injection | BLOCKER |
| 2 | `prompts/agent.prompt.md` | All agent paths point to `.claude/agents/` not `.github/agents/` | BLOCKER |
| 3 | `prompts/agent.prompt.md` | "Spawn a Task subagent" — Claude Code-specific API, no Copilot equivalent | BLOCKER |
| 4 | `prompts/setup.prompt.md` | Copilot detection branch missing; Claude Code + MCP instructions shown to Copilot users | FRICTION |
| 5 | `prompts/clean.prompt.md` | `.claude/skill-usage.json` and `.claude/skills/` path leaks | FRICTION |
| 6 | `prompts/status.prompt.md` | `.claude/skill-usage.json` and `.claude/skills/skill-rules.json` path leaks | FRICTION |
| 7 | `prompts/review.prompt.md` | References non-existent `code-review` skill | FRICTION |
| 8 | `agents/project-manager.agent.md` | `TaskCreate`, `TaskUpdate`, `TaskList` tools don't exist in Copilot | FRICTION |
| 9 | `agents/worker.agent.md` | `.claude/skills/implementation.md` path leak; `implementation` skill doesn't exist | FRICTION |
| 10 | `agents/architect.agent.md` | 6 non-existent skill references; `TaskCreate` reference | NOTE |
| 11 | `agents/refactorer.agent.md` | 4 non-existent skill references; `TaskUpdate` reference | NOTE |
| 12 | `agents/researcher-documenter.agent.md` | 8 non-existent skill references | NOTE |
| 13 | `skills/skill-creator/SKILL.md` | 4 `.claude/` path leaks; skill creation sends users to wrong directory | FRICTION |
| 14 | `skills/skill-rules.json` | `filePatterns` points to `.claude/skills/**` not `.github/skills/**` | FRICTION |
| 15 | `hooks/block-secrets.cjs` | Exit code 2 for blocking — untested whether VS Code respects this | NOTE |
| 16 | `hooks/post-write.cjs` | VS Code hook payload field name for file path is unverified | NOTE |
| 17 | `copilot-instructions.md` | "Copy ship/copilot/ contents" is ambiguous about what to copy | NOTE |
| 18 | `setup.sh` | Must be run from correct directory; no verification of where it's being run | NOTE |
| 19 | (all agent files) | `code-review` skill referenced in auditor.agent.md; doesn't exist | NOTE |
| 20 | (repo-wide) | No PLATFORM-PARITY.md documenting Copilot limitations vs. Claude Code | NOTE |

---

## Recommended Testing Order

1. **First**: Verify hook output format — fire a keyword-matching prompt and confirm skill content appears in context. This unblocks or invalidates half the other findings.
2. **Second**: Run `/agent worker do something` — will immediately surface the `.claude/agents/` BLOCKER.
3. **Third**: Run `/setup` — will surface the Claude Code confusion in that prompt.
4. **Fourth**: Run `/clean` and `/status` — will surface the skill path leaks.
5. **Fifth**: Try to create a new skill — will surface the skill-creator path leaks.
