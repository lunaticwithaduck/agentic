# Changelog

All notable changes to agentic are documented here.
Versioning follows [Semantic Versioning](https://semver.org): MAJOR.MINOR.PATCH.

- **MAJOR** — breaking change to install contract (hook output format, required file structure)
- **MINOR** — new capability, safe to update without reinstall
- **PATCH** — bug fix, no behavior change

---

## [0.1.1] — 2026-03-12

### Fixed
- **`.mdc` rule extensions** — all Cursor rule files renamed from `.md` to `.mdc`
  (`agent-instructions`, `skill-creator`, `skill-index`); `cursor-skill-injector.cjs`
  updated to scan `*.mdc`; synthesis instructions tell Claude to write `${domain}.mdc`;
  build script and verification checks updated throughout
- **`/setup` platform-aware** — detects `CLAUDE.md` vs `.cursor/rules/agent-instructions.mdc`
  at Step 0; Steps 3/4/5 branch per platform; MCP step skipped for Cursor; correct hook
  file paths referenced per platform
- **Workflow enforcement strengthened** — `CLAUDE.md` and `agent-instructions.mdc` now have
  an explicit `## Task Pipeline — REQUIRED` section naming TodoWrite, in-chat checklists,
  and built-in task UIs as invalid substitutes for `workflows/tasks/` files

---

## [0.1.0] — 2026-03-12

### Added
- **Cursor port** — full distribution target at `ship/cursor/` with 3-layer skill injection:
  Layer 1 (always-on skill index), Layer 2 (agentRequested rules), Layer 3 (afterFileEdit hook)
- **Multi-platform build** — `buildScripts/build.sh` produces `ship/claude-code/` and `ship/cursor/`
  from a single source, with platform-specific transforms (frontmatter conversion, path rewriting)
- **JS hook implementations** — all hooks rewritten in Node.js (`post-write.js`, `block-secrets.js`,
  `skill-detector.js`, `post-stop.js`); bash files are thin shims for backwards compatibility
- **Autolearn pipeline** — `.sc` candidating → domain threshold → synthesis injection → skill written,
  validated end-to-end; first auto-generated skill (`e2e-evaluation`) confirmed in production
- **7 benchmark suites** — infrastructure, skill detection, hook security, E2E quality, keyword
  overlap, token cost, and autolearn pipeline integrity
- **Platform parity doc** — `ship/PLATFORM-PARITY.md` with honest gap analysis

### Known Gaps (v0.1.0)
- Cursor keyword detection is probabilistic (Cursor `beforeSubmitPrompt` cannot inject context)
- Cursor command files reference `.claude/` paths in instructional text (cosmetic, AI adapts)
- `/agent` command is a no-op on Cursor (no subagent equivalent in Cursor)

---
