---
title: Production E2E smoke test for Tydro lane on anvil fork (Ink)
created: 2026-05-22
completed: 2026-05-22
status: done — caught 2 real production bugs, fixed both
---

## Result: caught 2 PRODUCTION bugs that would have hit on first real fire

### Bug 1: Swap-path imported HYPEREVM_DEXES instead of INK_DEXES
The sed-based rebrand of HyperLend → Tydro missed the `require('../lib/hyperevm-dexes')` line. Production code iterated HyperEVM's project-x + hyperswap DEXes against the Ink chain → all factory.getPool calls returned 0x0 → swap-path returned null → executor would skip every fire silently.

**Fix**: `const { INK_DEXES: HYPEREVM_DEXES } = require('../lib/ink-dexes');` (alias-import for code reuse with hyperlend-derived swap-path logic)

### Bug 2: spotQuote crashed on Velodrome Slipstream's slot0() ABI
The hyperlend swap-path's `spotQuote()` expected canonical Uni V3's slot0 struct (7 fields). Slipstream returns a different struct (fewer fields / different layout) → ethers decode threw → spotQuote returned 0n → all candidates filtered out → null returned even when the Slipstream pool clearly exists.

**Fix**: defensive spotQuote with fallback that parses raw bytes — sqrtPriceX96 is always the first 32 bytes regardless of struct layout. Allows the spot-quote math to proceed.

### Bonus discovery: tickSpacing set
Slipstream uses `[1, 50, 100, 200, 2000]` as the pool key (not canonical V3's [100, 500, 3000, 10000]). For USDT0/USDC, the pool is at tickSpacing **1**. Now in `lib/ink-dexes.js`.

## Verification
After fixes:
- `swap-path.findBestPool` returns Slipstream USDT0/USDC pool at fee=1, expectedOut ≈ 999,206 USDC per 1e6 USDT0 (correct 1:1 stable swap)
- Full liquidate() call chain executes 11 levels deep through Pool → flashLoan → callback → liquidationCall → ... (reaches Aave V3 protocol correctly)
- Reverts deep in the chain because position is healthy (HF 1.049) — the expected behavior for healthy-mode smoke

## What was NOT verified (still trust-but-verify on first real fire)
- Oracle-crash path → forced HF<1 → actual liquidate succeeds. Blocked because Tydro uses Chaos Labs oracle adapter that doesn't expose standard Chainlink-source feeds via `getSourceOfAsset()` (returns a feed ID, not a contract). Solving this requires deeper reverse-engineering of Tydro's oracle. Not done tonight; defer to first-real-fire validation.
- Actual collateral seize + swap settle. Same reason — needs forced HF<1 to test.

## Trade-off accepted
- Production code reuses HyperLend's flashloan+liquidate logic (proven live on HyperEVM since May 2026)
- Slipstream's swap-path is a thin wrapper over Sonic's tickSpacing-based Shadow handling (proven live on Sonic)
- Healthy-mode smoke confirms the code path reaches Pool.liquidationCall
- First real fire on Tydro WILL be the production validation — bounded downside ($0.005 gas if it reverts)

## Files modified
- `lib/ink-dexes.js`: renamed `tickSpacings` → `feeTiers` for compat with hyperevm swap-path code
- `tydro/swap-path.js`: aliased import `INK_DEXES: HYPEREVM_DEXES`; defensive `spotQuote` with raw-bytes fallback
- `tydro/healthy-smoketest.js`: NEW — runs the code path end-to-end on anvil, expects HF-guard revert

## Completion
Run `/complete workflows/tasks/2026-05-22-tydro-prod-smoketest.md`.
