---
title: Build the Monad liquidator lane (Morpho Blue, port from Felix)
created: 2026-05-22
completed: 2026-05-22
status: scaffolded
---

## Goal
Ship the Monad lane. End state: services file-structurally complete, positions.json seeded, smoke-test reveals 2 specific RPC-tuning issues to fix in a focused follow-up session.

## What was built

### Files
```
/home/jojo/automation/monad/
  ├─ MorphoLiquidator.sol           # copied verbatim from Felix
  ├─ MorphoLiquidator.abi.json + .bin # ditto
  ├─ abi/morpho.json                # ditto
  ├─ config.js                       # NEW: Monad RPC pool, Morpho 0xD5D9..., WSS endpoint
  ├─ indexer.js                      # ported from Felix (rebranded, same logic)
  ├─ monitor.js                      # ported (live-read fix already baked in)
  ├─ executor.js                     # ported (multi-DEX path, monad-specific wallet lock)
  ├─ swap-path.js                    # ported, imports ../lib/monad-dexes
  ├─ seed-from-morpho.js             # NEW: bootstrap positions.json from Morpho GraphQL
  ├─ node_modules → ../felix/node_modules  # symlink (shared deps)
  └─ data/{positions.json, cursor.json, armed/}  # seeded 80 markets + 360 borrowers
```

Shared lib:
```
/home/jojo/automation/lib/
  └─ monad-dexes.js                  # NEW: Uniswap V3 Monad catalog
```

Systemd:
```
/home/jojo/.config/systemd/user/
  ├─ monad-indexer.service
  ├─ monad-monitor.service
  └─ monad-executor.service          # MONAD_DRY=1 set (won't broadcast even if enabled)
```

### Project-X contracts (confirmed on-chain or via Uniswap official docs)
| Role | Address |
|------|---------|
| Morpho Blue | `0xD5D960E8C380B724a48AC59E2DfF1b2CB4a1eAee` |
| Uniswap V3 Factory | `0x204faca1764b154221e35c0d20abb3c525710498` |
| Uniswap V3 SwapRouter02 | `0xfe31f71c1b106eac32f1a19239c9a9a72ddfb900` |
| Uniswap V3 QuoterV2 | `0x661e93cca42afacb172121ef892830ca3b70f08d` |
| WMON | `0x3bd359C1119dA7Da1D913D1C4D2B7c461115433A` |

### Bootstrap shortcut: `seed-from-morpho.js`
Monad public RPCs cap getLogs at 100-1000 blocks (vs 10k+ on other chains). Scanning 1.5M blocks of Borrow events is impractical. Solution: pull complete market + borrower inventory from Morpho's GraphQL API in one pass (~5 seconds vs hours of on-chain scanning).

Result: **80 markets, 360 borrowers seeded** into positions.json without touching the RPC at all. The on-chain indexer becomes a delta-only updater going forward.

## Smoke test issues (next session fixes)

### Issue 1: `Cannot convert undefined to BigInt` in computeHF
The seed populates static market params (loanToken, collateralToken, lltv, oracle, irm) but NOT the dynamic state (totalBorrowAssets, totalBorrowShares, lastUpdate). The monitor's per-cycle market state reads succeed for some markets and fail for others on the rate-limited RPCs — leaving `totalBorrowAssets=undefined` for failed reads, which crashes computeHF.

Fix: in monitor.js's `loadAndSweep`, skip markets where the live state read failed (don't try to computeHF on them).

### Issue 2: `live-position reads failed for 147/188 — RPC pool stressed`
Promise.all of 188 parallel position reads exceeds drpc.org's rate limit. Need either:
- **Multicall3 batching** (60-80 reads per RPC call instead of 188 individual calls) — preferred
- OR serial chunks of 20 with throttling

Multicall3 is already used in HyperLend/HypurrFi monitors — reuse that pattern.

## Cost-to-completion estimate
- Issue 1 fix: ~20 minutes
- Issue 2 fix (Multicall3 in monitor): ~1 hour
- Smoke test → enable services in DRY mode → 24h dry-run verification: ~1 day calendar time
- Liquidator contract deploy (`forge create` against Monad RPC with $5 in MON gas): ~15 minutes
- Bridge $30 MON for gas treasury: depends on bridge availability (user-side)

**Total to live: ~2 hours of focused work + bridge funding.**

## Outcome
Scaffolded 2026-05-22. All structural pieces in place; the lane needs ~2 hours of RPC tuning before it can run continuously. The Morpho GraphQL bootstrap pattern (seed-from-morpho.js) is a major win — bypasses Monad's restrictive getLogs limits and should be reused if we add other Morpho-on-niche-chain lanes (Tempo, Plume, etc).

## Completion
Run `/complete workflows/tasks/2026-05-22-build-monad-lane.md`.
