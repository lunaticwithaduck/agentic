---
title: AgentFi X23 — RSS feed of recent agent actions
created: 2026-05-18
status: done
completed: 2026-05-18
---

## Goal
Expose `/feed.xml` (RSS 2.0) of the most recent on-chain actions across all agents. Lets power users and LLMs subscribe. Items include type, agent, detail, txHash, and link back to `/agent/[slug]`.

## Files
- `app/feed.xml/route.ts` — GET handler returning `application/rss+xml`
- `lib/feed.ts` — builder helper that gathers actions across all agents and serializes RSS 2.0
- `lib/__tests__/feed.test.ts` — 4 vitest cases for structure, escaping, RFC-822 dates
- `tests/smoke.spec.ts` — Playwright case asserting RSS shape
- `app/sitemap.ts` — added `/feed.xml` entry

## Steps
- [x] `lib/feed.ts` — gathers `recentActions` from all snapshots, sorts desc by ts, caps to 50, serializes RSS 2.0
- [x] `app/feed.xml/route.ts` — GET handler with `cache-control: public, max-age=300, s-maxage=300`
- [x] XML-escape all detail/title text (handles &, <, >, ", ')
- [x] Tests: vitest 164/164 (added 4), Playwright 26/26 (added 1)
- [x] Added `/feed.xml` to sitemap with hourly changeFreq + 0.3 priority
- [x] Build + test + Playwright green

## Outcome

Completed on 2026-05-18. `/feed.xml` returns a standards-compliant RSS 2.0 feed. Each item:
- `title`: `[TYPE] TICKER · detail` (e.g., `[CLAIM] AUTONO · +5.00 DIEM`)
- `link`: agent detail page
- `guid`: `${base}/action/${txHash}` with `isPermaLink="false"` (not a real URL — just unique)
- `pubDate`: RFC-822 format from action timestamp
- `category`: action type (CLAIM/STAKE/LP/SWAP/LOG/MILESTONE)
- `description`: type + detail + tx hash

Cache-Control headers: 5-min CDN cache. Since the snapshot indexer runs hourly, 5 minutes is generous and avoids re-running the snapshot fetch on every reader poll.

XML escaping covers all five characters the spec requires (`&`, `<`, `>`, `"`, `'`). A vitest case explicitly walks every item title and asserts no unescaped special chars leak through — defensive against future detail strings containing arbitrary token symbols / addresses.

**Skill candidate evaluation:**
- Technologies/frameworks touched: RSS 2.0 spec compliance, RFC-822 date formatting (`toUTCString()`), Next.js Response with custom content-type, sitemap integration
- Domain-specific knowledge: (a) `Date.toUTCString()` produces RFC-822-compliant output natively — no library needed; (b) `guid isPermaLink="false"` is the correct form when the GUID isn't a navigable URL (otherwise readers might try to follow it); (c) RSS escaping must cover all 5 characters (`& < > " '`) — many implementations only escape 3 and break on apostrophes in agent names; (d) `cache-control: s-maxage` is read by CDNs while `max-age` is read by browsers — set both for content that updates on a cron schedule.
- Verdict: SKIP
- Reason: RSS is a stable, well-documented spec from 2009 — the gotchas are well-known. Nothing project-specific worth encoding as a skill.

## Completion
Run `/complete workflows/tasks/2026-05-18-agentfi-x23-rss-feed.md`.
