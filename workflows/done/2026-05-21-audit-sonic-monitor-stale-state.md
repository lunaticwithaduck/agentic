---
title: Audit Sonic Silo V2 monitor for the same stale-state bug we just fixed on Felix
created: 2026-05-21
completed: 2026-05-21
status: done
---

## Goal
Verify whether `sonic-silo/monitor*.js` reads live position state on every HF/LTV computation, or whether it trusts an indexer-maintained snapshot.

Expanded scope mid-audit to cover the whole fleet, since the question "does this bug exist elsewhere?" is the real question.

## Findings — fleet survey

| Chain | Source of HF/LTV per tick | Status |
|-------|---------------------------|--------|
| Sonic Silo | `SiloLens.isSolvent + getUserLTV + debtBalanceOfUnderlying` (live) | ✅ Clean |
| HyperLend (Aave V3) | `Pool.getUserAccountData(user)` via Multicall3 (live) | ✅ Clean |
| HypurrFi (Aave V3) | `Pool.getUserAccountData(user)` via Multicall3 (live) | ✅ Clean |
| Felix (Morpho Blue) | `morpho.position(mid, addr)` live — NOW fixed | ✅ Clean (just fixed) |
| Bend (Morpho fork) | `p.collateral`/`p.borrowShares` from positions.json (STALE) | 🚨 **Buggy** |

## Root cause is structural
- Aave V3 and Silo V2 expose a **single live HF view function** (`getUserAccountData` / `isSolvent`). The monitor just calls it live — no cached state needed.
- Morpho Blue does **not**. To get HF you must compose from `position()` + `market()` + `oracle.price()`. That composition tempts caching the position part — which is exactly what Felix and Bend both did.

## Outcome
Completed 2026-05-21. Sonic, HyperLend, HypurrFi confirmed clean by architecture. Bend confirmed to have the identical bug as Felix (was the source file we copied Felix from). Bend has been quiet (no positions <HF 1.09 today) so it hasn't bitten, but the fix is the same one we just applied to Felix.

Separate task will be created for the Bend fix.

## Completion
Run `/complete workflows/tasks/2026-05-21-audit-sonic-monitor-stale-state.md`.
