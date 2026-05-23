---
title: Build discover-pools.js — weekly DEX pool survey across all liquidator chains
created: 2026-05-22
completed: 2026-05-22
status: done
---

## Goal
One-shot survey tool that uses CoinGecko's on-chain endpoints to list every DEX pool for every token referenced in our positions.json across Sonic, HyperEVM, and Berachain. Compares each token's deepest pool against the DEX our executor actually routes through; flags structural routing errors.

## Built
`/home/jojo/automation/lib/discover-pools.js` (CLI). Reads each lane's positions.json, queries CoinGecko `/onchain/networks/{slug}/tokens/{addr}/pools`, ranks by TVL, and writes a markdown report with ⚠️ flags where our DEX is <50% of the deepest pool or absent.

Usage:
```
node discover-pools.js                    # all chains, top 5 pools
node discover-pools.js --chain=hyperevm   # one chain
node discover-pools.js --out=path.md      # write to file
```

Manual .env parsing (no dotenv dep — lib/ has no node_modules); 700ms inter-call spacing to respect the 100/min Demo limit.

## Findings from first run (2026-05-22)

Saved to `bench/results/dex-survey-2026-05-22.md` (675 lines, 37 KB).

**🚨 Major finding: HyperEVM routes through the wrong DEX for nearly every important token.**

Our 3 HyperEVM lanes (Felix, HyperLend, HypurrFi) all route exclusively through `hyperswap`. The survey found that **`project-x`** has 5-50× the liquidity on every active token:

| Token | Deepest DEX (TVL) | Our HyperSwap TVL | Ratio |
|-------|-------------------|-------------------|-------|
| USDC | project-x ($11.5M) | not present | 0% |
| USDT0 | project-x (#1) | (not in top 5) | <small slice> |
| WHYPE | project-x ($11.5M) | not present | 0% |
| UBTC | project-x ($6.6M) | $467k | 7% |
| kHYPE | project-x ($4.7M) | $296k | 6% |
| UETH | project-x ($2.97M) | $254k | 9% |
| wstHYPE | nest ($714k) | $13k | 2% |
| USDH | project-x ($343k) | not present | 0% |

The Felix whale `0x24df4b7af6` ($286k debt) lives in a stable-stable market — when it crosses HF<1.0, the collateral→USDT0 swap will route through HyperSwap pools that are tiny fractions of the project-x pools, eating massive slippage or failing the profit gate.

**Sonic**: routing is mostly correct for active markets (USDC.e, wS, stS, beS, scUSD all dominated by shadow-exchange). The 12 warnings are on minor tokens we don't actively liquidate.

**Berachain**: kodiak is dominant for all HONEY-paired markets. Clean.

## Steps taken
- [x] Confirmed CoinGecko network slugs: `sonic`, `hyperevm`, `berachain`
- [x] Wrote `discover-pools.js` with chain config, token extraction from positions.json, rate-limit pacing
- [x] Manual .env parsing (no dotenv dep)
- [x] Ran full survey — 48+34+18 = ~100 tokens × 2 calls ≈ 200 CoinGecko calls (~2% of monthly budget)
- [x] Surfaced critical project-x finding on HyperEVM

## Outcome
Completed 2026-05-22. Tool is reusable for weekly audits. The first run already paid for itself by revealing that all 3 HyperEVM lanes need a project-x integration to route trades efficiently. Filed as separate follow-up task.

## Out-of-scope follow-up
- Add `project-x` (and `nest`, `curve-hyperevm`, `balancer-v3-hyperevm` as secondary fallbacks) to the HyperEVM swap-path. This is potentially worth $thousands per missed fire.
- Build a `--diff` mode that compares this survey to last week's to alert on liquidity migrations.

## Completion
Run `/complete workflows/tasks/2026-05-22-discover-pools-survey-tool.md`.
