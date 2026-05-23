---
title: Ship Monad V4 production integration + flip live
created: 2026-05-22
completed: 2026-05-22
status: done
---

## 🟢 MONAD LIVE — 6th chain shipped

Live since 13:48:53 UTC on 2026-05-22.

## What landed
- **MorphoLiquidator.sol** updated: when `swapTarget == UNIVERSAL_ROUTER`, contract does the Permit2 dance (ERC20.approve(Permit2, MAX) idempotent + Permit2.approve(token, UR, amount, deadline)) before calling `swapTarget.call(swapData)`
- **Recompiled** to 3956 bytes (up from 3425)
- **Redeployed** at `0x235899576Deb5ea87d7eE8fD0859e83E46BA5300` (tx `0xa90c2f47d2d898f7a5ac58b559f62417ee359d20beb9255789ba2661334317e6`, 1.05M gas, 0.105 MON ≈ $0.005)
- **`lib/monad-dexes.js`** rewritten: now includes both uniswap-v4-monad (with UR + Quoter + Permit2) and uniswap-v3-monad
- **`monad/swap-path.js`** rewritten with separate `findBestV3` and `findBestV4` branches keyed off `dex.routerSig`. Single `findBestPool()` flattens results across all DEX entries and picks max `expectedOut`.
- **`monad/executor.js`** updated to pass full `pool` object to `buildSwapData` so V4 paths get the PoolKey
- **systemd**: `MONAD_DRY=1` removed from `monad-executor.service`, daemon-reloaded, restarted

## Verification
- `swap-path.js --discover`: wstETH/WETH market `0x8bdb7d2c50` correctly routes via uniswap-v4-monad at fee=100, expectedOut 123.47 WETH for 100 wstETH probe ✓
- Markets that fail to route (`wsrUSD/USD1`, `syzUSD/USDC`, etc.) correctly show "no route" — they have no V3/V4 secondary market per the depth audit
- Service active, mode LIVE, signer + contract addr both correct

## Fleet status
| Chain | Status |
|-------|--------|
| 🦊 Felix | 🟢 LIVE |
| 🐻 Bend | 🟢 LIVE |
| 💧 HyperLend | 🟢 LIVE |
| 😼 HypurrFi | 🟢 LIVE |
| 🔵 Sonic Silo | 🟢 LIVE |
| ⚡ **Monad** | **🟢 LIVE (just shipped)** |

## What can fire on Monad now
- wstETH/WETH whale cluster ($16.5M total, 4 whales at HF 1.002) — V4 pool $535k has 100-300 wstETH per-fire capacity, plenty of headroom for Morpho's partial-liquidation caps
- earnAUSD/USDC ($2.79M) — V4 AUSD/USDC pool $3.88M
- Anything else on Monad with AUSD or WMON as loan token

## What stays uncapturable (acknowledged limits)
- wsrUSD/USD1, syzUSD/USDC, YZM/USDC: no V3/V4 secondary market exists. Monitor will log them but executor won't fire. Could add Curve later if depth materializes.

## Costs vs Felix lane
- Deploy: 0.105 MON ≈ $0.005 (vs Felix's ~$0.50)
- Per-swap gas: ~165k V4 (vs V3's ~120k) — still negligible at MON $0.03
- Treasury: 12.87 MON ≈ $0.39 remaining (fine for many fires)

## Completion
Run `/complete workflows/tasks/2026-05-22-ship-monad-v4-live.md`.
