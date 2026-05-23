---
title: Research — is adding Base profitable as a 6th liquidator chain?
created: 2026-05-22
completed: 2026-05-22
status: done
---

## Recommendation: **NO — do not add Base as a liquidator lane.**

The TVL numbers are massive but the realized liquidation opportunity is tiny and dominated by faster competitors. Our HTTP-polling + 1-3s detection cadence would lose every race that matters.

## Evidence

### TVL by lending protocol on Base
| Protocol | Base TVL | Architecture | Our existing template |
|----------|----------|--------------|------------------------|
| Morpho Blue | **$2.86B** | Morpho Blue | Felix code |
| Aave V3 | $415M | Aave V3 | HyperLend code |
| Compound V3 | $26M | Compound V3 | (no template) |
| SparkLend | $26M | Aave V3 fork | HyperLend code |

### Actual liquidation flow (Morpho Blue Base, last 48h via Morpho's GraphQL)
| Date | Market | Seized USD |
|------|--------|-----------|
| 2026-05-22 09:30 | WMTX/USDC | $0 |
| 2026-05-21 08:53 | **LsETH/WETH** | **$1,446** ← only real one |
| 2026-05-21 06:17 | WETH/USDC | $10 |
| 2026-05-21 05:48 | WETH/USDC | $10 |
| 2026-05-21 04:44 | WETH/USDC | $10 |
| (5 more @ $10) | | |

**Realized opportunity (48h): ~$1,500 in seized collateral across 10 liquidations.** At Morpho's ~3-7% LIF that's $50-100 of total profit for ALL liquidators combined on Base. Distributed across ~10+ competing bots → ~$5-10/bot. The pattern of $10 dust fires suggests automated bots are accepting tiny fees just to maintain pole position — they'd dominate any meaningful fire too.

### Current at-risk inventory
GraphQL query for positions on Morpho Base with HF≤1.10 and any borrow: returns 10 positions, total debt **<$300**. Compare to our existing Felix lane on HyperEVM where we have 13 positions at HF<1.10 with **>$1.1M total debt**. The opportunity *density* on Base is dramatically worse than HyperEVM despite 8× the TVL.

### Why TVL ≠ opportunity on Base
1. **Big curated Morpho markets (cbBTC/USDC, WETH/USDC)** are dominated by Coinbase / institutional borrowers who actively manage positions. They rarely cross HF<1.
2. **MEV competition is mature**: Wintermute, Flashbots searcher network, jaredfromsubway-tier bots run on Base. Sub-100ms detection via private mempool.
3. **Block time 2s + mainstream Flashbots Protect**: any HTTP-polling approach (1-3s latency) loses the race.
4. **The remaining opportunity** is the "dust + tail markets" segment, which our existing HyperEVM-derived code wouldn't have a comparative advantage in.

### Comparison to our existing chains
Our edge has been **going where competition is thin**:
- Felix (HyperEVM): vanilla Morpho markets on a newer L1
- Bend (Berachain): Morpho fork on a newer L1
- HyperLend/HypurrFi (HyperEVM): Aave forks on a newer L1
- Sonic (Silo V2): new chain, new protocol design

All 5 lanes share that thesis. Base breaks it.

### Engineering + capital cost (would have been low)
- 1-2 days to port Felix → Base Morpho lane (same code, change RPC + Morpho address)
- 1-2 days to port HyperLend → Base Aave V3 lane
- Capital: ~$200 ETH bridged for gas treasury
- Op cost: ~$3-5/mo additional RPC overhead

**Low cost, but the expected return doesn't justify it.** If we're realistically going to capture $0-30/month on Base while competing with sub-100ms shops, the engineering hours are better spent elsewhere.

## Better expansion candidates (for future research)

| Chain | Lending protocol | Why interesting |
|-------|------------------|----------------|
| **Plume Mainnet** | Morpho Blue (per DefiLlama, $XX TVL) | New, RWA-focused, niche |
| **Unichain** | Morpho Blue | New L2, less competition |
| **Tempo** | Morpho Blue | Listed by DefiLlama, newer |
| **Monad** | (TBD) | Soon-to-launch, ground floor |
| **TAC** | (TBD) | New chain |
| **Katana** | (TBD) | New chain |

The thesis we should chase: **be early to chains where Morpho/Aave deployments exist but competition is still building**. Same edge as HyperEVM gave us.

## Outcome
Completed 2026-05-22. Recommendation: skip Base. Better to keep our 5 lanes well-tuned and earlier-stage chains on the radar for the next expansion. If a really juicy Base opportunity appears (e.g., a new protocol launches with weak liquidator coverage), revisit.

## Completion
Run `/complete workflows/tasks/2026-05-22-research-base-expansion.md`.
