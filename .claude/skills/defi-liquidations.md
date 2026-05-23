---
name: defi-liquidations
description: Building DeFi liquidation bots — protocol mechanics, race architecture, Morpho HF math, contract gotchas, monitor/executor consistency, multi-DEX routing, chain expansion decisions, Silo V2 sToken handling, and economic filters. Surface when working on on-chain liquidation bots, flash-loan-based arb, Morpho/Aave/Liquity/Silo integration, multi-chain expansion, or evaluating which protocols are worth racing.
activation:
  keywords: ["liquidation", "liquidator", "liquidate", "morpho", "morpho-blue", "morpho-graphql", "bend", "beraborrow", "blur", "blend", "silo", "silo-v2", "stoken", "sTokenRequired", "flash loan", "flashloan", "health factor", "lltv", "lif", "oracle price", "race bot", "mev", "lending protocol", "berachain", "monad", "sonic", "shadow", "ramses", "tickspacing", "liquity", "dolomite", "aave", "borrower indexer", "monitor", "executor", "stale state", "false fire", "router-agnostic", "swap path", "multicall3"]
---

## Purpose
DeFi liquidation bots have a small number of recurring patterns: identify a permissionless venue, race other bots to the broadcast, compose atomic flash-loan + liquidate + collateral swap in one tx. This skill captures the protocol-specific gotchas (who gets the bonus, what breaks atomic exit, what the math looks like) and the architecture patterns (indexer + monitor + executor + contract) that recur across protocols.

Use this skill whenever working on: a liquidation bot, an on-chain MEV racer, a Morpho/Aave/Liquity integration, or evaluating "is protocol X worth racing for liquidations."

## Pre-build verification — read the source

Before recommending any liquidation venue, verify by reading the actual `liquidate()` / `seize()` / equivalent source. Protocol docs lie. Specifically check:

1. **Permissionless caller?** Look for `onlyOwner`, `onlyGlobalOperator`, `onlyLender`, role checks. If the function requires anything beyond `msg.sender` having capital, skip the protocol.
2. **Liquidator gets the bonus?** Some protocols send seized collateral to a Stability Pool (Liquity V2 and forks like Beraborrow) — the liquidator only gets a flat gas comp. Search for the recipient address on the seize/transfer call.
3. **Same-tx exit feasible?** If the collateral can't be sold atomically (oracle-signed orderbook like Blur, NFT-AMM not on this chain, etc.), flash-loan arb doesn't work. Test by writing the composite call structure on paper before committing.

