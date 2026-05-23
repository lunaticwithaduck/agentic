---
title: URGENT — fix Felix decimals bug (would skip the $286k whale at fire)
created: 2026-05-21
completed: 2026-05-21
status: done
---

## Goal
Felix monitor copied from Bend assumed 18-decimal loan token (HONEY). Felix has mixed 6/18-decimal loan tokens. The 6-decimal cases (USDC, USDT0) computed `debtUsd = borrowed / 1e18` → underflowed MIN_DEBT_USD = $20 → **silently skipped firing** even when HF crossed 1.0.

## Steps
- [x] Added `loadTokenDecimals(tokenAddr)` with lazy cache + pre-warm from positions.json at startup
- [x] Added `loanDecimals` + `loanToken` to result struct in `loadAndSweep`
- [x] Fixed verbose display: `borrowed / 10**loanD` instead of `/1e18`
- [x] Fixed alert filter: `debtUsd = ... / (10**loanD)` instead of `/1e18`
- [x] Restarted Felix monitor

## Verification
Before fix: `0x24df4b7af6` shown as `HF 1.007 debt $0` (silently dropped from MIN_DEBT filter)
After fix: `0x24df4b7af6` shown as `HF 1.007 debt 286111 (6-dec)` + `🎯 arm 0x8eecdd03:0x24df4b7af6 HF 1.0069`

The position is now correctly armed and will FIRE if HF crosses below 1.0.

## Severity
**Critical for live operation.** Bug would have caused us to silently miss the biggest near-term Felix fire opportunity ($286k debt, $7k+ gross profit) plus 4 other large USD₮0 positions in the same market.

## Completion
Run `/complete workflows/tasks/2026-05-21-felix-decimals-bug-fix.md`.
