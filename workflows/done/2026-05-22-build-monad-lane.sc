---
domain: defi-liquidations
source_task: 2026-05-22-build-monad-lane.md
date: 2026-05-22
keywords: ["morpho-graphql-bootstrap", "monad", "getlogs-cap", "rate-limit", "indexer-alternative", "rpc-tuning"]
---

## Extracted Knowledge

### When on-chain indexing is too slow, use the Morpho GraphQL API
For Morpho Blue deployments on chains with restrictive RPC limits (Monad public: 100-1000 block getLogs cap), scanning Borrow events from scratch is impractical (1.5M blocks → ~5000 round-trips → hours).

The Morpho-org public GraphQL API at `https://blue-api.morpho.org/graphql` exposes complete market + borrower inventory in a single batch:

```graphql
# Markets per chain
{ markets(first:100, where:{chainId_in:[CHAIN_ID]}, orderBy:SupplyAssetsUsd) {
    items { uniqueKey lltv oracle{address} irmAddress collateralAsset{address symbol decimals} loanAsset{address symbol decimals} }
}}

# Active borrowers per market
{ marketPositions(first:100, where:{chainId_in:[CHAIN_ID], marketUniqueKey_in:["..."], borrowShares_gte:1}) {
    items { user{address} state{borrowShares collateral supplyShares} }
}}
```

This gets us **80 markets + 360 borrowers in ~5 seconds** on Monad — replaces hours of on-chain event scanning. The on-chain indexer becomes delta-only (recent events since cursor) instead of full-history scanner.

### The "bootstrap once, delta after" indexer pattern
- **bootstrap** (`seed-from-morpho.js`): pull everything via GraphQL, write positions.json + cursor.json with current block. Run ONCE per chain on initial setup.
- **delta indexer** (`indexer.js`): scans only from cursor to current block. Small windows, RPC-friendly.
- **monitor**: live-reads quantitative state (per the felix decimals-bug fix), uses positions.json only as the directory.

Result: indexer's job is small and steady-state. Initial bootstrap cost is bounded.

### Monad RPC quirks (and how to discover them)
- `rpc.monad.xyz` (official): WSS works for `newHeads` ✓; getLogs **capped at 100 blocks** ✗
- `monadinfra.com`: 100-block getLogs cap, no WSS
- **`drpc.org`: 1000-block getLogs cap ✓, no WSS for Monad (but WSS available on rpc.monad.xyz)**

Optimal split:
- WSS via `rpc.monad.xyz` (free, sub-second)
- HTTP getLogs via `drpc.org` only (highest range)
- HTTP eth_call (position reads, oracle reads) via rotation across all three

### Always probe RPC limits before assuming defaults
Even within "EVM-compatible" chains, getLogs limits vary 10-100×:
| Chain | Typical getLogs cap |
|-------|---------------------|
| Ethereum mainnet | 10,000 blocks |
| Base | 10,000+ |
| Berachain publicnode | 50,000 |
| Sonic publicnode | 10,000 |
| HyperEVM Alchemy | 10 (yes, ten) |
| HyperEVM public | 1,000 |
| Monad rpc.monad.xyz | 100 |
| Monad drpc.org | 1,000 |

Always run a binary-search probe on a new chain before setting CHUNK_SIZE. Test 100/500/1000/2000/5000/10000 to find the actual ceiling. Surprises here cost hours of debugging.

### Rate-limit Promise.all in monitor live-reads
On rate-limited RPCs, `Promise.all` of 188 parallel `eth_call`s saturates the limit and 70%+ fail. Solutions in priority order:
1. **Multicall3 batching** — pack 60-80 reads into one RPC call. Already used in HyperLend/HypurrFi.
2. **Serial chunks with throttle** — 20 at a time with 100ms gap. Slower but safer.
3. **Skip failed reads** — `if (!live) continue;` (already in Felix monitor). Doesn't fix root cause but prevents the cascade crash.

For Monad's 188 active positions, plan A (Multicall3) is the right call.

## Proposed Skill Content

Extend `defi-liquidations` with a "Bootstrap from protocol API" section:

- **For Morpho lanes**, ALWAYS check if the protocol's GraphQL API can seed positions.json before writing an on-chain Borrow-event scanner. Saves hours on restrictive-RPC chains.
- **Test getLogs caps per RPC endpoint** with a binary-search probe before deploying an indexer. Surprises here cost the first hour of every new lane.
- **The "bootstrap once, delta after" pattern**: GraphQL seed → small on-chain delta scans. Cleaner than full-history scans.
- **For monitors on rate-limited RPCs**, use Multicall3 batching. Single-call rate limits >> per-tx rate limits.
- **Different RPCs on the same chain have wildly different capabilities**. Always probe all of them before picking one as primary.
