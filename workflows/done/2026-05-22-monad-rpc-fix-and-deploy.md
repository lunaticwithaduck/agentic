---
title: Fix Monad monitor RPC issues, narrow watch to HF<1.05, deploy liquidator
created: 2026-05-22
completed: 2026-05-22
status: done
---

## Outcome
**Monad lane is now LIVE in DRY mode** — 6th chain in the fleet. Contract deployed, all 3 services active, monitor catching the wstETH/WETH whale cluster at HF 1.002.

## What landed
- **Monitor fixed**: chunked Promise.all into batches of 8 with 150ms gap (was 188-parallel → 70%+ failures; now reliable)
- **Skip undefined market state**: `if (!m.totalBorrowAssets || !m.totalBorrowShares) continue` before computeHF (eliminates the BigInt(undefined) crash)
- **WATCH_THRESHOLD_HF lowered 1.10 → 1.05**: cuts per-tick position reads ~80%, reduces RPC saturation
- **Liquidator deployed**: `0x3C5183D8766d03f9B847a22A70b3805C62A73a0F` (same CREATE address as HyperLend's — same wallet + nonce). Tx `0x1d9295ca50357bf60e3413cf5c9e5c6a88e16f79a4f953bc307d1a47bc9d847f`. Cost: 0.091 MON ≈ $0.005.
- **Config updated**: `MONAD_LIQUIDATOR` constant set
- **3 systemd services enabled + started**: indexer / monitor / monad-executor (DRY env var set)

## Smoke test results
Monitor's first sweep found 19 armable positions at HF<1.02, including:
| Borrower | Market | HF | Estimated debt |
|----------|--------|-----|----------------|
| `0x76f768dbeaa0` | earnAUSD/USDC | **1.0009** | $2.79M (audit) |
| `0x713ab45c6625` | wstETH/WETH | 1.0018 | $2.95M |
| `0x044808e42653` | wstETH/WETH | 1.0021 | $5.59M |
| `0x7902bc5b6626` | wstETH/WETH | 1.0022 | $5.46M |
| `0x933a7c11cc5f` | wstETH/WETH | 1.0021 | $2.6M |
| `0xd3f78e020505` | syzUSD/USDC | 1.0035 | $684k |

When any of those crosses HF<1.0, the executor writes an armed file and the pre-sign+broadcast path kicks in (currently DRY → won't actually broadcast).

## Fleet status
| Chain | Status |
|-------|--------|
| 🦊 Felix (HyperEVM Morpho) | 🟢 LIVE |
| 🐻 Bend (Berachain Morpho fork) | 🟢 LIVE |
| 💧 HyperLend (HyperEVM Aave) | 🟢 LIVE |
| 😼 HypurrFi (HyperEVM Aave) | 🟢 LIVE |
| 🔵 Sonic Silo | 🟢 LIVE |
| ⚡ **Monad (Morpho Blue)** | **🟡 DRY — 6th chain** |

## To flip Monad live
```bash
# Edit /home/jojo/.config/systemd/user/monad-executor.service
# Remove the line: Environment="MONAD_DRY=1"
systemctl --user daemon-reload
systemctl --user restart monad-executor.service
```

Recommend 24h DRY observation first to verify:
- No spurious HF<1.0 events
- HF math matches Morpho GraphQL spot-checks
- No RPC saturation alerts

## Completion
Run `/complete workflows/tasks/2026-05-22-monad-rpc-fix-and-deploy.md`.
