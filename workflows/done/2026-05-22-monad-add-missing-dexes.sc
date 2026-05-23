---
domain: defi-liquidations
source_task: 2026-05-22-monad-add-missing-dexes.md
date: 2026-05-22
keywords: [curve-stableswap, pancakeswap-v3, multi-dex-dispatcher, get_dy, exchange, yield-vault-collateral, geckoterminal-discovery]
---

## Extracted Knowledge

### Curve stableswap integration pattern
Curve pools have NO factory.getPool — pools are manually registered. Each pool exposes
its tokens via `coins(uint256 i)` returning the token at index `i`. Swap is direct on
the pool (no router):
```
exchange(int128 i, int128 j, uint256 dx, uint256 min_dy)  // selector 0x3df02124
get_dy(int128 i, int128 j, uint256 dx)                    // selector 0x5e0d443f
```
- `i`/`j` are int128 token indices in coins[]
- Output goes to `msg.sender` (NOT a `recipient` arg — different from Uni V3 SwapRouter)
- Catalog entries hold `{ address, coins[], symbols[] }` for each registered pool
- Liquidator's swapTarget IS the pool address (since Curve has no router)
- Liquidator approve() must allow the pool to pull `dx` of tokenIn

### PancakeSwap V3 = canonical Uni V3 fork (drop-in)
Same SwapRouter02 / QuoterV2 ABI as Uniswap V3. Just add a catalog entry:
- Factory typically same `factory()` selector
- Router/Quoter often deployed at same canonical addresses across chains (e.g.
  `0x1b81D678ffb9C0263b24A97847620C99d213eB14` SwapRouter on BSC, Monad, etc.)
- **Fee tiers are `[100, 500, 2500, 10000]`** — NOTE the 2500 not 3000. Don't reuse
  Uni V3's `[100, 500, 3000, 10000]` blindly.

### Multi-DEX dispatcher pattern
Each DEX entry has a `routerSig` string. `findBestPool` dispatches:
```js
if (dex.routerSig === 'v3-classic')   return findBestV3(...);
if (dex.routerSig === 'v4-universal') return findBestV4(...);
if (dex.routerSig === 'curve-stable') return findBestCurve(...);
```
And `buildSwapData(pool, ...)` branches on `pool.routerSig` to generate the right
calldata. Adding a new DEX is: (1) add catalog entry with unique `routerSig`,
(2) implement `findBest<X>` + `buildSwapData` branch, (3) register in dispatcher.

### GeckoTerminal endpoint hierarchy for DEX discovery
On lesser-known chains where docs are sparse:
1. `/onchain/networks/{slug}/dexes` — full list of DEXes indexed (e.g. Monad has 26)
2. `/onchain/networks/{slug}/dexes/{dex-slug}/pools?sort=h24_volume_usd_desc` — top
   pools per DEX, gives you pool addresses + TVL
3. From pool address, on-chain reads: `factory()` (V3), `vault()` (Balancer),
   `coins(i)` (Curve), `getTokenX/Y/binStep` (TraderJoe LB) — identify type
4. Canonical router addresses often hold across chains for same DEX

### Yield-aggregator vault collateral = structurally unfireable for atomic arb
When a Morpho market has collateral that's a yield-aggregator vault (`earnAUSD`,
"Hyperithm Delta Neutral Vault", "Yuzu Money Vault"):
- `convertToAssets()` works (returns rate per share)
- `previewRedeem` / `maxRedeem` revert — redemption needs queue/cooldown
- Even ERC-4626 wrappers around liquid tokens (like USDC) can't be atomically redeemed
  because the strategy positions (basis trade, lending, etc.) need time to unwind
- No DEX routing fix unlocks these — the collateral itself isn't an AMM-tradeable
  asset. Same class of position as Liquity stability-pool liquidations: non-atomic only.

Don't waste cycles routing for these. Classify and skip in monitor.

## Proposed Skill Content

Add to `.claude/skills/defi-liquidations.md`:

### Curve handler implementation
Stableswap integration is one new file: catalog entry with pre-registered pools (no
factory), `findBestCurve` calling `get_dy(i, j, dx)`, `buildSwapData` for `curve-stable`
emitting `exchange(i, j, dx, min_dy)` calldata. Liquidator approve()s the pool;
swapTarget IS the pool.

### PancakeSwap V3 fee-tier gotcha
PCS V3 uses `[100, 500, 2500, 10000]` — NOT Uni V3's `[100, 500, 3000, 10000]`. Add
catalog entry with correct fee tiers or you'll silently miss the deepest pools.

### Yield-vault collateral classification
If a market's collateral token has `convertToAssets()` but `previewRedeem` reverts,
classify as non-atomic. No routing/DEX work helps. Skip silently.
