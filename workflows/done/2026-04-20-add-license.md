---
title: Add PolyForm Noncommercial license and README license section
created: 2026-04-20
completed: 2026-04-20
status: done
---

## Goal
Add the PolyForm Noncommercial 1.0.0 license to the repository and document licensing in README.

## Steps
- [x] Run existing validation commands before modifications
- [x] Add LICENSE file using PolyForm Noncommercial 1.0.0 with provided year and name
- [x] Add README License section with requested text
- [x] Run validation commands after modifications
- [x] Move this task file to workflows/done with outcome notes

## Outcome
Added `/home/runner/work/agentic/agentic/LICENSE` with the PolyForm Noncommercial License 1.0.0 text and included:

`Required Notice: Copyright 2026 Lyubomir Pacheliev`

Updated `/home/runner/work/agentic/agentic/README.md` by appending a new `## License` section with the requested non-commercial usage statement and license link.

Validated before and after the change with:
- `node --check .claude/hooks/*.cjs`
- `python3 -m json.tool .claude/skills/skill-rules.json`
- `bash .claude/scripts/validate.sh`

## .sc Evaluation
Domain: none — licensing/documentation update
Applied domain knowledge: no
Negative signal: none
Generate .sc: no
