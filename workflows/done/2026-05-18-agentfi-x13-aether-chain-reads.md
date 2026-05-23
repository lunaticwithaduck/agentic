---
title: AgentFi X13 — AETHER chain reads (treasury-runway strategy)
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Steps
- [x] `lib/chain/constants.ts` — `BASE_TOKENS` (USDC, WETH, DIEM) + `BASE_TOKEN_DECIMALS`. Centralizes Base addressbook.
- [x] `lib/chain/etherscan.ts` — added `getEthBalance(address)` (`module=account&action=balance`)
- [x] `lib/chain/aether.ts` — `getAetherSnapshotLive()`. Parallel reads: AETHER price, WETH price, AETHER supply, USDC balance, WETH balance, ETH balance. Composes treasury = USDC + WETH×price + ETH×price; mcap from Gecko or supply×price; `treasuryRunway` strategy. Returns null when placeholder address yields all zeros + null price (graceful fallback to mock).
- [x] `lib/mock-data.ts` — extracted `resolveSlug(slug)` generic helper, registered `LIVE_FETCHERS` map (autono + aether). `listSnapshots()` resolves all slugs in parallel; ethy/bankr remain mock (no V4 subgraph access).
- [x] 3 vitest tests: no-key returns null, placeholder address returns null, real-shaped data composes correct multiple

## Verification
- `pnpm test`: **120/120** (+3)
- `pnpm exec playwright test`: 17/17
- `pnpm build`: clean

## Outcome
Multi-agent indexer pattern proven generic. AUTONO + AETHER both go live when key is set; ETHY/BANKR cleanly fall back to mock. Adding a new agent to live reads is now: write its `lib/chain/<slug>.ts`, register in `LIVE_FETCHERS`. No mock-data changes needed.

The AETHER address is still PLACEHOLDER in `lib/agents.ts` — the live fetch returns null for the placeholder and the user sees mock data. The moment the real AETHER address ships, no code changes — values just become non-zero.

## What's still placeholder
- ETHY / BANKR: fee-revenue from Uniswap V4 fees. Needs subgraph access; not available locally without API setup.
- AETHER monthly_burn: treasuryRunway returns `runwayDays: null` for now (no burn data source).

## .sc
SKIP — extends X9's defi-data patterns + the multi-agent registry generalization. Both already covered conceptually in the X9 / X10 .sc files.
