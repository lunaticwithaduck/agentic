Quick dispatch of a named subagent.

## Instructions

1. Parse $ARGUMENTS to extract the agent type and task description
   - Format: `/agent [type] [task description]`
   - The first word is the agent type, the rest is the task description

2. Valid agent types and their definitions:
   - `pm` → `.claude/agents/project-manager.md`
   - `architect` → `.claude/agents/architect.md`
   - `worker` → `.claude/agents/worker.md`
   - `refactorer` → `.claude/agents/refactorer.md`
   - `researcher` → `.claude/agents/researcher-documenter.md`
   - `devops` → `.claude/agents/devops.md`
   - `security` → `.claude/agents/security.md`
   - `auditor` → `.claude/agents/auditor.md`

3. If no agent type is provided, list all available agents with a one-line description:
   - **pm** — Project manager: plans, prioritizes, and tracks work
   - **architect** — Architect: designs systems and makes technical decisions
   - **worker** — Worker: implements features and fixes bugs
   - **refactorer** — Refactorer: improves code quality and structure
   - **researcher** — Researcher: investigates topics and documents findings
   - **devops** — DevOps: manages infrastructure, CI/CD, and deployments
   - **security** — Security: audits code for vulnerabilities and threats
   - **auditor** — Auditor: reviews code quality, compliance, and standards

4. If no task description is provided (only the type), ask what the agent should do

5. Read the corresponding agent definition from `.claude/agents/`
6. Spawn a Task subagent with the agent definition as context plus the task description
7. Return the subagent's output to the user
