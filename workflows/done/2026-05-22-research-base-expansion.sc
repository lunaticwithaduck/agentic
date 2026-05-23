---
domain: defi-liquidations
source_task: 2026-05-22-research-base-expansion.md
date: 2026-05-22
keywords: ["chain-expansion", "tvl-vs-opportunity", "competition", "morpho-graphql", "defillama", "research", "decision-framework"]
---

## Extracted Knowledge

### TVL is NOT a proxy for liquidator opportunity
A $2.86B TVL lending protocol can produce $50-100/day of total realized seize value across all liquidators. The funnel is:
```
TVL → Active borrows → Borrows near HF=1 → Crossings → Captured by us
```
Each step has heavy attrition. On established chains (Base, Arbitrum, Optimism), institutional borrowers actively manage positions and bots cluster around the small fraction that crosses. Distributed across 10+ competing bots, our slice approaches zero.

**Better proxies than TVL:**
- Count of positions currently at HF<1.10 (the "near-cliff" inventory)
- Recent liquidation count + total seized USD over 7-30 days
- Number of competing bots visible in liquidation `tx.from` addresses (high diversity = lots of competition)
- Block time + private mempool availability (sub-100ms = we lose)

### "Go where competition is thin" thesis
Our 5 lanes all share a thesis: new chains where lending protocols exist but liquidator coverage is still building. HyperEVM (Felix, HyperLend, HypurrFi), Berachain (Bend), Sonic (Silo V2). The edge isn't speed — it's *presence* in markets MEV firms haven't bothered with yet.

Once a chain matures (Base did years ago), the edge evaporates and HTTP-polling architectures get outpaced by sub-100ms shops on private mempool.

**Future expansion candidates fit the same template**: Plume, Unichain, Tempo, Monad, Katana, TAC, etc. — chains where Morpho or Aave has deployed but the liquidator scene is sparse. The DefiLlama protocol API exposes `chains[]` so you can pick newly-listed deployments.

### Morpho Blue GraphQL API for chain-evaluation
```graphql
# Top markets by TVL on a target chain
{
  markets(first:8, where:{chainId_in:[CHAIN_ID]}, orderBy:SupplyAssetsUsd, orderDirection:Desc) {
    items {
      uniqueKey
      collateralAsset { symbol }
      loanAsset { symbol }
      state { supplyAssetsUsd borrowAssetsUsd }
      lltv
    }
  }
}

# Currently at-risk positions
{
  marketPositions(first:20, where:{chainId_in:[CHAIN_ID], healthFactor_lte:1.1, borrowShares_gte:1}, orderBy:HealthFactor, orderDirection:Asc) {
    items {
      user { address }
      market { collateralAsset { symbol } loanAsset { symbol } }
      healthFactor
      state { borrowAssetsUsd }
    }
  }
}

# Recent liquidations (revenue floor signal)
{
  transactions(first:20, where:{chainId_in:[CHAIN_ID], type_in:[MarketLiquidation]}, orderBy:Timestamp, orderDirection:Desc) {
    items {
      hash
      timestamp
      data {
        ... on MarketLiquidationTransactionData {
          seizedAssetsUsd
          repaidAssetsUsd
          market { collateralAsset{symbol} loanAsset{symbol} }
        }
      }
    }
  }
}
```
Endpoint: `https://blue-api.morpho.org/graphql` — no auth needed. Returns NULL for chains where Morpho isn't deployed.

### DefiLlama protocol API for protocol-on-chain TVL
```
GET https://api.llama.fi/protocol/{slug}
```
Returns `chains[]`, `chainTvls.{Chain}.tvl[]` history, etc. Useful for confirming Morpho/Aave presence per chain before bothering to evaluate.

### Decision template for adding a new lane
1. **TVL exists on chain?** (DefiLlama) → if no, skip
2. **At-risk inventory ≥ $50k?** (Morpho GraphQL or Aave subgraph) → if no, low opportunity ceiling
3. **Last 7d liquidation volume ≥ $X total?** → tells you whether bots are active OR positions just sit healthy
4. **Recent liquidator `tx.from` diversity** → many different addresses = high competition, few = thin
5. **Block time < 1s + private mempool dominant?** → we lose the race, skip
6. **Code reuse from existing lane?** → 1-2 day port if yes, 1-2 weeks if no

If steps 1-5 pass and 6 says reuse, then yes. Otherwise no, regardless of TVL.

## Proposed Skill Content

Extend `defi-liquidations` with a "Chain expansion decision framework" section:

- **TVL ≠ opportunity.** Always check at-risk inventory + recent liquidation volume + competitor diversity before assuming a high-TVL chain is worth entering.
- **Our architectural edge is presence, not speed.** Go to new chains early, not mature L2s.
- **The Morpho GraphQL API** (`blue-api.morpho.org/graphql`) is the fastest way to estimate Morpho opportunity on any chain — markets, positions, recent liquidations all in one place.
- **Decision template**: TVL → at-risk inventory → recent liquidations → competitor diversity → block time → code reuse cost.
- **Reject Base unless circumstances change.** Mature L2 with mature MEV competition; HTTP-polling architectures don't compete profitably.
