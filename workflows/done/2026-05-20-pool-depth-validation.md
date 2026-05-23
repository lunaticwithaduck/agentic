---
title: Validate HyperSwap pool depth-at-range vs oracle prediction
created: 2026-05-20
completed: 2026-05-20
status: done
---

## Goal
Our executor's `amountOutMinimum` uses AaveOracle prices. The actual HyperSwap pool delivers based on liquidity-at-tick-range. A pool with huge total `liquidity()` but concentrated far from spot will under-deliver, causing `NoProfit` reverts.

## Steps
- [x] Built `/home/jojo/automation/lib/pool-depth-check.js` — read-only analyzer that simulates expected swap per at-risk position and reports impact as % of pool reserves
- [x] Ran against current at-risk positions on both chains
- [x] Discovered TWO HyperLend whales whose swaps would be impossible/catastrophic
- [x] Surfaced a previously-unknown bug: **same-asset case** (coll == debt) when user has both supply and debt in same token
- [x] Added depth check + same-asset handling in both executors
- [x] Lint + restart

## Findings (impact = swap size / pool reserve)

| Chain | User | Pair | Impact | Verdict |
|---|---|---|---:|---|
| HL | `0x085b9899` | USDC→WHYPE | 0.32% | ✅ fine |
| HL | `0x095c9387` | kHYPE→WHYPE | **157.0%** | 🚨 would have reverted — now skipped |
| HL | `0xba325093` | WHYPE→WHYPE | — | same-asset — no-swap path now |
| HL | `0xf0e4438e` | kHYPE→WHYPE | **26.5%** | 🚨 huge slippage — now skipped |
| HL | `0x9f25248e` | WHYPE→UETH | 1.9% | ⚠️ borderline (under 5% threshold) |
| HP | `0xb5c46131` | WHYPE→USDC | **8.5%** | 🚨 the whale — now skipped |
| HP | `0xb450d2c5` | wstHYPE→wstHYPE | — | same-asset — no-swap path now |
| HP | `0x94391300` | USD₮0→WHYPE | 0.007% | ✅ fine |

## Hardening applied
**1. Pool depth check at presign:** for cross-asset swaps, query pool's `balanceOf(tokenIn)` and compute impact. If > 5% of reserve, skip with `🚨 swap would consume X% of pool reserves` log — no broadcast, no Telegram noise.

**2. Same-asset handling:** if `collAsset.underlying === debtAsset.underlying`, set `swapTarget = 0` and `swapData = '0x'`. Aave's `liquidationCall` natively handles same-asset liquidation (flash loan debt asset, liquidate, keep LIF bonus in same asset). No swap needed; previously would have built a self-swap that reverts.

## Outcome
Completed 2026-05-20. Real-world consequence: the HyperLend whale `0x095c9387` ($407k debt) would have been a **guaranteed failure** if HF dropped below 1.0 — our bot would have presigned + pre-flight-reverted on every poll cycle, spamming Telegram with each. Now it's silently skipped at presign with a clean log line.

Pool depth check is also a useful diagnostic — can be re-run anytime via `NODE_PATH=/home/jojo/automation/mibera/sweeper/node_modules node lib/pool-depth-check.js` to see if any new positions have entered the danger zone.

## Completion
Run `/complete workflows/tasks/2026-05-20-pool-depth-validation.md`.
