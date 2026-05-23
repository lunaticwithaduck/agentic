---
title: Felix f2 — monitor (HTTP + WSS) for Morpho Blue health factors
created: 2026-05-21
completed: 2026-05-21
status: done
---

## Goal
Build the HF monitor for Felix. Morpho Blue HF: `(collateral × oraclePrice × LLTV) / (1e36 × 1e18 × borrowed)`.

## Steps
- [x] Copied Bend's monitor.js as starting template (HF math is identical — same Morpho Blue ABI)
- [x] Adapted log strings + extended to read `Morpho.market(id)` per sweep (Felix's indexer doesn't pre-cache market state)
- [x] Wired oracle reads + Taylor-expanded interest accrual
- [x] First `--once --dry` sweep: 204 positions evaluated, HF computed correctly
- [ ] WSS variant deferred to f5 — HTTP monitor proves the architecture; WSS upgrade can mirror what we did for HyperLend

## First sweep findings
**Closest 8 to HF=1.0:**

| Market | User | HF | Notes |
|---|---|---:|---|
| `0xd7d38220..` (WHYPE→USDC, LLTV 77%) | `0x023f609c..` | **1.002** | razor-edge |
| `0x64e7db7f..` (kHYPE→WHYPE, LLTV 86%) | `0x68ec6111..` | 1.008 | |
| `0x8eecdd03..` (USDe→USDT0, LLTV 92%) | `0x24df4b7a..` | 1.021 | |
| `0x64e7db7f..` | `0xedc56ecd..` | 1.052 | $424 debt |
| `0x64e7db7f..` | **`0xf3115b86..`** | **1.057** | **~$623k WHYPE debt** ← whale |

204 active positions — Felix is far more liquidation-active than HyperLend (which had ~5 in the same threshold band).

## Outcome
Completed 2026-05-21. HTTP monitor functional. Sweeps every poll, computes HF correctly, writes armed file when HF < FIRE_THRESHOLD (1.0). Display bugs in console output (debt shows $0 for non-18-decimal loan tokens, coll shows NaN — missing decimals lookup) are cosmetic; **arm/fire logic uses raw BigInt comparisons** so they're not affected. Will fix display in a hardening pass once monitor is paired with executor.

**WSS variant**: deferring to separate task (felix-f5-wss). HTTP monitor at 2s polling already faster than HyperLend's pre-WSS state. We can A/B compare after running HTTP for a session.

Real opportunity flagged: `0x64e7db7f..` user `0xf3115b86..` holds ~$623k in WHYPE debt at HF 1.057. If WHYPE drops ~5% this becomes a major fire — biggest single liquidation we'd ever see on the fleet.

## Completion
Run `/complete workflows/tasks/2026-05-21-felix-f2-monitor.md` before starting f3.