**Protocols that fail these checks (don't waste time):**
- **Blur Blend** — `seize()` is gated to `msg.sender == lien.lender`. No third-party liquidator role exists. Even if it did, `BlurExchangeV2.takeBid` requires per-caller off-chain oracle signature, breaking atomic composability from a flash-loan contract.
- **Dolomite** — `LiquidateOrVaporizeImpl` requires `globalOperator`. Liquidations route through Chainlink Keepers; DAO captures the rewards.
- **Liquity V2 forks (Beraborrow, etc.)** — permissionless but `seize` pays liquidator only flat gas comp (~0.0375 WETH + min(0.5% coll, 2 LST)). Bulk of collateral goes to Stability Pool depositors. Only worth racing on `batchLiquidate(address[])` to amortize gas.

**Protocols that pass (worth racing):**
- **Morpho Blue and forks (Bend on Berachain)** — permissionless `liquidate`, full LIF to `msg.sender`, free 0% flash loan in same singleton, atomic composability.
- **Aave V3 and forks** — permissionless `liquidationCall`, 5-15% bonus to `msg.sender`, flash loan via 0.05% Aave or 0% Balancer Vault.

## Morpho Blue mechanics (Bend, Morpho on Ethereum/Base/etc.)

### Liquidation function
```solidity
function liquidate(
    MarketParams calldata marketParams,
    address borrower,
    uint256 seizedAssets,    // 0 = max allowed
    uint256 repaidShares,    // 0 = pay max debt
    bytes calldata data      // optional callback payload
) external returns (uint256 seizedAssets, uint256 repaidAssets);
```
Permissionless. Pass `seizedAssets = max` to take all liquidatable collateral.

### LIF formula — caps at 15% for all reasonable LLTVs
```
LIF = min(maxLIF, 1 / (CURSOR × (1 - LLTV)))
where CURSOR = 0.3, maxLIF = 1.15
```

For any LLTV ≥ ~0.75, LIF caps at 1.15 (15% bonus). At LLTV 0.86: `1/(0.3 × 0.14) = 23.8`, `min(1.15, 23.8) = 1.15`. Protocol docs sometimes claim 5% — they're computing the formula wrong. Always verify by plugging the LLTV into the cited formula.

### Flash loan — 0% fee, same singleton
```solidity
function flashLoan(address token, uint256 assets, bytes calldata data) external;

// Your contract implements:
interface IMorphoFlashLoanCallback {
    function onMorphoFlashLoan(uint256 assets, bytes calldata data) external;
}
```
Zero fee. Must approve Morpho to pull `assets` back before the callback returns. Enables atomic `flashLoan → liquidate → swap collateral → repay` in one external call.

### Distinguishing the singleton from a MetaMorpho vault
Both addresses appear in docs/registries with similar labels. Probe:
- `DOMAIN_SEPARATOR()` (selector `0x3644e515`) — succeeds on singleton, reverts on vault
- `MORPHO()` — returns singleton address on vault, reverts on singleton
- `asset()` / `name()` / `symbol()` — ERC-4626 surface; succeed on vault, revert on singleton

If `DOMAIN_SEPARATOR()` works and `MORPHO()` reverts → it's the singleton.

### `position()` decoding
```solidity
function position(bytes32 id, address user) returns (
    uint256 supplyShares,   // 32 bytes — lender claim on supplied liquidity
    uint128 borrowShares,   // 16 bytes padded to 32 — debt shares (NOT assets)
    uint128 collateral      // 16 bytes padded to 32 — raw collateral amount (NOT USD)
);
```
Total = 96 bytes. The uint128 fields are right-padded in ABI encoding. A position with `supplyShares > 0` and `borrowShares == 0` is a pure lender — skip these for liquidation purposes.

### Shares-to-assets conversion (round up for liquidation checks)
```
borrowAssets = (borrowShares × totalBorrowAssets + totalBorrowShares - 1) / totalBorrowShares
```
Round-up matches Morpho's internal liquidation check. Re-read `totalBorrowAssets` and `totalBorrowShares` from `market(Id)` on every reconcile — they change every block due to interest accrual.

### Health factor formula
```
maxBorrowValue = collateral × oraclePrice × lltv / (1e36 × 1e18)
borrowAssets   = (borrowShares × totalBorrowAssets + totalBorrowShares - 1) / totalBorrowShares
HF             = maxBorrowValue / borrowAssets    // >= 1 healthy, < 1 liquidatable
```

The oracle returns price scaled to `10^(36 + loanDecimals - collateralDecimals)`. The 1e36 in the denominator cancels both the oracle scale and the decimal difference — you don't need per-token decimal handling in the formula. The standard Morpho oracle interface is just `function price() external view returns (uint256)`.

### Liquidate event layout
```solidity
event Liquidate(
    Id indexed id,            // topic[1]
    address indexed caller,   // topic[2] (the liquidator)
    address indexed borrower, // topic[3]
    uint256 repaidAssets,     // data slot 0 (loan-token units)
    uint256 repaidShares,     // data slot 1
    uint256 seizedAssets,     // data slot 2 (collateral-token units)
    uint256 badDebtAssets,    // data slot 3 (0 if collateral covered debt)
    uint256 badDebtShares     // data slot 4
);
```
Topic 0: `0xa4946ede45d0c6f06a0f5ce92c9ad3b4751452d2fe0e25010783bcab57a67e41`. 5 × 32 bytes in data. Liquidator gross profit ≈ `repaidAssets × (LIF - 1)`. Implied collateral price = `repaidAssets × LIF / seizedAssets`.

### Morpho event topic table
| Event | Topic |
|---|---|
| `Borrow(bytes32,address,address,address,uint256,uint256)` | `0x570954540bed6b1304a87dfe815a5eda4a648f7097a16240dcd85c9b5fd42a43` |
| `Supply(bytes32,address,address,uint256,uint256)` | `0xedf8870433c83823eb071d3df1caa8d008f12f6440918c20d75a3602cda30fe0` |
| `Withdraw(bytes32,address,address,address,uint256,uint256)` | `0xa56fc0ad5702ec05ce63666221f796fb62437c32db1aa1aa075fc6484cf58fbf` |
| `Repay(bytes32,address,address,uint256,uint256)` | `0x52acb05cebbd3cd39715469f22afbf5a17496295ef3bc9bb5944056c63ccaa09` |
| `SupplyCollateral(bytes32,address,address,uint256)` | `0xa3b9472a1399e17e123f3c2e6586c23e504184d504de59cdaa2b375e880c6184` |
| `WithdrawCollateral(bytes32,address,address,address,uint256)` | `0xe80ebd7cc9223d7382aab2e0d1d6155c65651f83d53c8b9b06901d167e321142` |
| `Liquidate(...)` | `0xa4946ede45d0c6f06a0f5ce92c9ad3b4751452d2fe0e25010783bcab57a67e41` |
| `CreateMarket(...)` | `0xac4b2400f169220b0c0afdde7a0b32e775ba727ea1cb30b35f935cdaab8683ac` |
| `AccrueInterest(bytes32,uint256,uint256,uint256)` | `0x9d9bd501d0657d7dfe415f779a620a62b78bc508ddc0891fbbd8b7ac0f8fce87` |
| `FlashLoan(address,address,uint256)` | `0xc76f1b4fe4396ac07a9fa55a415d4ca430e72651d37d3401f3bed7cb13fc4f12` |

## Bot architecture (4 components)

### 1. Indexer — discover positions
**Events for discovery, on-chain reads for state.** Scan `SupplyCollateral` + `Borrow` events to enumerate `(marketId, borrower)` pairs. For each pair, call `position(id, addr)` directly to get current values. Never derive position state from event deltas — too easy to miss an event and drift.

Per-market collateral decimals must be read per token (WBTC=8, WETH=18, USDC=6). A hardcoded `/1e18` divisor silently mis-displays WBTC by 10 orders of magnitude.

Discover ALL markets via `CreateMarket` event scan — docs lists may be incomplete.

### 2. Monitor — compute HF, alert, arm
WSS `newHeads` triggers per-block re-evaluation. Read all market oracles in parallel via `Promise.all`. Compute HF for every active position. Use multi-tier thresholds:
- `WARN = 1.10` — Telegram heads-up
- `ARM = 1.02` — executor should pre-sign
- `FIRE = 1.00` — executor must broadcast

Dedup alerts per `(positionKey, threshold)` tuple with 5-min TTL. Without the threshold in the key, crossing WARN→ARM→FIRE squashes the FIRE alert as a "repeat."

Filter alerts on `debtUsd < MIN_DEBT_USD` (15% × $20 = $3 barely covers gas). Keep tracking dust positions, just don't alert.

Cross-process IPC: write `armed/<marketId>-<borrower>.json` (atomic temp+rename) when a position fires. Executor watches the dir.

### 3. Executor — pre-sign, race, broadcast
Per-position pre-sign as soon as it arms (HF < 1.02). Hold signed tx in memory. On each block, re-check HF; if HF < 1.0, broadcast via dual-RPC race (`Promise.any([publicnode, official])`). Wall-clock fallback timer fires at T+Ns in case WSS dropped events. Pattern proven on MIBERA loan-132 (won 13/13 NFTs in one shot).

### 4. Atomic contract — flashLoan → liquidate → swap → repay
```solidity
function liquidate(MarketParams calldata mp, address borrower, ..., uint256 minProfitWei) external onlyOwner {
    // Trigger Morpho flash loan with payload
    bytes memory data = abi.encode(mp, borrower, ...);
    MORPHO.flashLoan(mp.loanToken, fundingAmount, data);
}

function onMorphoFlashLoan(uint256 assets, bytes calldata data) external {
    require(msg.sender == address(MORPHO));
    (MarketParams memory mp, address borrower, ...) = abi.decode(data, (...));

    // 1. Liquidate (uses flash-loaned loan token to repay debt, receives collateral)
    MORPHO.liquidate(mp, borrower, max, 0, "");

    // 2. Swap collateral → loan token on DEX (Kodiak on Berachain)
    IERC20(mp.collateralToken).approve(DEX_ROUTER, type(uint256).max);
    DEX_ROUTER.exactInputSingle(swapParams);

    // 3. Approve Morpho to pull back flash loan + check profit
    uint256 balance = IERC20(mp.loanToken).balanceOf(address(this));
    require(balance >= assets + minProfitWei, "no profit");
    IERC20(mp.loanToken).approve(address(MORPHO), assets);
    // Profit (balance - assets) stays in contract for owner to sweep
}
```

`minProfitWei` enforced client-side AND in the callback — if profit < threshold, revert the whole tx and pay only gas.

## Chain-specific notes

### Berachain
- Pyth deployed at `0x2880aB155794e7179c9eE2e38200202908C17B43` — but Bend/Beraborrow/Dolomite all use Redstone push or Chainlink instead. No `pyth.updatePriceFeeds() + liquidate()` bundling needed.
- BEX flash loans are disabled via prohibitive protocol fee.
- Bend's Morpho singleton (`0x24147243f9c08d835C218Cda1e135f8dFD0517D0`) is the canonical flash loan source.
- publicnode RPC: `eth_getLogs` accepts ~50k blocks reliably; 100k blocks fail when running 14+ chunks in parallel.
- `eth_getCode` against publicnode for historical block returns "historical state not available" beyond the last ~128 blocks. To find a contract deploy block, scan `eth_getLogs` for the first relevant event instead.

### Ethereum / general
- Maker DSS-Flash: still live mainnet, 0% fee, DAI only.
- Balancer V2 Vault: `0xBA12222222228d8Ba445958a75a0704d566BF2C8`, 0% fee, broadest token support.
- Aave V3 flash loan: 0.05%.
- For race performance on mainnet, you need Flashbots-equivalent private orderflow — public mempool is uncompetitive.

### Berascan v1 API is deprecated
Use Etherscan v2 with chainid param: `https://api.etherscan.io/v2/api?chainid=80094&module=...`. Requires API key (free tier exists).

## BigInt patterns for liquidation math

```js
// Naive Number(a) / Number(b) overflows for large positions. Use:
const HF = Number((maxBorrow * 10000n) / borrowAssets) / 10000;  // 4-decimal precision

// Round up shares→assets:
const borrowed = (borrowShares * totalBorrowAssets + totalBorrowShares - 1n) / totalBorrowShares;
```

## Targets filter checklist

A venue is worth racing if ALL apply:
1. Permissionless liquidation entry point (no role gating)
2. Liquidator receives the bonus directly to `msg.sender` (not socialized to a Stability Pool, not fixed gas comp)
3. Same-tx atomic exit on a DEX (not held inventory)
4. Flash loan source available at same-tx scope (Balancer 0%, Morpho 0%, Aave 0.05%)
5. Real activity: ≥4-8 liquidations/month in recent history

Less than all 5 = don't bother.

## Failure Modes

**Confident-wrong about NFTfi/Blend race target**
Earlier ranked Blend as the top opportunity ($500-10k/hit, less competition) based on protocol docs framing. The contract reveals `seize()` is lender-only. Lesson: read source code BEFORE recommending — protocol docs aren't trustworthy on who-can-call.

**LIF docs vs formula mismatch**
Bend docs claim LIF = 1.05 at LLTV 86%. Plugging into `min(1.15, 1/(0.3 × 0.14))` gives 1.15 (cap). The docs' example math is wrong. Always verify numeric examples by computing from the cited formula.

**Misreading "Morpho (Vault)" label**
Bend's docs list the singleton labeled "Morpho (Vault): 0x24147243..." — the word "Vault" suggests MetaMorpho, which doesn't emit Liquidate events. Probe `DOMAIN_SEPARATOR()` vs `MORPHO()` to confirm.

**Position display bug from hardcoded /1e18 divisor**
WBTC has 8 decimals. A naive `/1e18` divisor makes 0.26 WBTC display as `0.0000000000`. Per-market collateral decimals must be read from each collateral token via `decimals()` (selector `0x313ce567`). The data persisted is fine — only the formatting is broken.

**borrowShares ≠ borrowAssets**
Shares grow with accrued interest. A "borrower with 14 billion shares" might be a $10 debtor or a $100k debtor depending on the market's total shares/assets ratio. Always convert via `borrowAssets = shares × totalBorrowAssets / totalBorrowShares` with current totals.

**Naive WSS subscription pulls 1500 FlashLoan events/hour**
On Bend, a single arb bot (`0xc1fad5...`) fires ~1500 FlashLoan events per hour using Morpho's free flash loan as capital. A naive `topics: [Object.values(MorphoTopics)]` filter pulls them all. Filter to position-affecting topics only (`SupplyCollateral`, `WithdrawCollateral`, `Borrow`, `Repay`, `Liquidate`).

**Naive Number/Number for HF causes overflow**
`Number(maxBorrow) / Number(borrowAssets)` loses precision and may return Infinity for large positions. Use `Number((maxBorrow * 10000n) / borrowAssets) / 10000` to preserve precision via integer scaling.

## Monitor/executor consistency — live-read every quantitative input

A liquidation bot is typically Indexer → Monitor → Executor. If the **monitor** computes HF from the indexer's cached `(collateral, borrowShares)` instead of live-reading from the protocol, every collateral top-up by the borrower silently produces a false FIRE. The executor's "fresh HF" re-verify catches it (`HF healed` skip path), but the pipeline still looks healthy and you can no longer tell legitimate fires from false-fire noise.

**Diagnostic signature** (Felix 2026-05-21 incident):
- Monitor log: `🔥 FIRE 0xMID:0xUSER HF 0.9966` recurring
- Executor log: `fresh HF: 1.3113 → HF healed (1.3113) — skipping silently` constant value across many polls

A constant "fresh HF" across many polls is the smoking gun — live state drifts block-to-block as oracles move. A stable executor HF means cached input on the monitor side.

```js
// WRONG — uses indexer's cached p.collateral / p.borrowShares
const hf = computeHF(p, market, oraclePrice, blockTimestamp);

// RIGHT — live-read position state per tick
const live = await readPositionState(marketId, borrower);  // morpho.position() / pool.getUserAccountData()
if (!live) continue;  // skip this tick — DO NOT fall back to cache
const pLive = { ...p, collateral: live.collateral, borrowShares: live.borrowShares };
const hf = computeHF(pLive, market, oraclePrice, blockTimestamp);
```

Never fall back to stale data on RPC failure — a missed tick is safe (HF can't change drastically in 1 block); a false-FIRE from stale data is not. After this fix, the indexer's `positions.json` is a **directory** (which users exist in which markets), not the source of truth for position values. Its freshness becomes a perf optimization, not a correctness requirement. Applies equally to Morpho Blue (`position(bytes32,address)`), Aave V3 (`getUserAccountData(address)`), and Silo (`getDebt() + collateral()` per-silo).

## Bootstrap from Morpho GraphQL API (restrictive-RPC chains)

For Morpho deployments on chains with low getLogs caps (Monad ≤1000 blocks, HyperEVM Alchemy ≤10 blocks), scanning Borrow events from scratch is impractical — 1.5M blocks × 5000 round-trips = hours.

The Morpho-org public GraphQL endpoint at `https://blue-api.morpho.org/graphql` returns the complete market + borrower inventory in seconds:

```graphql
# Markets per chain
{ markets(first:100, where:{chainId_in:[CHAIN_ID]}, orderBy:SupplyAssetsUsd) {
    items { uniqueKey lltv oracle{address} irmAddress
            collateralAsset{address symbol decimals}
            loanAsset{address symbol decimals} }
}}

# Active borrowers per market
{ marketPositions(first:100, where:{chainId_in:[CHAIN_ID], marketUniqueKey_in:["..."], borrowShares_gte:1}) {
    items { user{address} state{borrowShares collateral supplyShares} }
}}

# Recent liquidations (revenue floor signal)
{ transactions(first:20, where:{chainId_in:[CHAIN_ID], type_in:[MarketLiquidation]}, orderBy:Timestamp, orderDirection:Desc) {
    items { hash timestamp data { ... on MarketLiquidationTransactionData {
              seizedAssetsUsd repaidAssetsUsd
              market { collateralAsset{symbol} loanAsset{symbol} } } } }
}}

# Currently at-risk positions (for chain evaluation)
{ marketPositions(first:20, where:{chainId_in:[CHAIN_ID], healthFactor_lte:1.1, borrowShares_gte:1}, orderBy:HealthFactor, orderDirection:Asc) {
    items { user{address} market{collateralAsset{symbol} loanAsset{symbol}} healthFactor state{borrowAssetsUsd} }
}}
```

**Bootstrap once, delta after**:
- `seed-from-morpho.js` (one-shot): pull everything via GraphQL → write `positions.json` + `cursor.json` at current block
- `indexer.js` (long-running): scans only from cursor → current. Small windows, RPC-friendly
- `monitor.js`: live-reads quantitative state, uses positions.json only as the directory

Result: 80 markets + 360 borrowers in ~5 seconds on Monad vs. hours of on-chain scanning. No auth needed. Returns NULL for chains without Morpho deployment.

## Always probe RPC limits before assuming defaults

Even within "EVM-compatible" chains, getLogs limits vary 10-100×:

| Chain | RPC | Typical getLogs cap |
|---|---|---|
| Ethereum mainnet | most | 10,000 blocks |
| Base | most | 10,000+ |
| Berachain | publicnode | 50,000 |
| Sonic | publicnode | 10,000 |
| HyperEVM | Alchemy | **10** (silent fail beyond) |
| HyperEVM | public | 1,000 |
| Monad | rpc.monad.xyz | 100 |
| Monad | drpc.org | 1,000 |
| Monad | monadinfra.com | 100 |

Run a binary-search probe on a new chain (test 100/500/1000/2000/5000/10000) to find the actual ceiling. Surprises here cost hours of debugging. Also probe WSS availability — on Monad, `rpc.monad.xyz` has WSS but small getLogs; `drpc.org` has 10× larger getLogs but no WSS. Optimal split: WSS via rpc.monad.xyz, getLogs via drpc.org, eth_call rotated across all.

## Rate-limit Promise.all in monitor live-reads

On rate-limited RPCs, `Promise.all` of 188 parallel `eth_call`s saturates the limit and 70%+ fail. Solutions in priority order:

1. **Multicall3 batching** — pack 60-80 reads into one RPC call. Best for >50 positions.
2. **Serial chunks with throttle** — 20 at a time with 100ms gap. Slower but safer.
3. **Skip failed reads** — `if (!live) continue;` (the felix monitor pattern). Doesn't fix root cause but prevents the cascade crash.

For ≤20 borrowers per tick (watch-list mode), plain `Promise.all` is fine. Don't reach for Multicall3 prematurely.

## Router-agnostic liquidator contracts

Don't hardcode a DEX router. Accept `(address swapTarget, bytes calldata swapData)` as **per-call parameters**:

```solidity
function liquidate(..., address swapTarget, bytes calldata swapData) external onlyOwner {
    // ...
    if (collBal > 0 && swapTarget != address(0)) {
        IERC20(collateral).approve(swapTarget, collBal);
        (bool ok, bytes memory ret) = swapTarget.call(swapData);
        if (!ok) revert SwapFailed(ret);
    }
}
```

Benefits:
- Add a new DEX without redeploying the liquidator
- Off-chain executor compares routes at execution time and picks the best
- Per-call `approve` keeps allowance fresh; router-switching has zero state cost

The off-chain executor builds calldata against any router (Kodiak, HyperSwap, Project-X, 1inch, aggregator). Pair this with a multi-DEX best-quote function for routing.

## Multi-DEX best-quote pool selection (V3 forks)

Many chains have 5+ Uni V3 forks (HyperEVM, Sonic). The right pool depends on trade size — concentrated liquidity means active liquidity at the current tick can be tiny even when total reserves are huge.

**Anti-pattern: probe + quote separately**
```js
// WRONG — pool selected without knowing trade size
const best = await findBestPool(rpc, tokenIn, tokenOut);
const out = await quoteSwap(rpc, tokenIn, tokenOut, best.tickSpacing, amountIn);
```

**Correct: pass real amountIn to selection**
```js
async function findBestPool(rpc, tokenIn, tokenOut, amountIn) {
  // Enumerate every DEX × every fee tier / tickSpacing
  const candidates = (await Promise.all(DEXES.flatMap(d => d.feeTiers.map(async fee => {
    const pool = await factory.getPool(tokenIn, tokenOut, fee);
    return pool !== ZeroAddress ? { dex: d.name, router: d.router, quoter: d.quoter, fee, pool } : null;
  })))).filter(Boolean);

  // Quote at the REAL amountIn (precise via quoter, or slot0 fallback)
  const quoted = await Promise.all(candidates.map(async c => ({
    ...c,
    expectedOut: c.quoter
      ? await quoter.quoteExactInputSingle({...})
      : await spotQuoteFromSlot0(c.pool, ...)  // fallback when no quoter exists
  })));

  // Filter reverts/zeros, pick max
  return quoted.filter(q => q.expectedOut > 0n).sort(byExpectedOut)[0];
}
```

Real impact (Shadow on Sonic, same trade, same time): wrong pool → `quote: 211 wS, profit: -2,896 wS (skip)`. Right pool → `quote: 3,217 wS, profit: +178 wS (fire)`. 15× output difference, sign-flipped profit.

**Quoter availability varies**: HyperSwap V3 doesn't expose a public quoter — fall back to `slot0` math: `(sqrtPriceX96² / 2^96)` gives the token1/token0 ratio. Ignores price impact but enough for the executor's profit-gate.

## V3 fork ABI variations (don't assume canonical)

| Fork | Pool param | Router struct field |
|---|---|---|
| Uniswap V3 (canonical), Kodiak, Project-X | `uint24 fee` | `fee` | `exactInputSingle` selector `0x414bf389` |
| Ramses V3 (Shadow on Sonic, Aerodrome Slipstream on Base, Velodrome Slipstream on Ink) | `int24 tickSpacing` | `tickSpacing` | `exactInputSingle` selector `0xa026383e` |
| Algebra V3 (SwapX, etc.) | varies | varies | varies |

```solidity
// Canonical V3 — selector 0x414bf389
exactInputSingle((tokenIn, tokenOut, uint24 fee, recipient, deadline, amountIn, amountOutMinimum, sqrtPriceLimitX96))

// Ramses V3 fork — selector 0xa026383e
exactInputSingle((tokenIn, tokenOut, int24 tickSpacing, recipient, deadline, amountIn, amountOutMinimum, sqrtPriceLimitX96))
```

**Critical gotcha — wire-identical, selector-different**: For small positive tickSpacings (1, 50, 100, 200, 2000) the ABI-encoded calldata bytes after the selector are IDENTICAL between the int24 and uint24 forms. But the function selectors differ because keccak256 of the full signature string differs. Symptom of using the wrong interface: SwapRouter bare-reverts with no return data (function doesn't exist at that selector). Caught Tydro on Ink 2026-05-22 — every fire would have failed silently in production. Diagnostic: in callTracer trace, look for the liquidator's `SwapFailed(bytes)` selector propagating up — it means the inner router call returned bare empty bytes.

`factory()` pointer being right doesn't mean the router signature matches. Confirm ABI before assuming compatibility. For Ramses-style: tickSpacings common values 1, 10, 50, 100, 200, 1000, 2000.

## DEX discovery without docs/SDK

When you know ONE contract on a chain (e.g., factory from CoinGecko `/onchain/networks/{slug}/dexes`) and need the rest:

1. **Get the factory's deployer** via Etherscan V2 (`module=contract&action=getcontractcreation`)
2. **Enumerate the deployer's contract creations** (`module=account&action=txlist`) — V3 deploys cluster Factory → SwapRouter → NFPM → NFTDescriptor → QuoterV2 within a small block range
3. **Verify each candidate's `factory()` pointer** matches the live factory (multiple test/real deployments are common)
4. **Resolve verified names** via `module=contract&action=getsourcecode`
5. **Selector-probe unverified candidates**: try `quoteExactInputSingle((address,address,uint256,uint24,uint160))` against each — sensible output identifies the Quoter

Standard fallback sequence when discovery is needed:
1. CoinGecko `/onchain/networks/{slug}/dexes` — inventory + top pool TVLs
2. Etherscan V2 multichain (`chainid=999` HyperEVM, `146` Sonic, `80094` Berachain) for verified contracts
3. On-chain `factory()` / selector tests for unverified
4. WebFetch project's main site last resort (often JS-rendered SPAs)

## Silo V2 sToken model — atomic liquidation has a structural ceiling

When `SiloLens.maxLiquidation` returns `sTokenRequired=true`, the protocol is saying: "the collateral silo's liquidity is too low to give you underlying — you must take sToken (ERC4626 share) instead." A liquidator CAN accept the sToken, but the in-tx redeem can still revert.

Two separate constraints:
1. `liquidationCall(_receiveSToken=true)` — protocol lets you take the sToken ✓
2. `collateralSilo.redeem(shares, you, you)` — requires `getLiquidity() > 0` ✗ often fails when sTokenRequired was set

The flashloan repay needs underlying debt-asset. If redeem fails AND no DEX pool exists for the sToken, the tx must hold the sToken → flashloan can't be repaid → tx reverts. **Atomic liquidation impossible in this state — not a code bug, a chain state issue.**

**Diagnostic — is a Silo V2 fire atomically fireable?**
```js
const silo = new Contract(siloAddr, [
  "function getLiquidity() view returns (uint256)",       // canonical "redeemable now"
  "function getCollateralAssets() view returns (uint256)", // total deposited
  "function getDebtAssets() view returns (uint256)",       // total borrowed (util = debt/coll)
  "function totalAssets() view returns (uint256)",         // ERC4626 vault total
  "function previewRedeem(uint256 shares) view returns (uint256)",  // theoretical, ignores liquidity
  "function asset() view returns (address)",
]);
// Also check: CoinGecko /onchain/networks/{slug}/tokens/{stokenAddress}/pools for DEX pool existence
```

`previewRedeem` does NOT account for liquidity constraints — can return non-zero when `getLiquidity() = 0`. Don't trust it as a liquidity check.

**The multi-tx hold-and-redeem alternative** (only worth it if profit > 10× capital opportunity cost):
1. Replace flashloan with own-capital funding (you need debt asset in wallet)
2. `liquidationCall(_receiveSToken=true)` with own capital
3. Hold sToken (exposed to silo risk)
4. Monitor `siloCollateral.getLiquidity()`, call `redeem` when positive
5. Swap underlying → debt asset

For sub-$100 profit fires, **skip is correct**. But always alert on big skipped opportunities — silent skip = blind. Threshold a Telegram alert at meaningful profit floor so a $5k fire doesn't disappear into the skip log.

## Anvil smoketest pattern for liquidator contracts

The minimum viable smoke test:
1. Spin up anvil fork at current block (`--fork-url <rpc> --port 8545`)
2. Read the actual borrower's position state (collateral, borrowShares, oracle price)
3. Compute current HF — confirm it matches your monitor
4. (Optional) `anvil_setCode` the oracle to crash collateral price → HF<1
5. **Always pre-flight via `eth_call`** before broadcasting — captures revert reason as decoded `error.data`
6. Broadcast the `liquidate()` tx with realistic args
7. Decode Liquidated event from receipt logs
8. Verify owner ERC-20 balance increased by event's `profit` field

Pre-flight `eth_call` is non-negotiable. Solidity custom errors (`revert MarketNotCreated()`) and Panic codes are only visible through call revert data, NOT transaction receipts. Without it you'll see generic "transaction execution reverted" and waste time guessing.

**Oracle override via `anvil_setCode`** (works for any oracle: Chainlink, Redstone, Pyth, proprietary):
```js
const crashedPrice = (realPrice * 65n) / 100n;
const priceHex = crashedPrice.toString(16).padStart(64, '0');
// PUSH32 price; PUSH1 0; MSTORE; PUSH1 0x20; PUSH1 0; RETURN
const bytecode = '0x7f' + priceHex + '60005260206000f3';
await provider.send('anvil_setCode', [oracleAddress, bytecode]);
```
Replaces the oracle for ALL function calls. More reliable than `anvil_setStorageAt` (which requires reverse-engineering the storage slot). Caveat: returns the same value for any function — fine for Morpho (only calls `price()`).

**Aave V3 fork with adapter oracle** (Tydro, Chaos-Labs-fronted oracles): direct anvil_setCode on the top-level `AaveOracle` crashes ALL asset prices uniformly because every `getAssetPrice` returns the same constant — HF doesn't move because both collateral and debt drop equally. And `getSourceOfAsset()` may return a feed-ID, not a Chainlink aggregator, so you can't override the source directly. Strategy that works:

```js
// 1. Deploy a stub aggregator at a fresh address
const stubAddr = '0x000000000000000000000000000000000000aaaa';
await p.send('anvil_setCode', [stubAddr, '0x7f' + priceHex + '60005260206000f3']);

// 2. Find the AaveOracle admin via PoolAddressesProvider.getACLAdmin()
const PAP_OWNER = '0x1dF462e2712496373A347f8ad10802a5E95f053D';  // Tydro's

// 3. Impersonate and call standard Aave V3 setAssetSources
await p.send('anvil_impersonateAccount', [PAP_OWNER]);
await p.send('anvil_setBalance', [PAP_OWNER, '0x' + (10n ** 18n).toString(16)]);
const admin = await p.getSigner(PAP_OWNER);
const oracle = new ethers.Contract(AAVE_ORACLE, [
  'function setAssetSources(address[] assets, address[] sources)',
  'function getAssetPrice(address) view returns (uint256)',
], admin);
await oracle.setAssetSources([COLLATERAL_ASSET], [stubAddr]);
// Now only that one asset has a crashed price → HF actually drops
```

**Right-sizing debt chunk in oracle-crash smoke** — after the crash, seize amounts can be huge (massive token amounts at deflated oracle prices) that exceed DEX pool depth. Cover a small `debtChunk` (e.g. 1000 USDC) instead of `MaxUint256`. Compute expected seize before broadcasting:

```js
expectedSeize = debtChunk * LIF_BPS / 10000n * debtOraclePrice / collOraclePrice  // both 1e8 base
swapAmountIn = expectedSeize * 95n / 100n  // safety margin for rounding
```

Worth knowing: in a real oracle crash, this same math means the first liquidator captures a massive arbitrage between the crashed oracle price and the still-correct DEX price. The Tydro test showed $984 profit on a $1000 chunk (98% bonus) because the DEX didn't see the crash. Real prod fires during oracle incidents would extract proportionally large value.

## Morpho.liquidate ABI gotcha — `seizedAssets = max` overflows

Morpho internally computes `seizedAssetsQuoted = seizedAssets.mulDivUp(collateralPrice, ORACLE_PRICE_SCALE)` **before** clamping to actual liquidatable. If `seizedAssets == type(uint256).max` and `collateralPrice > 0`, this overflows → revert with `Panic(0x11)`.

```js
// WRONG — overflows
iface.encodeFunctionData('liquidate', [marketParams, borrower, ethers.MaxUint256, ...]);

// RIGHT — pass actual collateral, Morpho clamps internally
iface.encodeFunctionData('liquidate', [marketParams, borrower, BigInt(position.collateral), ...]);
```

This bug would silently destroy a production liquidator — every fire reverts with cryptic `Panic OVERFLOW(17)`, you lose gas, never seize. Discovery channel: anvil-fork smoke test. Would NOT have been found by healthy-position revert tests or code review.

## Chain expansion decision framework

TVL ≠ opportunity. Funnel: `TVL → active borrows → near HF=1 → crossings → captured by us`. Each step has heavy attrition. A $2.86B TVL protocol can produce $50-100/day total realized seize across all liquidators; your slice across 10+ bots approaches zero on mature L2s.

**Better proxies than TVL:**
- Count of positions currently at HF<1.10 (near-cliff inventory)
- Recent liquidation count + total seized USD over 7-30 days
- Liquidator `tx.from` diversity (many addresses = competition; few = thin)
- Block time + private mempool dominance (sub-100ms = HTTP-polling architectures lose)

**Decision template (add a new lane?):**
1. TVL exists on chain (DefiLlama `/protocol/{slug}`) — if no, skip
2. At-risk inventory ≥ $50k (Morpho GraphQL / Aave subgraph) — if no, low ceiling
3. Last 7d liquidation volume ≥ threshold — tells you if bots are active or positions just sit healthy
4. Liquidator `tx.from` diversity — high = competitive, low = thin
5. Block time < 1s + private mempool dominant — we lose, skip
6. Code reuse from existing lane — 1-2 day port if yes, 1-2 weeks if no

If 1-5 pass and 6 says reuse, do it. Otherwise no, regardless of TVL.

**Our edge is presence, not speed.** Go to new chains early (Plume, Unichain, Tempo, Monad, Katana, TAC). Once a chain matures (Base did years ago), MEV firms with sub-100ms private-orderflow shops outcompete HTTP-polling bots. **Reject Base unless circumstances change.**

## Failure Modes (additional)

**Stale-indexer false-FIRE loop (Felix 2026-05-21)**
Monitor used cached `(collateral, borrowShares)` from `positions.json`; executor lived-read. A borrower's recent collateral top-up wasn't reflected in cache → monitor armed and fired; executor saw `HF healed (1.31)` and silently skipped on every poll. Smoking gun: constant fresh-HF across many polls. Fix: live-read every quantitative input on the monitor side; never fall back to cache on RPC failure.

**Pool-selection ignores trade size (Shadow on Sonic)**
`findBestPool(tokenIn, tokenOut)` without `amountIn` picked a shallow concentrated-liquidity pool that quoted 15× lower than the optimal pool. Same pair, same time. Sign-flipped profit. Fix: pass real `amountIn` to selection, quote each candidate with that exact size, bundle the quote with the selection result so the caller doesn't double-RPC.

**Silo V2 `sTokenRequired=true` blocks atomic liquidation structurally**
Not a code bug. When the collateral silo's `getLiquidity() = 0`, you can take sToken but can't redeem to underlying in the same tx → flashloan can't repay → revert. Detect with `getLiquidity() > 0` check + sToken DEX pool existence check before firing. Alert on big skips so a $5k fire doesn't vanish into the silent-skip log.

**`seizedAssets = type(uint256).max` panics Morpho.liquidate**
Morpho multiplies seizedAssets by collateralPrice BEFORE clamping. `max × non-zero` overflows uint256 → `Panic(0x11)`. Pass `BigInt(position.collateral)` instead; Morpho clamps internally. Found via anvil smoke test, NOT visible to unit tests or code review.

**Generic "transaction execution reverted" on broadcast hides the real error**
Solidity custom errors and Panic codes aren't surfaced through transaction receipts. Always pre-flight via `provider.call(...)` BEFORE broadcasting in smoke tests — the thrown error has `.data` with the 4-byte selector decodable against contract error definitions.

**Promise.all of N parallel eth_calls cascades on rate-limited RPCs**
On Monad with 188 active positions, parallel reads hit the rate limit → 70%+ fail. Priority: Multicall3 batching (best), serial-chunked throttling (safer), skip-on-fail (cheapest patch). For watch-list mode (≤20 borrowers), plain Promise.all is fine; don't preemptively complicate.

## Uniswap V4 routing (different from V3)

- **Singleton PoolManager** holds ALL pool state. No per-pool deploy, no factory.getPool() existence check.
- **PoolKey** = `(currency0, currency1, uint24 fee, int24 tickSpacing, address hooks)`; `currency0 < currency1` by address; `hooks = 0x0` for vanilla pools.
- **Quoter is the source of truth.** `getPool != 0x0` doesn't exist; instead query the Quoter with the PoolKey. If you get a non-zero amountOut, the pool has liquidity.
- **Quoter ABI has double-paren nested struct** — easy to mis-encode:
  ```solidity
  function quoteExactInputSingle(((address,address,uint24,int24,address), bool, uint128, bytes))
      returns (uint256 amountOut, uint256 gasEstimate);
  ```
- **Capacity probing**: sweep amounts (0.01, 1, 10, 100, 1000) of the input token; the size at which the quoter reverts ≈ pool's effective capacity. Use it to cap per-fire seize.

### UniversalRouter + Permit2 (V4 token-pull is NOT ERC20.approve)

UR pulls tokens through Permit2, not direct ERC20. Three layers:
1. `IERC20.approve(PERMIT2, max)` — one-time, idempotent (liquidator → Permit2)
2. `IAllowanceTransfer(PERMIT2).approve(token, UR, amount, deadline)` — per-call OR with infinite deadline (Permit2 → UR)
3. `UR.execute(commands, inputs, deadline)`

Permit2 canonical address (all chains): `0x000000000022D473030F116dDEE9F6B43aC78BA3`.

If a router-agnostic liquidator's `swapTarget == UniversalRouter`, the contract MUST do the Permit2 hop before `swapTarget.call(swapData)`. A naive `IERC20.approve(swapTarget, amount)` doesn't move tokens through UR.

### V4 swap encoding (working example)

```js
// V4 Actions (from v4-periphery/libraries/Actions.sol)
const V4_ACTIONS = { SWAP_EXACT_IN_SINGLE: 0x06, SETTLE_ALL: 0x0c, TAKE_ALL: 0x0f };
const UR_COMMANDS = { V4_SWAP: 0x10 };

function encodeV4SwapExactInSingle(poolKey, zeroForOne, amountIn, amountOutMin) {
  const abi = ethers.AbiCoder.defaultAbiCoder();
  const actions = '0x' + [V4_ACTIONS.SWAP_EXACT_IN_SINGLE, V4_ACTIONS.SETTLE_ALL, V4_ACTIONS.TAKE_ALL]
    .map(c => c.toString(16).padStart(2, '0')).join('');
  const swapParams = abi.encode(
    ['tuple(tuple(address,address,uint24,int24,address) poolKey, bool zeroForOne, uint128 amountIn, uint128 amountOutMinimum, bytes hookData)'],
    [{ poolKey, zeroForOne, amountIn, amountOutMinimum: amountOutMin, hookData: '0x' }],
  );
  const inputCurrency  = zeroForOne ? poolKey.currency0 : poolKey.currency1;
  const outputCurrency = zeroForOne ? poolKey.currency1 : poolKey.currency0;
  const settleParams = abi.encode(['address','uint256'], [inputCurrency, amountIn]);
  const takeParams   = abi.encode(['address','uint256'], [outputCurrency, amountOutMin]);
  return abi.encode(['bytes','bytes[]'], [actions, [swapParams, settleParams, takeParams]]);
}
```

V4 Action encoding is intricate. Hand-rolling is fine for a single swap path; reach for `@uniswap/v4-sdk` if you need multi-hop. Verified working: 1 wstETH → 1.234 WETH @ 165k gas on Monad anvil fork.

## Slipstream / Ramses V3 fork — SwapRouter selector differs from canonical

- Canonical Uni V3 SwapRouter02 `exactInputSingle` selector: `0x414bf389` (`uint24 fee`)
- Slipstream / Velodrome / Ramses V3 SwapRouter selector: `0xa026383e` (`int24 tickSpacing`)
- Wire bytes for small positive values are identical, but `keccak256` of the signature string differs, so the selector differs.
- Symptom of mis-encoded selector: **bare revert with no return data from the SwapRouter call**.
- Affects: Velodrome Slipstream (Ink), Ramses (Arbitrum/Avalanche), Aerodrome Slipstream (Base), Shadow (Sonic) — any Ramses-fork V3.
- Factory `getPool(tokenA, tokenB, tickSpacing)` selector still matches `uint24` since only the router's parameter name changed.
- Fix: change the router interface fragment to use `int24 tickSpacing` so ethers computes the right selector.

## Partial liquidation cascade — when one swap can't drain the position

For whale positions whose collateral exceeds any single pool's capacity (V4 PoolManager OOG on huge swaps, V3 returns 0 quote), don't skip — halve and retry:

```js
async function findBestPoolWithFallback(rpc, tokenIn, tokenOut, requestedAmount) {
  let amount = BigInt(requestedAmount);
  let pool = await findBestPool(rpc, tokenIn, tokenOut, amount);
  if (pool) return { pool, amount };
  for (let i = 0; i < 6; i++) {  // 64× reduction range
    amount /= 2n;
    if (amount === 0n) return null;
    pool = await findBestPool(rpc, tokenIn, tokenOut, amount);
    if (pool) return { pool, amount };
  }
  return null;
}
```

Pass the **same amount** to both `Morpho.liquidate(seizedAssets, ...)` AND the swap's `amountIn`. The position re-arms naturally after each partial fire; the chain drains the whale over several txs.

Cascade economics: per-fire profit ≈ `$X × (1/k) × m / (1 + m)` (in loan-token); total over draining ≈ `$X × m / (1 + m)` (same as a single full fire would yield if the pool could absorb it). Trade-off: gas per fire × k, but unlocks whales single-fire can't capture.

### The `seizedAssets == swap.amountIn` invariant

With pre-built off-chain swap calldata, `Morpho.liquidate(seizedAssets, ...)` MUST seize exactly the amount the swap will pull:
- Less than swap expects → swap's `transferFrom` fails → tx reverts.
- More than swap expects → contract holds extra collateral (acceptable; sweep to OWNER).

Don't pass `MaxUint256` and rely on Morpho's internal clamp. Compute `expectedSeized` deterministically and pass it explicitly. **Also cap at borrower's collateral** for deeply-underwater positions:

```js
const rawExpectedSeized = (seizedValue * ORACLE_SCALE) / oraclePrice;
const expectedSeized = rawExpectedSeized > collateral ? collateral : rawExpectedSeized;
```

This bug only surfaces on HF << 1 positions; normal-HF fires (1.001–1.05) won't trip it. Easy to miss without an oracle-crash smoke test.

## Modern Aave V3 forks — typed custom errors

Legacy Aave V3 emits `revert(Errors.HEALTH_FACTOR_NOT_BELOW_THRESHOLD)` → `Error("35")` with selector `0x08c379a0`. Modern forks (Tydro, etc.) emit `revert HealthFactorNotBelowThreshold()` → custom selector `0x930bb771`. Both are in the wild — handle both.

A legacy `/35|HEALTH_FACTOR/i` string check matches nothing on modern forks → false "unknown error" alerts.

Pre-load these modern Aave V3 selectors in your decoder:

| Selector | Error | Meaning |
|---|---|---|
| `0x930bb771` | `HealthFactorNotBelowThreshold()` | healthy-position skip (expected) |
| `0x6679996d` | `HealthFactorLowerThanLiquidationThreshold()` | healthy-position skip (expected) |
| `0x6d305815` | `ReserveFrozen()` | unexpected |
| `0xd37f5f1c` | `ReservePaused()` | unexpected |
| `0x823d7200` | `AssetPaused()` | unexpected |
| `0x40753f33` | `ReserveNotActive()` | unexpected |
| `0xa5897d94` | `NotEnoughCollateralToLiquidate()` | sizing bug |
| `0x8bd79d6d` | `InvalidLiquidationAmount()` | sizing bug |
| `0xd9a162e8` | `InvalidLiquidationCallParams()` | calldata bug |

### Centralize the revert-decoder

Put one `lib/revert-decoder.js` per project; every chain's executor imports the same module. `expected: true` = silent skip (user healed / position healthy / race lost). `expected: false` = actionable alert worth Telegram-paging. New chains inherit the proven classification for free; noise stays out of operator chat.

## Live-RPC eth_call dry-run before first fire

Before broadcasting on a new chain, zero-cost validation:

1. Pick the closest-to-HF=1 borrower from the indexer
2. Build the exact liquidate() calldata (real pool discovery, real oracle prices, real debt chunk)
3. `provider.call({ to: liquidator, from: ownerAddr, data: calldata })`
4. **Expected**: `HealthFactorNotBelowThreshold()` revert — wires are correct, position healed naturally
5. **Any other revert** = real bug to fix before first fire

Cost: 0 wei. Risk: 0. Catches mismatches that anvil-fork tests don't:
- Live pool exists at the rediscovered tickSpacing
- Live oracle prices align with sizing math
- Live block state hasn't drifted from indexer snapshot
- All addresses (Pool, Oracle, hToken/vDebt, swap router) match live deployment

## Oracle-crash via `setAssetSources` (Aave V3 forks with adapter oracles)

Many Aave V3 forks (Tydro, Chaos Labs adapters) make oracle reversal painful — `getSourceOfAsset()` returns a feed ID, not a Chainlink aggregator address. Don't try to reverse the adapter; exploit AaveOracle's admin function:

1. `anvil_impersonateAccount(papOwner)` — find via `PoolAddressesProvider.getACLAdmin()` or mainnet `setAssetSources` history.
2. Deploy a constant-returning stub via `anvil_setCode`:
   ```
   bytecode = 0x7f<32-byte-price>60005260206000f3
   // PUSH32 price | PUSH1 0 | MSTORE | PUSH1 0x20 | PUSH1 0 | RETURN
   ```
   (41 bytes; works for `latestAnswer()`, `latestRoundData()`, any uint256-returning view.)
3. As impersonated admin: `oracle.setAssetSources([target], [stubAddr])`.
4. Verify `getAssetPrice(target)` returns crashed price; verify `Pool.getUserAccountData(borrower).healthFactor < 1`.
5. Use a **small debt chunk** (e.g., 1000 USDC), not `MaxUint256` — at deflated oracle prices, full seize amounts overflow any V3 pool's depth.

Sizing for the dry-run swap:
```
expectedSeize = debtChunk * LIF_BPS / 10000 * debtOraclePrice / collOraclePrice  // 1e8 base
amountIn = expectedSeize * 95 / 100  // 5% safety margin
```

## V4 OOG diagnosis via `debug_traceTransaction`

V4 swap reverts in mysterious ways → anvil's `debug_traceTransaction` with `callTracer` reveals the exact subcall:

```bash
curl -X POST http://localhost:8546 \
  -d '{"method":"debug_traceTransaction","params":["0xHASH",{"tracer":"callTracer"}],...}'
```

Walk the nested call tree for `"error": "out of gas"` or `"output": "0x08c379a0..."` (Error(string)). For V4, OOG inside PoolManager almost always means the swap tried to consume more than the pool's active liquidity at the current tick range → halve `amountIn` and retry. The OUTERMOST reverting call propagates the selector, but the ACTUAL origin is the deepest call with non-zero output.

## `getPool() != 0x0` does NOT mean the pool has liquidity

On any V3 (and V4 via factory-like wrappers), `factory.getPool(...)` returns non-zero for pools that were *created* but have *no LP positions*. Necessary, not sufficient. Truth lives in:
1. **Quoter**: `quoteExactInputSingle(...)` reverts with "Unexpected error" for any non-zero amount → pool is empty.
2. **Pool state**: `pool.liquidity() == 0` AND `token0.balanceOf(pool) == 0`.
3. **Tiny probe**: 10^14 wei for an 18-dec token should quote to something non-zero. If even that reverts, the pool is dead.

Monad 2026-05-22: Uniswap V3 has wstETH/WETH pools at fees 100/500/3000 — all three EMPTY despite the factory listing them. Liquidating the $16.5M wstETH/WETH whale cluster required Curve, not Uni V3. Without this check, a smoketest "passes" without revealing the lane can't actually fire.

## Don't route discovery through anvil fork

A common smoketest pitfall: `findBestPool(FORK_RPC, ...)`. The fork transparently fetches state from the upstream public RPC for every `eth_call`. Multi-step discovery (factory × N fee tiers + quoter × N pools) cascades into a flurry of upstream calls that all rate-limit together.

```js
// ❌ slow — discovery proxies every call to upstream public RPC
const pool = await swapPath.findBestPool(FORK_RPC, coll, loan, probe);

// ✅ fast — read-only discovery hits mainnet directly; fork only used for write/simulate
const pool = await swapPath.findBestPool(cfg.HTTP_RPC, coll, loan, probe);
```

Pools are the same address regardless; only the writes need the fork.

## What a good smoketest actually proves

A passing smoketest is NOT "the code runs without throwing." It's "this lane can capture the inventory it was built for." Three-step bar before going live:

1. ✅ Code paths execute end-to-end (basic).
2. ✅ Contract deployed correctly, calldata round-trips through anvil (basic).
3. ✅ **The lane atomically captures at least one real at-risk position** — Quoter returns non-zero amountOut for the seize size needed by the closest-to-HF=1 position. If step 3 fails, the lane is structurally blocked regardless of code quality. Don't flip live.

## Failure Modes (further additions)

**`getPool != 0x0` but quoter reverts → empty pool**
Factory returns non-zero for any pool ever created via `initialize`, including positionless placeholders. A naïve "pool found" check passes; the actual swap reverts on first fire. Always probe the Quoter with a tiny amount before trusting a pool.

**Slipstream/Ramses V3 SwapRouter bare-revert**
Mis-encoded selector because the ABI fragment said `uint24 fee` instead of `int24 tickSpacing`. The wire bytes look identical for small positive values, so the bug looks like a routing issue, not an ABI issue. Cross-check the selector (`0xa026383e` for tickSpacing routers) before blaming pool depth.

**V4 OOG inside PoolManager**
Means swap exceeded the pool's active liquidity at the current tick range, not "gas too low." Bumping gas limit doesn't help. Halve `amountIn`, retry; consider partial liquidation cascade if the position is whale-sized.

**Modern Aave V3 fork emits `0x930bb771` and decoder returns "unknown error"**
Legacy decoders match `/35|HEALTH_FACTOR/i` against the Error(string) message, which doesn't exist on modern forks. Result: every healed-position skip pages the operator. Pre-load custom error selectors in a centralized `revert-decoder.js`.

**`seizedAssets = MaxUint256` + off-chain swap calldata = mismatched amounts**
Morpho clamps internally, but the swap calldata was built with a specific `amountIn`. Once Morpho seizes less than the swap expects, swap's `transferFrom` fails. Compute `expectedSeized` explicitly (clamped at borrower collateral) and pass it both to Morpho AND to the swap encoder.

## Cross-chain revert decoder must handle 4 encodings

A multi-chain liquidator fleet sees revert payloads in four distinct formats. A single shared `lib/revert-decoder.js` must handle all of them:

1. **`0x` (empty bytes)** — out-of-gas or no revert reason. Skip-silently.
2. **`0x08c379a0...` — `Error(string)`**. ABI-decode the string: `abi.decode(data.slice(4), ['string'])`. Don't regex over the raw bytes — the encoded string has length prefix + padding that breaks substring matches.
3. **`0x4e487b71...` — `Panic(uint256)`**. `0x11` = arithmetic overflow, `0x21` = invalid enum, etc.
4. **`0x<4-byte-selector>...` — custom error**. Match selector against the union of all expected selectors across chains (Aave V3 modern, Morpho, contract-local `NoProfit`/`SwapFailed`, etc.).

Centralizing this in one module means new chains inherit the proven classification with zero copy-paste. Restart executors after each decoder update — long-lived Node processes hold the previous version in memory.

## Sibling Aave V3 forks can run different protocol versions

Don't assume two forks on the same chain emit the same errors. Tydro (modern, typed custom errors) and a legacy Aave V3 fork on the same chain can coexist; one fires `0x930bb771 HealthFactorNotBelowThreshold()`, the other fires `Error("35")`. A revert decoder MUST handle both classes — pre-load all known selectors AND the legacy `Error(string)` ABI-decode path.

## dRPC has stricter eth_call limits than QuickNode/publicnode

Empirically discovered on Monad: dRPC blocks or strictly throttles `eth_call` payloads above a small gas budget — fine for `eth_sendRawTransaction` but breaks pre-flight simulation. Pattern: use dRPC ONLY for broadcast/getLogs, use a public RPC (rpc.monad.xyz, etc.) or QuickNode for `eth_call` simulations and oracle reads. Mixing RPCs by call type is normal; one RPC isn't required to handle everything well.

## Morpho markets without DEX pools aren't bugs

A Morpho deployment can list a market for a token pair that has zero AMM liquidity on the same chain. The borrower's collateral is real and the position is liquidatable, but the seized asset can't be sold atomically → flashloan can't be repaid → no atomic capture. This is structural, not a code bug; alert on the size of skipped markets so a $X big-debt opportunity doesn't disappear into a silent skip. Same pattern as Silo V2's `sTokenRequired=true` — different protocol, identical "structurally unfireable" failure mode.

## Live-RPC dry-run before LIVE flip on every new chain

Mandatory pre-flight checklist for any new liquidator lane:

1. Pre-flight the closest-to-HF=1 position via `provider.call({ to: liquidator, from: ownerAddr, data: realCalldata })`.
2. Expected reverts (decode and classify): `HealthFactorNotBelowThreshold()` (healthy), `NoProfit()` (uneconomic), `Error("35")` (legacy healthy), `0x` empty (OOG).
3. ANY other revert is a real bug — fix before first fire.
4. Stale indexer is normal — even a 5-min-old `positions.json` is enough for the dry-run; the live read inside the contract uses fresh chain state.

This catches mismatches that anvil-fork tests don't: live pool depth, live oracle alignment, live address sanity. Cost: 0 wei, risk: 0.
