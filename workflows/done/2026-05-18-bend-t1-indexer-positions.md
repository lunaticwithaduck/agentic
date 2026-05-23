---
title: Bend position indexer — discover and track all open borrower positions
created: 2026-05-18
completed: 2026-05-18
status: done
---

## Goal
Build the foundation: an indexer that knows every open borrower position across all 6 Bend markets at any moment. Without this, we can't compute health factors or know who to liquidate.

Singleton: `0x24147243f9c08d835C218Cda1e135f8dFD0517D0` (Bend = Morpho Blue fork on Berachain)

## Approach
Scan Borrow + SupplyCollateral + WithdrawCollateral + Repay events from genesis to populate an on-disk JSON registry. Then subscribe via WSS to keep it live.

## Markets to track (all loan token = HONEY `0xFCBD14DC51f0A4d49d5E53C2E0950e0bC26d0Dce`)
- WBTC/HONEY — `0x950962c1cf2591f15806e938bfde9b5f9fbbfcc5fb640030952c08b536f1f167` (LLTV 86%, $162k borrowed)
- sUSDe/HONEY — `0x1ba7904c73d337c39cb88b00180dffb215fc334a6ff47bbe829cd9ee2af00c97` (LLTV 91.5%, $60k)
- WETH/HONEY — `0x1f05d324f604bd1654ec040311d2ac4f5820ecfd1801a3d19d2c6f09d4f7a614` (LLTV 86%, $57k)
- WBERA/HONEY — `0x147b032db82d765b9d971eac37c8176260dde0fe91f6a542f20cdd9ede1054df` (LLTV 77%)
- wgBERA/HONEY — `0x63c2a7c20192095c15d1d668ccce6912999b01ea60eeafcac66eca32015674dd` (drained, watch for re-borrows)
- iBERA/HONEY — `0x594de722a090f8d0df41087c23e2187fb69d9cd6b7b425c6dd56ddc1cff545f0` (drained, watch for re-borrows)

## Steps
- [x] ~Determine deploy block~ — instead auto-discover all markets via CreateMarket event log scan. Found 7 markets (6 in docs + 1 unlisted `0xa19a3d03...`)
- [x] Implement historical event scrape (SupplyCollateral + Borrow are the discovery events; WithdrawCollateral/Repay/Liquidate handled via reconcile)
- [x] Build position registry: `{ marketId, borrower } → { supplyShares, borrowShares, collateral, active, lastChecked }` plus per-market params (oracle, irm, lltv, totals, collDecimals)
- [x] Persist registry to `/home/jojo/automation/bend/data/positions.json` (atomic temp+rename writes)
- [x] WSS subscriber filtered to position-affecting topics only (drops FlashLoan/AccrueInterest noise — `0xc1fad5...` bot fires ~1500 flashloans/hr)
- [x] Reconciliation via direct `position(id, borrower)` reads — never derive position state from event deltas
- [x] CLI util `positions.js`: summary mode, `--market WBTC/HONEY`, `--addr 0xabc...`, `--json`
- [x] `--quick` mode for incremental updates (skips CreateMarket scan + scans only from cursor)

## Validation results (2026-05-18)
- 33 active borrowers across 5 active markets, $210k+ HONEY under management
- Largest: WBTC borrower `0xe80859fe...` with 1.52 WBTC ($150k) coll + $73,626 debt (LTV ~49%, healthy)
- WETH whale: `0x9258bf43...` with 46 WETH + $49,746 debt
- iBERA + wgBERA markets fully drained (matches the May 6-11 liquidation cluster from research)
- WSS subscription verified live — filter correctly drops FlashLoan/AccrueInterest spam

## Constraints
- Use only publicnode RPC (no paid services for v1)
- Tolerate RPC errors; reconnect WSS automatically; never crash on missing events
- Disk format must be human-readable JSON for debugging

## Outcome

Completed 2026-05-18 (same-day build, ~2h). The Bend position indexer is live at `/home/jojo/automation/bend/`. It auto-discovers markets (found a 7th market not in the docs), discovers borrowers via SupplyCollateral+Borrow events, reconciles state via direct `position()` reads (never event-derived — too easy to drift), persists atomically to JSON, and tails new events via WSS filtered to position-affecting topics only (drops the ~1500/hr FlashLoan spam from the existing `0xc1fad5...` arb bot). Current state: 33 active borrowers, ~$210k addressable HONEY debt across 5 active markets. Foundation for t2 (health-factor monitor) is in place.
