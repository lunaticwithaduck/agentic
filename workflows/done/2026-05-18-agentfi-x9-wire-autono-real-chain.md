---
title: AgentFi X9 — Wire AUTONO snapshot to real Base chain data
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Goal
Replace AUTONO's mock snapshot with real Base reads via Etherscan v2 multichain + GeckoTerminal.

## Steps
- [x] `lib/chain/etherscan.ts` — typed v2 client (chainid=8453), exponential backoff on 429/5xx, `hasKey()` gate
- [x] `lib/chain/geckoterminal.ts` — public price API wrapper (no key needed)
- [x] `lib/chain/autono.ts` — `getAutonoSnapshotLive()` composes price + supply + DIEM balance + recent token transfers → `AgentSnapshot`. Returns null when no key.
- [x] `lib/mock-data.ts` patched: `getSnapshot`/`listSnapshots` now async; 5-min in-memory cache on the live AUTONO snapshot; mock fallback when no key OR live fetch fails
- [x] All consumers updated to await: `app/page.tsx`, `app/agent/[slug]/page.tsx`, `app/admin/dry-run/page.tsx`, `app/og/{agent/[slug],comp,action/[txhash]}/route.tsx`, `components/CompTable.tsx` (now async server component), `scripts/dry-run-poster.ts`
- [x] `AgentCard` extended with optional `nowMs` prop so OG renders use pinned `MOCK_NOW_ISO` for byte-stable snapshots; live page defaults to `Date.now()`
- [x] `.env.local` created (gitignored) with placeholder + comment pointing to https://etherscan.io/myapikey
- [x] `.env.example` committed
- [x] 10 new vitest tests across `chain-etherscan.test.ts` (6) and `chain-autono.test.ts` (4): URL params, backoff, no-key fallback, end-to-end snapshot composition, tokentx failure resilience
- [x] OG snapshot baseline regenerated (`pnpm exec playwright test snapshots.spec.ts --update-snapshots`)

## Verification
- `pnpm test`: **87/87** vitest (was 77 — +10 chain tests)
- `pnpm exec playwright test`: **17/17** in 9.7s
- `pnpm build`: clean — 14 routes, AUTONO snapshot now async-resolved

## Architecture notes
- **Etherscan v2 multichain** — one key works for Base (chainid=8453), Ethereum, Optimism, Arbitrum, etc. Free tier: 5 req/sec, 100k req/day. Plenty for hourly indexer.
- **5-min in-memory cache** on autono live snapshot prevents per-render API hits. Real production should swap to Next's `unstable_cache` or revalidate config.
- **AgentCard `nowMs` prop** — clean way to make OG renders byte-stable without breaking live-page freshness. Live page omits the prop → defaults to `Date.now()`. OG routes pass `MOCK_NOW_ISO`.
- **Type-only circular import** (chain/autono ↔ mock-data) — fine because both sides use `import type` so it's stripped at compile time.

## Outcome
Mechanics done. Three things to land before AUTONO is fully real on screen:

1. **Paste real key into `.env.local`** — user action. Until then, mock returns silently. The whole rest of the system works.
2. **Build-mode rate calc** — currently returns `rate: 0` from live path (needs trailing-7d snapshot delta from indexer history). Falls back to `eta unknown`. Real fix needs a Postgres snapshots table.
3. **7d delta + sparkline** — live path returns 0% delta and a flat sparkline. Same fix as #2 (needs history).

So AUTONO mcap/compute_val will be live once key is set, but the build-mode bar + sparkline will still look mock-shaped until the indexer history task lands. Worth a follow-up task `agentfi-x10-snapshot-history`.

## ETHY / BANKR / AETHER
Stay pure mock. Each needs its own strategy: ETHY/BANKR fee-revenue from Uniswap V4 subgraph, AETHER treasury-balance read. Multi-agent indexer is a separate larger task.

## .sc — Skill candidate evaluation

**Skill candidate evaluation:**
- Technologies touched: Etherscan v2 multichain API, GeckoTerminal public API, Next.js async server components throughout consumer chain, vitest fetch mocking
- Domain knowledge: Etherscan v2 single-key-all-chains via `?chainid=`; GeckoTerminal `/networks/<chain>/tokens/<addr>` shape; pattern for "live or mock" snapshot resolution with module-scope cache; async-server-component cascade for replacing sync data sources
- Verdict: **GENERATE**
- Reason: Etherscan v2 multichain (2024 release) is non-obvious — many devs still write per-chain code with separate Basescan/Arbiscan keys. Worth a `defi-data` domain entry that future on-chain reads (DexScreener, DefiLlama, Coingecko Pro) can join.
- Domain: `defi-data` (new — 1st entry, below 3-file synthesis threshold)
