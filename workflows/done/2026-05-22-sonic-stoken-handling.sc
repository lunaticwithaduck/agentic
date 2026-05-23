---
domain: defi-liquidations
source_task: 2026-05-22-sonic-stoken-handling.md
date: 2026-05-22
keywords: ["silo-v2", "stoken", "atomic-liquidation", "100-percent-utilization", "erc4626", "redeem-fails", "structural-limit"]
---

## Extracted Knowledge

### Silo V2's `sTokenRequired=true` is a STRUCTURAL block, not a code issue
When SiloLens.maxLiquidation returns `sTokenRequired=true`, the protocol is saying: "the collateral silo's liquidity is too low to give you the underlying — you must take sToken (ERC4626 share) instead." A liquidator can ACCEPT the sToken, but **redeeming it back to underlying in the same transaction can still revert** if the silo is at high utilization. Two separate constraints:

1. **`liquidationCall(_receiveSToken=true)`** — protocol lets you take the sToken. ✓ Works.
2. **`collateralSilo.redeem(shares, you, you)`** — converts sToken back to underlying. Requires `getLiquidity() > 0`. **Often fails** in the same tx when sTokenRequired was set.

The flashloan repay needs underlying debt-asset. If redeem fails AND no DEX pool exists for the sToken, the tx must hold the sToken — which means the flashloan can't be repaid → tx reverts. **Atomic liquidation is impossible in this state.**

### How to diagnose if a Silo V2 fire is atomically fireable
Before attempting `receiveSToken=true`, check the collateral silo's:
- `getLiquidity()` — returns 0 if no withdrawals allowed
- `getCollateralAssets()` / `getDebtAssets()` — utilization = debt/coll. 100%+ means no liquidity
- Physical underlying balance (`underlying.balanceOf(silo)`) — necessary but not sufficient (protocol invariants can lock it)
- Whether a DEX pool exists for the sToken address itself (CoinGecko `/onchain/networks/{slug}/tokens/{stokenAddress}/pools`)

If none of these allow conversion → skip and log. The fire is uneconomical to capture atomically.

### The multi-tx hold-and-redeem alternative (not always worth it)
For positions worth pursuing despite sTokenRequired:
1. Replace flashloan with **own-capital funding** (you need the debt asset in your wallet)
2. Call `liquidationCall(_receiveSToken=true)` with your own capital
3. **Hold the sToken** (you're now exposed to the silo's risk)
4. Monitor `siloCollateral.getLiquidity()` and call `redeem` when it goes positive
5. Swap underlying → debt asset to recover (and bank profit)

Trade-offs:
- Capital lockup (your wallet's debt-asset for duration)
- Silo risk exposure (if the silo gets bad-debt'd, you eat it)
- Multi-day or multi-week redeem timing (no SLA)
- Only worth it if profit > $X×capital cost (rule of thumb: 5-10× the gas + capital opportunity cost)

For sub-$100 profit fires, **skip is correct**.

### Silo V2 quick reference (per-silo views)
```js
// Liquidity check before assuming a redeem will succeed
const silo = new Contract(siloAddr, [
  "function getLiquidity() view returns (uint256)",       // canonical "redeemable now"
  "function getCollateralAssets() view returns (uint256)", // total deposited
  "function getDebtAssets() view returns (uint256)",       // total borrowed
  "function totalAssets() view returns (uint256)",         // ERC4626 vault total
  "function previewRedeem(uint256 shares) view returns (uint256)",  // theoretical share-to-asset
  "function asset() view returns (address)",               // underlying
]);
```

`previewRedeem` returns the THEORETICAL exchange rate (shares × pricePerShare) — it does NOT account for liquidity constraints. It can return a non-zero value even when `getLiquidity() = 0`. Don't trust it as a liquidity check.

### The detection-vs-action gap (worth alerting)
Silently skipping all sTokenRequired fires means we don't notice when a BIG one happens. For each Silo V2 lane, add a Telegram alert when `sTokenRequired=true` AND `expectedProfit > $threshold` — that's the signal to manually evaluate the multi-tx hold-and-redeem path. Cost: 1 log line per skip; benefit: don't miss a $5k fire because we coded a silent skip.

## Proposed Skill Content

Extend `defi-liquidations` with a "Silo V2 sToken model" section:

- **`sTokenRequired=true` is a hard structural block for atomic flashloan liquidation.** The contract can accept the sToken, but the in-tx redeem will fail when silo utilization is 100%.
- **Don't waste cycles trying to "fix" the executor** when the chain state is the blocker. The contract code is fine; the silo's liquidity is the issue.
- **For atomic fires**: check `getLiquidity() > 0` AND a sToken-trading DEX pool exists BEFORE assuming you can fire.
- **For non-atomic fires**: requires own-capital funding (no flashloan), multi-tx tracking, and timing for the eventual redeem. Only worth implementing if expected profit > 10× capital opportunity cost.
- **Always alert on big skipped opportunities** — silent skip = blind. Threshold the alert at a meaningful profit floor.
