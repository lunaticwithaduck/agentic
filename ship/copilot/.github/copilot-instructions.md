# copilot-instructions.md - Agentic Project Infrastructure

This project uses **agentic**, an AI-first infrastructure for GitHub Copilot.
It provides workflows, skills, hooks, prompts, and agents out of the box.

## Workflow Pipeline

Ideas flow through a pipeline:

1. **Ideas** (`workflows/ideas/`) - Raw ideas, feature requests, brainstorms
2. **Tasks** (`workflows/tasks/`) - Refined, actionable work items with clear acceptance criteria
3. **Done** (`workflows/done/`) - Completed work with outcome notes and `.sc` skill candidates
4. **Problems** (`workflows/problems/`) - Open design problems and known issues (not actionable yet)

Use `/idea`, `/promote`, `/complete`, and `/status` prompts to manage the pipeline.

## Skill System

Skills inject **domain-specific knowledge** for specialized domains. Three layers work together:

**Layer 1 — Always-on instructions** (`copilot-instructions.md`, this file): Provides project context and lists available skills. Always active in every session.

**Layer 2 — Prompt-attached skills** (via `#` file references in chat): Reference a skill file directly in Copilot Chat with `#.github/skills/skill-name/SKILL.md` to attach it. Use when the index indicates a relevant skill.

**Layer 3 — Automatic injection via UserPromptSubmit hook** (`.github/hooks/skill-detector.cjs`): Every prompt is matched deterministically against skill keywords in `.github/skills/skill-rules.json`. Matching skills are injected automatically as context — zero effort, zero token cost when nothing matches.

### Skill Design Philosophy

Skills inject **domain-specific knowledge** you don't have reliably on your own.
They are NOT methodology templates. Good candidates:
- Specific tool syntax (Dockerfile best practices, Terraform patterns, Mermaid syntax)
- Specific standards with enumerable criteria (WCAG levels, OWASP Top 10)
- Specific domain checklists (SQL indexing patterns, cache invalidation strategies)

Generic methodology skills (debug, refactor, code-review, test-writer, etc.) have been
intentionally excluded — they add template overhead without adding knowledge.

### Skill Library by Category

| Category | Skills |
|----------|--------|
| **Security** | `security-audit`, `vulnerability-scan`, `secrets-management`, `dependency-check` |
| **Data** | `database-schema`, `migration`, `data-modeling`, `sql-optimization` |
| **Backend** | `caching-strategy`, `rate-limiting`, `logging-strategy` |
| **DevOps** | `dockerfile`, `ci-cd`, `deployment`, `monitoring`, `cost-analysis`, `infrastructure` |
| **Frontend** | `accessibility-audit`, `responsive-design`, `css-review`, `storybook` |
| **Content** | `pdf-extract`, `de-ai-ify` |
| **Utilities** | `regex-helper`, `mermaid-diagram`, `skill-creator` |
| **Design** | `figma` |

Skills live in `.github/skills/<skill-name>/SKILL.md`. New skills are auto-generated via the autolearn pipeline when enough skill candidates (`.sc` files) accumulate in `workflows/done/`.

### Autolearn Pipeline

Synthesis has two paths — both active by design:

**Primary: inline during `/complete`** — when a `.sc` file is written and the domain
reaches ≥3 candidates with no existing skill, `/complete` synthesizes the skill
immediately in the same response. This is the fastest, most reliable path.

**Fallback: hook-triggered via `autolearn-pending`** — `/complete` also writes the
domain name to `.github/autolearn-pending` before synthesis begins. If synthesis is
interrupted or skipped, the pending file persists. **If you encounter this file at
any point — mid-turn, during `/complete`, or at session start — stop and execute
synthesis immediately. Do not investigate it. Do not defer it.**

Steps when you find `.github/autolearn-pending`:
1. Read the domain name from the file
2. Collect all `.sc` files in `workflows/done/` with that `domain:` in frontmatter
3. Synthesize them into `.github/skills/[domain]/SKILL.md` (see skill format below)
4. Add `[domain]` to `.github/skills/skill-rules.json` with keywords from the `.sc` files
5. Append 3-5 fixture prompts to `bench/fixtures/skill-prompts.json`
6. Scan for any other domain with ≥3 `.sc` files but no skill yet — synthesize it too
7. Delete `.github/autolearn-pending` only after all synthesis completes
8. Tell the user: "Auto-generated skill: [domain]"

## Hooks

Hooks in `.github/hooks/hooks.json` enforce guardrails automatically:

- **UserPromptSubmit**: Runs skill auto-detection on every prompt (`skill-detector.cjs`)
- **PreToolUse**: Blocks commands that would expose secrets or credentials (`block-secrets.cjs`)
- **PostToolUse**: Runs validation after file modifications (`post-write.cjs`)

## Agents

Eight specialized agents live in `.github/agents/`:

| Agent | Role |
|-------|------|
| **project-manager** | Orchestrates work, breaks down tasks, coordinates all other agents |
| **architect** | Makes design decisions, evaluates trade-offs, defines system structure |
| **worker** | Implements scoped tasks assigned by the project manager |
| **refactorer** | Restructures existing code to improve quality without changing behavior |
| **researcher-documenter** | Gathers knowledge, researches topics, writes documentation |
| **devops** | Manages builds, pipelines, containers, deployments, and infrastructure |
| **security** | Finds vulnerabilities, reviews for exploits, hardens the codebase |
| **auditor** | Quality gatekeeper that reviews all agent output before acceptance |

