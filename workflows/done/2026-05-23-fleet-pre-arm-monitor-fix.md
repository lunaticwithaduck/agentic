---
title: Ship pre-arm armed-file write across fleet (was: log-only at cliff edge)
created: 2026-05-23
completed: 2026-05-23
status: done
---

## Goal
Yesterday's $4M Sonic cliff (0xbf5b0bc2, 13min at LTV=LT) wasn't captured because
the monitor only wrote armed files when STRICTLY underwater (`!solvent`/HF<1.0).
Cliff-edge oscillating positions briefly cross underwater between sweeps but get
marked solvent on each sweep tick → no armed file → executor never even attempts
to fire. Audit all chains and patch.

## Audit findings
| Chain | Active monitor | Pre-fix behavior | Status |
|---|---|---|---|
| Felix | monitor-wss.js | Already writes at HF<1.02 | OK |
| Monad | monitor-wss.js | Already writes at HF<1.02 | OK |
| Tydro | monitor-wss.js | Already writes with isFire flag | OK |
| HyperLend | monitor-wss.js | Only writes at HF<1.0 | **PATCHED** |
| HypurrFi | monitor-wss.js | Only writes at HF<1.0 | **PATCHED** |
| Bend | monitor.js | Only writes at HF<FIRE; arm zone log-only | **PATCHED** |
| Sonic-silo | monitor.js | Only writes at !solvent; arm zone log-only | **PATCHED** (earlier today) |

## Patches shipped

### `sonic-silo/monitor.js`
Arm-zone branch (`else if (r.ltvRatio >= ARM_THRESHOLD_LTV_RATIO)`) now also writes
the armed file with `fire: false` flag, so executor pre-signs and polls maxLiquidation
each tick.

### `hyperlend/monitor-wss.js` + `hypurrfi/monitor-wss.js`
- Added `ARM_THRESHOLD_HF = 1.02e18`
- Changed gate from `HF >= FIRE_THRESHOLD_HF (1.0)` to `HF >= ARM_THRESHOLD_HF (1.02)`
- Tag armed payload with `fire: HF<1.0` flag
- Log tag dynamically ("🎯 arm" vs "🔥 FIRE")
- Telegram alert only on fire transitions (silent on pre-arm)

### `bend/monitor.js`
Arm-zone branch now writes armed file with `fire: false`, mirroring sonic-silo
pattern. Telegram still suppressed for arm zone (executor handles all action alerts).

## Restart verification (all green)
- sonic-silo-monitor: active
- sonic-silo-executor: active  (includes partial-fire-fix from earlier today)
- hyperlend-monitor-wss: active
- hypurrfi-monitor-wss: active
- bend-monitor: active
- felix-monitor-wss: active (unchanged)
- monad-monitor-wss: active (unchanged)
- tydro-monitor-wss: active (unchanged)

## Expected behavior change
For each chain, positions oscillating at the cliff edge (HF or LTV ratio near
threshold) will now have an armed file present BEFORE they cross underwater.
Executor will poll maxLiquidation each tick and fire the moment the position
crosses. Combined with sonic-silo's partial-fire-within-liquidity fix from this
morning, this captures the previously-lost $1.8-4.7k/fire opportunity on Sonic
and similar cliff-edge events on the other 3 chains.

## Risk / regression
- Monitors will now generate MORE armed file writes (positions in 1.0-1.02 HF band)
- Executor handles "armed but maxLiquidation=0" by logging and waiting (already in code)
- No regression on healthy positions — they don't trigger the arm branch
- Pre-fire Telegram noise unchanged (only fires after HF<1.0 transition)

## Skill candidate evaluation
- Technologies/frameworks touched: Silo V2, Morpho Blue (Felix/Monad/Bend), Aave V3 (HyperLend/HypurrFi/Tydro), fleet-wide monitor architecture audit, fire-vs-arm armed-file semantics
- Domain-specific knowledge involved: the "log-only at arm zone" anti-pattern; cliff-edge oscillating positions oscillating between solvent/insolvent between sweeps; armed file as the pre-sign trigger (not the fire trigger); the fire-flag in payload pattern; uniform pre-arm semantics across chains regardless of protocol family
- Verdict: **GENERATE**
- Reason: First fleet-wide arm-file semantic audit. Establishes a uniform "pre-arm in arm zone" requirement for all liquidation monitors across protocol families. Reusable on any new chain we add.
