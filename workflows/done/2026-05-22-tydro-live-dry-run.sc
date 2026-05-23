---
domain: defi-liquidations
source_task: 2026-05-22-tydro-live-dry-run.md
date: 2026-05-22
keywords: [live-dryrun, eth_call, aave-v3-custom-errors, healthfactornotbelowthreshold, 0x930bb771, revert-decoder]
---

## Extracted Knowledge

### Modern Aave V3 forks emit typed custom errors, not legacy `revert(Errors.code)`
- Legacy Aave V3: `revert(Errors.HEALTH_FACTOR_NOT_BELOW_THRESHOLD)` → `Error("35")` with selector `0x08c379a0`
- Modern Aave V3 (Tydro, recent forks): `revert HealthFactorNotBelowThreshold()` → custom error selector `0x930bb771`
- Both forms are still in the wild — code your revert decoder to handle both
- The legacy `/35|HEALTH_FACTOR/i` string check matches nothing on modern forks → false "unknown error" alerts in production
- Other modern Aave V3 selectors worth pre-loading in decoder:
  - `0x930bb771` HealthFactorNotBelowThreshold() — healthy-position skip (expected)
  - `0x6679996d` HealthFactorLowerThanLiquidationThreshold() — also healthy-position skip
  - `0x6d305815` ReserveFrozen()
  - `0xd37f5f1c` ReservePaused()
  - `0x823d7200` AssetPaused()
  - `0x40753f33` ReserveNotActive()
  - `0xa5897d94` NotEnoughCollateralToLiquidate()
  - `0x8bd79d6d` InvalidLiquidationAmount()
  - `0xd9a162e8` InvalidLiquidationCallParams()

### Live-RPC eth_call dry-run pattern
Before broadcasting on a new chain, run a zero-cost validation:
1. Pick the closest-to-HF=1 borrower from the indexer
2. Build the exact liquidate() calldata you'd broadcast (real Slipstream pool discovery, real oracle prices, real debt chunk)
3. `provider.call({ to: liquidator, from: ownerAddr, data: calldata })`
4. Expect `HealthFactorNotBelowThreshold()` revert — confirms wire-correctness
5. ANY other revert is a real bug to fix before first fire

What this catches that anvil-fork tests don't:
- Live Slipstream pool exists at the rediscovered tickSpacing
- Live oracle prices align with what your sizing math assumes
- Live block state hasn't drifted from your indexer snapshot
- All addresses (Pool, Oracle, hToken/vDebt, swap router) match live deployment

Cost: 0 wei. Risk: 0. Confidence: very high before first real fire.

### revert-decoder is the single source of truth for "expected vs actionable"
Centralize at `lib/revert-decoder.js`. Each chain's executor imports the same
decoder. The `expected: true` flag means "user healed / position is healthy /
race lost — silent skip, no Telegram." `expected: false` means "actionable
anomaly worth alerting." This keeps noise out of operator's Telegram and lets
new chains inherit the proven classification for free.

## Proposed Skill Content

Add to `.claude/skills/defi-liquidations.md`:

### Aave V3 custom-error selectors to pre-load
Modern Aave V3 forks (Tydro, post-2024 deploys) revert with typed custom errors
not legacy `revert("35")` strings. Decode by 4-byte selector:
- `0x930bb771` HealthFactorNotBelowThreshold()
- `0x6679996d` HealthFactorLowerThanLiquidationThreshold()
- Pre-load these in your revert-decoder marked `expected: true` so healthy-position
  pre-flights don't spam Telegram.

### Live-RPC eth_call dry-run before first fire
Validate every new lane on the live chain RPC via eth_call simulation against
a real near-HF=1 borrower. Cost 0. Expect HealthFactor revert. Any other revert
is a bug. Catches wire-format issues that anvil can't (oracle drift, address
mismatch, real pool state).
