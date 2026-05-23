---
title: AgentFi X37 — README status shields
created: 2026-05-21
status: done
completed: 2026-05-21
---

## Goal
Add shields.io badges to the README: CI status, license, stack versions, test count.

## Files
- `README.md` — badge row above the project intro

## Outcome

Completed on 2026-05-21. Six badges:
- **CI**: live from `actions/workflows/ci.yml/badge.svg` — auto-updates on each run
- **License**: static badge linking to LICENSE (PolyForm NC isn't OSI/SPDX so a static shield is appropriate, color matches the signal palette)
- **Next.js 16 / React 19 / Tailwind v4**: static stack badges with brand colors
- **Tests: 181 unit + 47 e2e**: static badge pointing to README anchor

All link to relevant targets (CI badge → workflow runs, license → LICENSE file, stack → official docs). Signal-color CI/license badges keep brand consistency.

## Completion
Run `/complete workflows/tasks/2026-05-21-agentfi-x37-readme-shields.md`.
