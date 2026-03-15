Invoke a named agent role for a specific task.

## Instructions

In GitHub Copilot, agents are not dispatched as separate subprocesses. Instead, you adopt the
requested agent role directly and apply its expertise to the task at hand.

Agent role definitions live in `.github/agents/`.

### Available Agent Roles

1. Parse $ARGUMENTS to extract the agent type and task description
   - Format: `/agent [type] [task description]`
   - The first word is the agent type, the rest is the task description

2. Valid agent types and their roles:
   - `pm` → `.github/agents/project-manager.agent.md`
   - `architect` → `.github/agents/architect.agent.md`
   - `worker` → `.github/agents/worker.agent.md`
   - `refactorer` → `.github/agents/refactorer.agent.md`
   - `researcher` → `.github/agents/researcher-documenter.agent.md`
   - `devops` → `.github/agents/devops.agent.md`
   - `security` → `.github/agents/security.agent.md`
   - `auditor` → `.github/agents/auditor.agent.md`

3. If no agent type is provided, list the available roles above and ask which one to use

4. If no task description is provided (only the type), ask what the agent should do

5. Read the corresponding agent definition from `.github/agents/` for the full context of how
   that agent operates in this project

6. Adopt the requested agent role: apply that role's perspective, priorities, and expertise
   to the task description provided. Respond as that agent would — with the appropriate
   focus, depth, and decision-making style

7. You are not calling out to a separate process. You are the agent. Respond directly.
