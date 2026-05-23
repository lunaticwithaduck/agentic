---
domain: hyperevm
source_task: 2026-05-20-per-asset-lif-discovery.md
date: 2026-05-20
keywords: ["aave-v3", "liquidation-bonus", "LIF", "reserve-configuration", "bitmap", "hyperlend", "hypurrfi"]
---

## Extracted Knowledge

### Aave V3 ReserveConfiguration bitmap
`Pool.getConfiguration(asset) returns (uint256)` — packed config. Layout (bit positions):
- 0-15:   LTV (basis points, e.g. 8000 = 80%)
- 16-31:  liquidation threshold (basis points)
- **32-47: liquidation bonus** (basis points; 10500 = 5% bonus, i.e. LIF = 1.05)
- 48-55:  decimals
- 56:     active
- 57:     frozen
- 58:     borrowing enabled
- 59:     stable borrowing enabled (deprecated v3)
- 60:     paused
- 61:     borrow-able in isolation
- 62:     siloed
- 63:     flashloan-able
- 64-79:  reserve factor
- 80-115: borrow cap
- 116-151: supply cap
- 152-167: liquidation protocol fee
- ...

### Extraction code
```js
const config = BigInt(await pool.getConfiguration(asset));
const lifBps = Number((config >> 32n) & ((1n << 16n) - 1n));
// lifBps of 11500 = 1.15 LIF = 15% bonus
```

### Real values are NOT 5% — fork-specific
Aave's canonical mainnet defaults to 5%. Forks customize per asset:
- HyperLend: WHYPE/kHYPE 10%, wstHYPE/UETH 15%, UBTC 20%, stables 8%
- HypurrFi: native-like 12%, everything else 8%

**Don't hardcode 5% in liquidator math.** Read the bitmap once at startup, cache as `{ assetAddrLowercase: bps }`.

### bonus=0 means asset is debt-only
If `lifBps === 0`, the asset cannot be used as collateral. Executor should never select it as collateral. Defensive guard: reject the presign rather than pass a 0 to downstream math (would produce zero seized → division-by-zero somewhere).

### Why this matters for estimate-and-cap swap-path math
Liquidator computes `seizedValue = debtValue × LIF`. Under-estimating LIF means under-sizing `swapAmountIn` → leaves more dust → less gas-efficient. Over-estimating LIF would be dangerous (swap reverts if collateral doesn't reach the encoded amount). So:
- Use real LIF when known
- Fallback to a conservative default (10500 = 5%) when unknown — under-estimate is safe
- NEVER use a default higher than the chain's minimum observed LIF

## Proposed Skill Content
Add to `hyperevm` skill under "Aave V3 fork patterns":
- LIQUIDATION_BONUS is per-asset, not chain-wide; bitmap bits 32-47 of `getConfiguration`
- HyperLend ranges 10800-12000; HypurrFi ranges 10800-11200; canonical Aave is 10500
- Bonus=0 means debt-only; reject as collateral candidate
- Cache the table at executor startup; refresh hourly via the monitor (rare changes but possible after admin tx)
