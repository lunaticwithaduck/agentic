---
title: AgentFi X36 — Restore GitHub repo links across the product
created: 2026-05-21
status: done
completed: 2026-05-21
---

## Goal
Restore github.com/vpjonny/agentfi-terminal links in Footer + methodology footer that X28 stripped as placeholders pre-push.

## Files
- `components/Footer.tsx` — github link between `api` and `privacy`
- `app/methodology/page.tsx` — "Source code: github.com/vpjonny/agentfi-terminal ↗" in the doc footer
- `tests/smoke.spec.ts` — footer-link test asserts the github href

## Outcome

Completed on 2026-05-21. Two restorations + one test update. Links open in new tab with `noreferrer noopener`. Visible from every page via Footer; explicit "Source code:" attribution on `/methodology` per protocol-docs convention.

## Completion
Run `/complete workflows/tasks/2026-05-21-agentfi-x36-restore-github-links.md`.
