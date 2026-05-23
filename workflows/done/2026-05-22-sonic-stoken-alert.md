---
title: Add Telegram alert for big sTokenRequired skips on Sonic
created: 2026-05-22
completed: 2026-05-22
status: done
---

## Goal
Sonic executor was silently skipping fires when Silo V2 reports `sTokenRequired=true` (atomically uncapturable due to silo at 100% utilization). Add a Telegram alert for the cases worth manually intervening on (≥$100 expected profit), without engineering the full multi-tx hold-and-redeem path for small ones.

## Steps taken
- [x] Added cached `getWSPrice()` helper (CoinGecko Demo, 1h TTL, $0.05 fallback if API fails)
- [x] Added `stokenAlerted` Map with 1h dedup keyed on `(silo, borrower)` so re-arms don't spam
- [x] In the sTokenRequired skip path: compute expectedProfit in USD, send Telegram if ≥ $100
- [x] Syntax check, dry-validate against current $8.55 fire (correctly does NOT alert), restart service

## Verification
- Service restarted at 14:02:34 EEST (11:02:34 UTC), confirmed active
- Logic dry-run: 178.05 wS × $0.048 = $8.55 → below $100 → no alert (correct)
- A future 10× position (~$100+ expected profit) will trigger the Telegram and we can manually evaluate the multi-tx capture path

## Outcome
Completed 2026-05-22. Sonic's silent-skip blind spot for big sToken-blocked fires is now lit. Logic is conservative (only the big ones, deduped per-borrower per-hour) and best-effort (price-fetch failure doesn't block the skip — only suppresses the alert).

## Completion
Run `/complete workflows/tasks/2026-05-22-sonic-stoken-alert.md`.
