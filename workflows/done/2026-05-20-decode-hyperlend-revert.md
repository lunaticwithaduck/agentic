---
title: Decode the HyperLend custom revert selector 0x930bb771
created: 2026-05-20
completed: 2026-05-20
status: done
---

## Goal
HyperLend's Pool reverts with custom selector `0x930bb771` on healthy users instead of Aave's string error code "45". We assumed this was `HEALTH_FACTOR_NOT_BELOW_THRESHOLD` but never confirmed. Risk: if it was actually a different Aave error (reserve paused, frozen, liquidator blocked), our null-return path would silently swallow forever.

## Steps
- [x] Brute-force search Aave V3 error roster for matching selector
- [x] Confirmed: `0x930bb771 = HealthFactorNotBelowThreshold()` — exactly the assumption
- [x] Built `/home/jojo/automation/lib/revert-decoder.js` with 14 known selectors + 4 string codes; classifies each as `expected:true` (silent skip) or `expected:false` (Telegram alert)
- [x] Wired into both executors' pre-flight error handler — expected reverts log a "✓" line; unknown/actionable reverts still fire Telegram
- [x] Lint + restart both executors

## Outcome
Completed 2026-05-20. `0x930bb771` decoded to `HealthFactorNotBelowThreshold()`. New revert decoder handles both encodings (HyperLend custom selectors and HypurrFi `revert("45")` strings) and 14 known signatures.

**Behavior change:**
- Pre-flight reverts for HF-healed users now log `✓ pre-flight: HealthFactorNotBelowThreshold() — skipping silently` — no Telegram noise
- Reserve-paused / frozen / asset-paused / NoProfit / SwapFailed all still alert via Telegram with the decoded signature for easier diagnosis
- Unknown selectors are still alerted (`unknown-selector`) so we can add them to the table when we see them

## Completion
Run `/complete workflows/tasks/2026-05-20-decode-hyperlend-revert.md`.
