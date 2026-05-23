---
domain: hyperevm
source_task: 2026-05-20-decode-hyperlend-revert.md
date: 2026-05-20
keywords: ["aave-v3", "custom-errors", "revert-decoding", "hyperlend", "hyperevm", "selector"]
---

## Extracted Knowledge

### HyperLend Aave V3 fork uses CUSTOM ERRORS, not string codes
Canonical Aave V3 reverts with `require(cond, "45")` → `Error(string)` → `0x08c379a0` + ABI-encoded "45". HyperLend instead uses Solidity custom errors:
```solidity
error HealthFactorNotBelowThreshold();
revert HealthFactorNotBelowThreshold();
```
Decodes to a 4-byte selector. **`0x930bb771 == keccak256("HealthFactorNotBelowThreshold()").slice(0,4)`**.

### Confirmed HyperLend / HypurrFi selectors
| Selector | Signature | Treat as |
|---|---|---|
| `0x930bb771` | `HealthFactorNotBelowThreshold()` | ✅ expected — silent skip |
| `0x6679996d` | `HealthFactorLowerThanLiquidationThreshold()` | ✅ expected |
| `0x6d305815` | `ReserveFrozen()` | ⚠️ alert |
| `0xd37f5f1c` | `ReservePaused()` | ⚠️ alert |
| `0x823d7200` | `AssetPaused()` | ⚠️ alert |
| `0x40753f33` | `ReserveNotActive()` | ⚠️ alert |
| `0x911ceb81` | `CollateralCannotCoverNewBorrow()` | ⚠️ alert |
| `0xa5897d94` | `NotEnoughCollateralToLiquidate()` | ⚠️ alert |

### Brute-force decoding pattern
When you have an unknown 4-byte selector and suspect Aave V3 origin:
1. Make a list of every error name in Aave V3 ErrorsLibrary (~90 entries) + common fork variants
2. Compute `ethers.id(sig).slice(0,10)` for each
3. Compare to target
4. Try variants: `HealthFactorNotBelowThreshold()`, `HEALTH_FACTOR_NOT_BELOW_THRESHOLD()`, `HealthFactorLowerThanLiquidationThreshold()` — forks rename
5. Most matches happen within the first 30-50 names when sorted by frequency in HF / liquidation logic

### ethers v6 revert data extraction
Revert data lives in different places depending on the call path:
- `provider.call(...)` throws `CallException` → data on `err.data` or `err.info.error.data`
- `provider.estimateGas(...)` throws differently — wraps in `err.error.data`
- Raw JSON-RPC errors have `err.data` at the top level
Use a multi-path resolver: `err.data ?? err.error?.data ?? err.info?.error?.data`.

### Filter expected reverts at the call site
Race-tier liquidators see "user healed" hundreds of times per day across all monitored positions. Without classification, every pre-flight revert spams Telegram. Solution: decode → check `expected` flag → log silently if true, alert if false. Saves real anomalies (paused reserves, swap failures) from being drowned in noise.

## Proposed Skill Content
Add to `hyperevm` skill under "Revert decoding":
- HyperLend uses custom errors, HypurrFi uses string codes — handle both encodings
- Reusable decoder pattern with `expected` flag per signature
- Selector table is short (14 entries cover 95%+ of observed reverts) — embed as constants, no on-chain ABI fetch needed
