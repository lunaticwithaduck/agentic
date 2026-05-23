---
title: Scope the Monad liquidator lane (Morpho Blue, $21.6M at-risk inventory)
created: 2026-05-22
completed: 2026-05-22
status: done
---

## Goal
Scope adding Monad as the 6th liquidator lane. Document only — no implementation yet.

## Recommendation: **GO. Implement when you're ready.**

Estimated effort: **1-2 days** (one focused session). Risk: low. Code reuse from Felix: ~90%.

## Research findings

### Chain primitives
| Property | Value |
|----------|-------|
| Chain ID | 143 (`0x8f`) |
| Block time | **0.5s** (fast — must use WSS, HTTP polling would lag) |
| Public HTTP RPCs | `https://rpc.monad.xyz`, `https://rpc-mainnet.monadinfra.com`, `https://monad.drpc.org` |
| Public WSS | **`wss://rpc.monad.xyz`** confirmed `newHeads` subscription works |
| Native gas token | MON |
| Explorer | (likely monadscan.xyz) |

### Morpho Blue deployment
- **Address**: `0xD5D960E8C380B724a48AC59E2DfF1b2CB4a1eAee` (confirmed bytecode 31KB)
- Same canonical interface — `position(bytes32,address)`, `market(bytes32)`, `flashLoan(token, assets, data)`
- TVL: $154M
- Sample at-risk position from audit: market `0xfe1d7da2fbde85b1...`

### Liquidator contract
**`MorphoLiquidator.sol` from Felix can be deployed VERBATIM** with only the constructor arg changed to Monad's Morpho address. Confirmed:
- Uses `IMorpho.flashLoan(token, assets, data)` (Morpho's own flashloan, same interface across all Morpho deployments)
- Router-agnostic `swapTarget` pattern (already supports multi-DEX from project-x integration)
- No Monad-specific code paths needed

### DEX coverage for swap-back
| DEX | Confirmed pools (samples) | Architecture |
|-----|--------------------------|--------------|
| Uniswap V3 (Monad) | WMON/USDC ($1.7M), EURW/USDC ($1M), shMON/WMON | canonical Uni V3 |
| Pancakeswap V3 (Monad) | USDC/WMON ($442k), AUSD/WMON ($412k), WBTC/WMON | canonical Uni V3 fork |
| **Curve (Monad)** | **cbBTC/WBTC/LBTC ($5.65M), AUSD/USDC/USDT0 ($3M), shMON/WMON/sMON/gMON ($775k)** | StableSwap |
| Uniswap V4 (Monad) | listed but newer arch | V4 hooks (skip for v1) |
| LFJ V2.1/V2.2, DYORSwap, Madness, Aethon, Octo | mostly V2/V3 forks | mixed |

Multi-DEX best-quote pattern from project-x integration applies directly. For v1 we wire Uniswap V3 + Pancakeswap V3 + Curve (the three with verified depth on the assets we care about).

### Opportunity (snapshot 2026-05-22)
- 11 live positions at HF ≤1.10, total debt **$21.6M**
- Whale cluster: 4× wstETH/WETH @ HF 1.002 totaling **$16.5M**
- Recent 60d seize total: $342k across 20 events (one $320k single fire on syzUSD/USDC in April)
- Multiple lending protocols on Monad (Euler V2 $88M, Curvance $62M, Neverland $44M) — future expansion targets

## Implementation plan

### Files (mirroring Felix structure)
```
/home/jojo/automation/monad/
  ├─ config.js              # Monad RPC pool, Morpho addr, DEX catalog
  ├─ indexer.js             # event scan: Borrow, SupplyCollateral, etc → positions.json
  ├─ monitor.js             # live-read position state (per the felix fix), arm HF<1.02
  ├─ executor.js            # presign + multi-DEX swap + broadcast
  ├─ MorphoLiquidator.sol   # copy of Felix's contract
  └─ data/
       positions.json
       healthfactors.json
       armed/
```

### Shared lib additions
```
/home/jojo/automation/lib/
  ├─ monad-dexes.js         # NEW — Uniswap V3, Pancake V3, Curve factory+router+quoter addresses
  └─ (existing hyperevm-dexes.js stays as-is)
```

The shared `lib/discover-pools.js` survey tool gets a new `CHAINS.monad` entry so we can audit Monad routing weekly.

### Systemd services
- `monad-indexer.service`
- `monad-monitor.service` (WSS-driven, similar to hyperlend-monitor-wss / hypurrfi-monitor-wss)
- `monad-executor.service`

### Step-by-step
1. **Bridge gas treasury** (manual) — ~$200 MON via official bridge or Stargate. Need to confirm bridge availability.
2. **Deploy `MorphoLiquidator.sol`** with constructor arg = Morpho Monad address. Use `forge create` or our existing deploy script with Monad RPC.
3. **Catalog DEX addresses**: query each DEX's first verified pool, call `factory()` to get factory addr. Look up routers via Etherscan or follow the same enumeration-by-deployer pattern we used for project-x.
4. **Write `monad/config.js`** with RPC pool, Morpho addr, deployed liquidator addr, DEX catalog reference.
5. **Port `felix/indexer.js`** — change `cfg.MORPHO`, event topics are identical for Morpho. Adjust scan chunk size if Monad RPC has limits.
6. **Port `felix/monitor.js`** — same live-read pattern, point to Monad RPC pool, use the WSS-driven monitor architecture for sub-second detection (essential at 500ms block time).
7. **Port `felix/executor.js`** — change config import, leave swap-path logic the same since multi-DEX pattern already works.
8. **Port `felix/swap-path.js`** — change `require('../lib/hyperevm-dexes')` to `require('../lib/monad-dexes')`.
9. **Smoke test in `--dry` mode** for 24h. Verify positions.json populates, HF values match Morpho GraphQL, would-fire events would have been profitable.
10. **Flip live** by removing DRY env var, watch for first fire.

### Risk + cost
- Bridge: ~$200 in MON (gas treasury)
- Liquidator deploy gas: ~$5
- Monthly RPC: $0 (Monad public RPCs reliable per our test)
- Engineering: 1-2 days focused
- **Total cost to live: ~$210 + 1-2 days**

### Decision gates during implementation
- If Monad WSS proves flaky after 24h dry-run → fall back to 500ms HTTP polling (acceptable but burns more CPU)
- If DEX coverage too thin for a specific whale's collateral → keep that market in the watch list but skip-fire until depth improves
- If Morpho's Monad markets all stabilize back to HF >1.10 within 48h of going live → fine, opportunity is event-driven not steady-state

## Out-of-scope (later)
- Aave V3 on Monad (if deployed — DefiLlama didn't show one, but worth re-checking quarterly)
- Euler V2 / Curvance / Neverland lanes — separate architectures
- Uniswap V4 hook-based pools — different routing model

## Outcome
Completed 2026-05-22. Scope is recommend-to-implement. All foundational pieces verified on-chain. Ready to pick up as a focused 1-2 day session whenever you say go.

## Completion
Run `/complete workflows/tasks/2026-05-22-scope-monad-lane.md`.
