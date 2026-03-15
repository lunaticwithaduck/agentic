Show an overview of the entire workflow pipeline.

## Instructions

1. Scan all three workflow directories:
   - `workflows/ideas/` - list all `.md` files
   - `workflows/tasks/` - list all `.md` files
   - `workflows/done/` - list all `.md` files
2. For each file, read the frontmatter to extract: title, created, priority, status
3. Display a formatted status report:

```
## Workflow Status  (agentic v[version from VERSION file, or "unknown" if not found])

### Ideas ([count] items)
| Priority | Title | Created |
|----------|-------|---------|
| [priority] | [title] | [date] |

### Tasks In Progress ([count] items)
| Priority | Title | Promoted | Complexity |
|----------|-------|----------|------------|
| [priority] | [title] | [date] | [complexity] |

### Recently Completed ([count] items)
| Title | Completed | Days to Complete |
|-------|-----------|------------------|
| [title] | [date] | [days from created to completed] |

### Skill Health
- Total skills: [count from .github/skills/skill-rules.json]
- Active (fired in last 90 days): [count]
- Stale (no fires in 90+ days): [count]
- Never fired: [count]
- (Read `.github/skill-usage.json` and compare against `.github/skills/skill-rules.json`)
- (If skill-usage.json doesn't exist, show "No usage data yet — skills will be tracked as they fire")

### Summary
- Ideas waiting: [count]
- Tasks in progress: [count]
- Completed (all time): [count]
- Skill health: [active]/[total] active
```

4. If all directories are empty, say: "The pipeline is empty. Use `/idea [title]` to get started."
5. If there are ideas older than 30 days, note: "There are [N] ideas older than 30 days. Consider promoting or archiving them."
6. If there are tasks with no activity for 14+ days, note: "There are [N] stale tasks. Consider reviewing them."
7. If there are stale or never-fired skills, note: "There are [N] stale/unused skills. Run `/clean` for details."
