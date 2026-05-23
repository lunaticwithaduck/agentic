---
title: Scope Tydro liquidator lane (Aave V3 fork on Ink, $204M TVL)
created: 2026-05-22
completed: 2026-05-22
status: done
---

## Recommendation: **GO. ~5-7 hours focused work.**

Best opportunity outside Monad. Aave V3 fork = ~90% code reuse from HyperLend, thin competition (~5 EOAs), $204M TVL, real liquidation flow (3/day).

## Verified primitives

### Chain
- ChainID: **57073** (`0xdef1`)
- Block time: ~1s (Ink is OP-Stack L2)
- Public RPCs (all work): `rpc-gel.inkonchain.com`, `ink.drpc.org`, `rpc-qnd.inkonchain.com`
- getLogs cap: 100k blocks (fine for indexer)
- Explorer: `explorer.inkonchain.com`

### Tydro contracts (verified)
| Contract | Address |
|----------|---------|
| Pool | `0x2816cf15F6d2A220E789aA011D5EE4eB6c47FEbA` |
| PoolAddressesProvider | `0x4172E6aAEC070ACB31aaCE343A58c93E4C70f44D` |
| TydroOracle | `0x4758213271BFdC72224A7a8742dC865fC97756e1` |
| ProtocolDataProvider | `0x96086C25d13943C80Ff9a19791a40Df6aFC08328` |
| ACLManager | `0x86E2938daE289763D4e09a7e42c5cCcA62Cf9809` |

### All 13 Tydro reserves
| Token | Address | Decimals |
|-------|---------|----------|
| WETH | `0x4200000000000000000000000000000000000006` | 18 |
| kBTC | `0x73E0C0d45E048D25Fc26Fa3159b0aA04BfA4Db98` | 8 |
| USD₮0 | `0x0200C29006150606B650577BBE7B6248F58470c1` | 6 |
| USDG | `0xe343167631d89B6Ffc58B88d6b7fB0228795491D` | 6 |
| GHO | `0xfc421aD3C883Bf9E7C4f42dE845C4e4405799e73` | 18 |
| USDC | `0x2D270e6886d130D724215A266106e6832161EAEd` | 6 |
| weETH | `0xA3D68b74bF0528fdD07263c60d6488749044914b` | 18 |
| wrsETH | `0x9f0a74A92287E323Eb95c1cd9eCdBEb0e397cAe4` | 18 |
| ezETH | `0x2416092f143378750bb29b79eD961ab195CcEea5` | 18 |
| sUSDe | `0x211Cc4DD073734dA055fbF44a2b4667d5E5fE5d2` | 18 |
| USDe | `0x5d3a1Ff2b6BAb83b63cd9AD0787074081a52ef34` | 18 |
| SolvBTC | `0xaE4EFbc7736f963982aACb17EFA37fCBAb924cB3` | 18 |
| syrupUSDT | `0x8A76fe7fA6da27f85a626c5C53730B38D13603d7` | 6 |

### Ink DEX landscape (for swap-back)
| DEX | Architecture | Why useful |
|-----|--------------|-----------|
| **Velodrome Slipstream (Ink)** | Velodrome V3 fork (tickSpacing model) | DOMINANT — USDT0/WETH $1.7M, USDT0/kBTC $1.5M, kBTC/WETH $338k |
| **Reservoir V3** | Canonical Uniswap V3 | Backup for V3-style pairs |
| **Curve (Ink)** | StableSwap | Best for stable-stable (USDe/USDT0, etc.) |
| **Velodrome V2** | V2 AMM | Small but present |
| **InkySwap V3** | V3 fork | Some niche pairs |
| **DYORSwap, SquidSwap, InkySwap (V2)** | minor | tail |

### Key insight: Velodrome Slipstream uses tickSpacing (like Shadow on Sonic)
NOT canonical Uni V3. Same ABI gotchas as `sonic-silo/swap-path.js`: `tickSpacing` parameter where V3 uses `fee`. **Can reuse Sonic's swap-path code with minimal adaptation** — major win.

For canonical V3 backup, Reservoir V3 has standard ABI matching HyperLend.

## Implementation plan

### Files (mirroring HyperLend structure)
```
/home/jojo/automation/tydro/
  ├─ config.js               # Ink RPC pool, Tydro Pool addr, DEX catalog ref
  ├─ HyperLendLiquidator.sol # copy of the proven Aave V3 liquidator
  ├─ HyperLendLiquidator.abi.json / .bin
  ├─ deploy.js               # mirror of hyperlend/deploy.js
  ├─ indexer.js              # Borrow event scan → positions.json
  ├─ monitor.js (WSS or HTTP polling) — Aave V3 Multicall3.getUserAccountData
  ├─ executor.js             # presign + multi-DEX swap + broadcast
  ├─ swap-path.js            # multi-DEX (Velodrome Slipstream + Reservoir V3 + Curve)
  └─ data/...
```

### Shared lib additions
```
/home/jojo/automation/lib/
  └─ ink-dexes.js            # NEW — catalog of Ink DEXes
```

### Step-by-step (in target order)
1. Identify Velodrome Slipstream Ink contracts (factory, SwapRouter, QuoterV2) — 30 min via etherscan enumerate-by-deployer or Velodrome docs
2. Identify Reservoir V3 contracts (factory, router, quoter) — 30 min
3. Write `lib/ink-dexes.js` with both DEXes catalogued — 30 min
4. Copy `hyperlend/` → `tydro/` directory — 5 min
5. Adapt `tydro/config.js` (RPCs, Pool addr, reserve set) — 30 min
6. Adapt `tydro/swap-path.js` to use `lib/ink-dexes.js`, port tickSpacing handling from `sonic-silo/swap-path.js` — 1-2 hours
7. Adapt `tydro/indexer.js` (mostly same Aave V3 events) — 30 min
8. Adapt `tydro/monitor.js` (Multicall3 same on Ink) — 30 min
9. Deploy `HyperLendLiquidator.sol` on Ink (~$0.10 in ETH) — 15 min
10. Bridge $50-100 in ETH treasury — manual (Superbridge or official Ink bridge)
11. Smoke test on anvil with oracle-crash on a real Tydro position — 1 hour
12. Enable systemd services in DRY mode — 5 min
13. 24h DRY observation → flip live

**Total: 5-7 hours focused work + bridge funding.**

### Required for live cutover (manual user actions)
- Bridge ~$50-100 in ETH to Ink for gas treasury (via official Ink bridge `bridge.inkonchain.com` or Superbridge)
- Approve liquidator's `MIBERA_PK` to hold ETH on Ink

### Risk + cost
- **Cost to deploy**: <$0.50 in ETH on Ink (L2 cheap)
- **Engineering**: 5-7 hours
- **Monthly RPC cost**: $0 (public RPCs adequate; may want premium later if competition intensifies)
- **Risk**: medium — Velodrome Slipstream's exact ABI needs verification, but Shadow code on Sonic is a working reference

### Expected revenue
Based on observed: 3 liqs/day, varied sizes ($890 - $117k), thin competition.
- Conservative: $500-$1,500/mo (capturing a fraction of the smaller fires)
- Realistic: $2,000-$5,000/mo
- Upside: $10k+/mo if we hit one of the bigger $50k+ liquidations

Better risk/reward than Monad's current state because the lane is more mature with real ongoing flow.

## Out-of-scope (later)
- Sentora (Risk Curators on Ink) — different product, separable
- Spreads Finance Leverage ($238k TVL) — too small to bother

## Completion
Run `/complete workflows/tasks/2026-05-22-scope-tydro-lane.md`.