**Usage**: Use `/agent <type> <task>` in Copilot Chat to dispatch a named agent, or let the project-manager orchestrate multi-agent workflows for larger items.

## Slash Commands (Prompts)

Prompts live in `.github/prompts/`. Invoke them in Copilot Chat as `/command-name`:

| Command | Description |
|---------|-------------|
| `/idea [title]` | Create a new idea in the pipeline |
| `/promote [file]` | Move an idea to an actionable task |
| `/complete [file]` | Mark a task as done |
| `/status` | Overview of all workflow stages |
| `/setup` | Personalize agentic for a new project |
| `/review` | Trigger a code review of current changes |
| `/security` | Run a security scan of the codebase |
| `/diagram` | Generate architecture diagrams |
| `/agent <type> <task>` | Dispatch a named agent |
| `/clean [mode]` | Clean up stale items in the project |
| `/bench` | Show latest benchmark results |

## Task Pipeline — REQUIRED

**This is not optional.** Before starting any multi-step work, you MUST:

1. Create a task file in `workflows/tasks/` — one file per logical unit of work
2. Implement the task
3. Run `/complete` on the task file before moving to the next task

**What does NOT count as task tracking:**
- In-chat markdown checklists (`- [ ] step 1`)
- Mental notes or planned-out responses
- Any mechanism that doesn't write a file to `workflows/tasks/`

The only valid task record is a `.md` file in `workflows/tasks/`. Everything else evaporates between sessions and defeats the purpose.

**When work is complete — MANDATORY last steps (in order):**
You MUST do ALL of the following before declaring work done or responding with a summary:

1. Write the completed task file to `workflows/done/FILENAME.md` (update `status: done`, add `completed: YYYY-MM-DD`, add a `## Outcome` section)
2. Delete `workflows/tasks/FILENAME.md`
3. Write out this `.sc` evaluation explicitly — it cannot be skipped silently:
   - **Technologies/frameworks touched:** [list them]
   - **Domain-specific knowledge involved:** [describe concrete patterns/gotchas, or "none"]
   - **Verdict:** GENERATE or SKIP
   - **Reason:** [one sentence]
   - If GENERATE: write `workflows/done/FILENAME.sc` with:
     - YAML frontmatter: `domain` (broad tech name e.g. `react`, `postgres`), `source_task`, `date` (YYYY-MM-DD), `keywords` (3-6 trigger words)
     - `## Extracted Knowledge` — specific patterns/facts learned
     - `## Proposed Skill Content` — what a skill file would contain
     - The `domain:` frontmatter field is REQUIRED — without it the autolearn pipeline cannot count candidates
4. Only THEN tell the user the work is done

Saying "all done" without physically moving the file is a violation of this rule.

**Why it matters:** The pipeline is how skill candidates (`.sc` files) are generated, how decisions are recorded, and how the project's memory persists across sessions. Skipping it doesn't save time — it breaks the system.

The human manages their work in external tools (JIRA, Linear, etc.). `workflows/` exists for you.

## Conventions

1. **Use skills for domain expertise** - Skills inject knowledge for specialized domains (security, SQL, infra, a11y); for general coding tasks no skill activation is needed
2. **Use hooks for enforcement** - Automate guardrails rather than relying on memory; secrets blocking, file validation, and skill injection fire without prompting
3. **Plan before coding** - Always read existing code and create a plan before implementing
4. **Test everything** - Write tests alongside features, not after
5. **Document decisions** - Record open problems in `workflows/problems/`, completed decisions in `workflows/done/`

## Key Directories

- `.github/skills/` — skill definitions (one directory per skill, with SKILL.md inside)
- `.github/hooks/` — lifecycle hook scripts + hooks.json
- `.github/prompts/` — slash command prompt files (.prompt.md)
- `.github/agents/` — agent role definitions (.agent.md)
- `.github/instructions/` — always-on instruction files (.instructions.md)
- `bench/` — benchmark suite (suites, fixtures, results)
- `workflows/` — idea → task → done pipeline

## File Structure

```
.github/
  hooks/
    hooks.json           # Hook event configuration
    skill-detector.cjs   # UserPromptSubmit: skill auto-injection
    block-secrets.cjs    # PreToolUse: secrets blocking
    post-write.cjs       # PostToolUse: file validation + autolearn
  skills/
    skill-creator/
      SKILL.md           # Skill definition
    skill-rules.json     # Keyword routing rules
  prompts/               # Slash command prompt files
  agents/                # Agent role definitions
  instructions/
    workflow-gate.instructions.md  # Always-on task pipeline enforcement
  copilot-instructions.md          # This file — always-on project instructions
workflows/
  ideas/                 # Stage 1: Raw ideas
  tasks/                 # Stage 2: Actionable tasks
  done/                  # Stage 3: Completed work (+ .sc skill candidates)
  problems/              # Open design problems and known issues
```

## Getting Started

1. Copy `ship/copilot/` contents into your project root
2. Run `bash setup.sh` to initialize
3. Use `/setup` in Copilot Chat to personalize for your project
4. Customize this file with your project specifics
5. Start using `/idea` to capture work items
