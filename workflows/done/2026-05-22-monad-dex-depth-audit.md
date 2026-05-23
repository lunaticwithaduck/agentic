---
title: Use CoinGecko on-chain API to find DEXes with real depth for Monad at-risk markets
created: 2026-05-22
completed: 2026-05-22
status: done
---

## Findings (probed via CoinGecko /onchain/networks/monad/tokens/{addr}/pools)

| Token | Best pool | DEX | TVL |
|-------|-----------|-----|-----|
| wstETH (whale cluster collateral) | wstETH/WETH 0.01% | **uniswap-v4-monad** | **$535,912** |
| WETH (whale cluster loan) | MON/WETH 0.05% + USDC/WETH | uniswap-v4-monad | $4.6M aggregate |
| earnAUSD ($2.79M coll) | AUSD/USDC 0.005% | uniswap-v4-monad | $3.88M |
| AUSD (common loan) | MON/AUSD + AUSD/USDC | uni v4 + traderjoe-v2.2 | $13.4M aggregate |
| wsrUSD ($2.5M coll) | — | none indexed | — |
| USD1 (loan) | — | none indexed | — |
| syzUSD, earnAUSD-other, YZM | — | none indexed | — |

## DEX coverage ranking (sum of TVL across audited tokens)
| DEX | Coverage |
|-----|----------|
| **uniswap-v4-monad** | **$20.5M** ← integration target |
| traderjoe-v2-2-monad | $3.07M |
| curve-monad | $3.06M (but wstETH/WETH/weETH pool is only $459 — empty) |
| pancakeswap-v3-monad | $610k |

## Decision update vs prior plans
- **DEPRIORITIZE Curve integration** — wstETH/WETH/weETH Curve pool has $459 TVL; not the path
- **PRIORITIZE Uniswap V4 via UniversalRouter** — single integration unlocks $20.5M of routable depth
- **ACCEPT 3 markets as uncapturable**: wsrUSD/USD1, syzUSD/USDC, YZM/USDC have NO secondary DEX market. Monitor should suppress these from the arm pipeline (they'll log but never fire).

## Implementation hint (next session)
UniversalRouter on Monad: `0x0d97dc33264bfc1c226207428a79b26757fb9dc3` (per Uniswap docs). Encodes commands as a `bytes` array — different from V3's `exactInputSingle`. Reference implementation: Uniswap's `permit2-relay` examples or the SDK at `@uniswap/universal-router-sdk`.

## Outcome
Completed 2026-05-22. Survey re-routed the next integration from Curve → Uniswap V4. The CoinGecko on-chain API was the key signal — `getPool() != 0x0` on Uni V3 was misleading; depth comparison across DEXes is the truth.

## Completion
Run `/complete workflows/tasks/2026-05-22-monad-dex-depth-audit.md`.
