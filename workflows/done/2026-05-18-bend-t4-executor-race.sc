---
domain: defi-liquidations
source_task: 2026-05-18-bend-t4-executor-race.md
date: 2026-05-18
keywords: ["kodiak", "uniswap v3", "exactInputSingle", "swap router", "quoter", "executor", "race"]
---

## Extracted Knowledge

### Kodiak v3 (Berachain native DEX) addresses
Kodiak Finance is the deepest concentrated-liquidity DEX on Berachain. Berachain-mainnet addresses:
- SwapRouter02:    `0xe301E48F77963D3F7DbD2a4796962Bd7f3867Fb4` (no-deadline, modern style)
- SwapRouter (v3): `0xEd158C4b336A6FCb5B193A5570e3a571f6cbe690` (with-deadline, legacy)
- QuoterV2:        `0x644C8D6E501f7C994B74F5ceA96abe65d0BA662B`
- V3 Factory:      `0xD84CBf0B02636E7f53dB9E5e45A616E05d710990`
- kXRouter:        `0x43Dac637c4383f91B4368041E7A8687da3806Cae` (smart aggregator used by app.kodiak.finance)
- kXExecutor:      `0xEB109d3935eA00B90b6eBe56e4606a1CdacF0b98`

Default fee tiers: `[100, 500, 3000, 10000]` bps (0.01%, 0.05%, 0.3%, 1%).

For a liquidator contract, use SwapRouter02 (newer, no deadline) over SwapRouter unless you need multi-hop. ABI:

```solidity
struct ExactInputSingleParams {
    address tokenIn;
    address tokenOut;
    uint24 fee;
    address recipient;
    uint256 amountIn;
    uint256 amountOutMinimum;
    uint160 sqrtPriceLimitX96;  // 0 = no limit
}
function exactInputSingle(ExactInputSingleParams) external payable returns (uint256 amountOut);
```

### Pool discovery — iterate fee tiers, prefer deepest
For a token pair, query `V3Factory.getPool(tokenA, tokenB, fee)` across all fee tiers. Non-zero address means a pool exists at that fee. Then quote a small probe amount through each existing pool — the highest amountOut wins (= deepest pool / least price impact).

Empirically on Bend markets: WBTC/HONEY at 0.30%, sUSDe/HONEY at 0.05%, WETH/HONEY at 0.30%, WBERA/HONEY at 0.01%. Fee tier varies per pair based on volatility and where liquidity providers find it economical.

### Oracle scale gotcha — always re-derive before theorizing
Morpho oracles return price scaled to `10^(36 + loanDecimals - collateralDecimals)`. For WBTC (8 dec) collateral + HONEY (18 dec) loan, scale is `10^46`. The raw uint256 reading divided by `10^46` gives loan-token-units per collateral-token-unit.

Common mistake: read the raw number, mentally divide by 1e36 (the "ORACLE_SCALE" constant from Morpho source), and get a misleading answer for non-18-dec tokens. The 1e36 in Morpho's HF formula already handles the cross-decimal adjustment when the collateral amount is in raw units.

If on-chain numbers don't match expected market price, recompute from raw values FIRST before theorizing about depegs, mispricing, or pool issues. Real example from this session: I computed WBTC oracle as $99k vs Kodiak's $76k, claimed a 23% "gap" implying HONEY depeg. Reality: my scale arithmetic was wrong, prices match within 1%, HONEY is pegged at $0.9992.

### Bridged-asset price gap is normal
Berachain's bridged WBTC (`0x0555E30...`) trades at ~$76k vs mainnet BTC ~$99k — a ~23% discount. This is NORMAL for bridge-wrapped assets and reflects:
- Bridge redemption friction (can't trivially convert back to mainnet WBTC)
- Lower depth in the destination market
- Insurance discount against bridge hack risk
- Lack of cross-chain arb infrastructure for niche pairs

The discount is structural, not arbitrage-able. Bend's own oracle correctly tracks the bridged-asset price (not mainnet BTC), so HF math is consistent. Lesson: a "weird" price gap might be a real bridged-asset discount, not a bug.

### Multi-process IPC via filesystem
Monitor (t2) writes `armed/<positionKey>.json` (atomic temp+rename); executor (t4) reads them. `fs.watch(armedDir, callback)` fires on file create/delete. Simple, no message bus needed, works across systemd-restart boundaries.

Initial scan on executor startup catches files written while executor was down. The full cycle is:
1. Monitor sees HF < threshold → writes `armed/<key>.json`
2. Executor fs.watch fires → reads payload → pre-signs tx
3. WSS block subscription on executor → re-evaluates HF on each block
4. If HF crosses fire threshold → broadcast via dual-RPC race
5. Confirmed → delete armed file
6. If HF recovers and stays high for 2min → drop armed file (position safe again)

### Pre-sign + dual-RPC race pattern (proven on MIBERA loan-132)
Pre-sign the liquidation tx as soon as a position arms — store the signed raw bytes in memory. When the trigger fires:

```js
Promise.any([
  fetch(publicnode_rpc, { ... eth_sendRawTransaction ... }),
  fetch(official_rpc,   { ... eth_sendRawTransaction ... }),
])
```

First RPC to ack wins; the other's tx becomes a no-op nonce collision. Both broadcast simultaneously to maximize landing in the same block as the trigger.

Wall-clock fallback timer (2 min default) drops armed positions if HF recovers — frees the signed tx slot, avoids broadcasting a stale tx into a healthy position.

### Slippage protection for Kodiak swaps
`amountOutMinimum = quote × (10000 - SLIPPAGE_BPS) / 10000`. With SLIPPAGE_BPS=50 (0.5%), we accept up to 0.5% worse than the quote. If actual execution swap delivers below this, SwapRouter02 reverts → the whole liquidate-and-swap composite reverts via the contract's `minProfitWei` check → we lose only gas.

Two-layer profitability gate:
1. Off-chain: executor refuses to fire if Kodiak-derived expected output < flash loan repay + buffer
2. On-chain: contract reverts if final HONEY balance < flashloan + minProfitWei

Defense in depth — one layer can be miscomputed without losing real money.

## Proposed Skill Content

Already covered in `.claude/skills/defi-liquidations.md`. This `.sc` adds the Kodiak-specific addresses + the oracle-scale gotcha + the bridged-asset price expectation.
