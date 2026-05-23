---
domain: defi-liquidations
source_task: 2026-05-18-bend-t2-monitor-healthfactor.md
date: 2026-05-18
keywords: ["health-factor", "morpho", "oracle", "lltv", "bigint", "liquidation-monitor"]
---

## Extracted Knowledge

### Morpho health-factor formula (verbatim)
```
maxBorrowValue = collateral × oraclePrice × lltv / (1e36 × 1e18)
borrowAssets   = (borrowShares × totalBorrowAssets + totalBorrowShares - 1) / totalBorrowShares  // round up
HF             = maxBorrowValue / borrowAssets    // >= 1 healthy, < 1 liquidatable
```

The position is liquidatable when `maxBorrowValue < borrowAssets`. Morpho rounds borrowAssets UP for the liquidation check (worst case for the borrower), so the bot must match that to compute the same HF on-chain.

### Morpho oracle price scale convention
Oracle returns price as `uint256` scaled to `10^(36 + loanDecimals - collateralDecimals)`. The 1e36 factor in the HF denominator cancels both the oracle scale AND the differing token decimals automatically. So:

- WBTC (8 dec) collateral + HONEY (18 dec) loan → oracle scales by `10^(36+18-8) = 10^46`
- WETH (18 dec) + HONEY (18 dec) → oracle scales by `10^36`

You don't need to know the scale — just divide by 1e36 in the formula. The oracle's internal scaling handles the rest.

### Standard Morpho oracle interface
```solidity
interface IOracle {
    function price() external view returns (uint256);
}
```
Selector: `ethers.id('price()').slice(0, 10)` → `0xaabbc0ed...`-style 4-byte. No parameters, no decimals call required.

### BigInt-safe HF ratio in JavaScript
Naive `Number(maxBorrow) / Number(borrowAssets)` overflows for large positions. Pattern:

```js
const HF = Number((maxBorrow * 10000n) / borrowAssets) / 10000;  // 4 decimals precision
```

Multiply numerator by 10^N first to preserve precision after the integer division, then convert to Number.

### Per-block oracle re-reads (architecture)
Oracle prices can move every block (Redstone push updates frequently; Chainlink updates on threshold). The right loop:

1. WSS `newHeads` subscription = trigger
2. On each new block: re-read ALL market oracles in parallel (`Promise.all`)
3. Recompute HF for every position (or top-N closest if many positions)
4. Compare new HF against thresholds; alert on threshold crossings

At ~22 positions and 7 markets on Berachain, each sweep takes ~250ms (mostly RPC latency, not compute). Scales linearly — at 200 positions still well under 1 block time.

### Multi-tier alert thresholds with dedup
A useful threshold layout:
- `WARN = 1.10` — early heads-up, executor doesn't act
- `ARM = 1.02`  — executor should pre-sign now
- `FIRE = 1.00` — executor must broadcast immediately

Dedup per (positionKey, threshold) tuple with 5-minute TTL. Without dedup, every block-sweep retriggers the alert. Without the (position, threshold) key, crossing from WARN → ARM → FIRE squashes the FIRE alert because dedup thinks it's a repeat.

### Min-debt filter for economic relevance
LIF × debt gives gross profit potential. If 15% × debt < gas + flash loan overhead, the alert is useless noise. Filter:

```js
if (debtUsd < MIN_DEBT_USD) continue;  // skip dust positions for alerts but keep tracking
```

On Berachain at $0.0004/tx gas, $20 minimum is generous. On chains with $5+ gas, raise it.

### Cross-process IPC via `armed/` directory
Pattern: monitor writes `armed/<positionKey>.json` when HF < FIRE. Executor watches that directory (file-watch or polling) and consumes. Clean separation of concerns — monitor has no idea about pre-signing, executor has no idea about HF math.

Atomic write via temp+rename so the executor never reads a partial file.

## Proposed Skill Content

Adds to the `defi-liquidations` skill (third `.sc` in this domain):

**Section: Morpho HF computation in code**
- Full formula with worked example
- 1e36 oracle scale convention and why decimals cancel automatically
- Shares-to-assets round-up convention

**Section: BigInt patterns for liquidation math**
- Why naive Number(a)/Number(b) overflows
- The × 10000n / b pattern for ratio extraction

**Section: Monitor architecture**
- WSS-driven per-block re-evaluation
- Multi-tier thresholds with dedup tuple
- Min-debt economic filter
- File-based IPC to executor via armed/ directory
