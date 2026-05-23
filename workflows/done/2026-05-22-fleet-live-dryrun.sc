---
domain: defi-liquidations
source_task: 2026-05-22-fleet-live-dryrun.md
date: 2026-05-22
keywords: [live-dryrun, multi-chain, revert-formats, aave-v3-versions, morpho-string-errors, silo-userissolvent, dRPC-gas-cap, rate-limit-pacing]
---

## Extracted Knowledge

### Four distinct revert formats coexist within one liquidation fleet
A truly multi-chain revert-decoder must handle all four:

1. **Aave V3 modern custom errors** (Tydro, HyperLend):
   - `0x930bb771 HealthFactorNotBelowThreshold()`
   - `0x6679996d HealthFactorLowerThanLiquidationThreshold()`
   - 4-byte selector, no Error(string) prefix

2. **Aave V3 legacy Error(string)** (HypurrFi):
   - `Error("45")` = HEALTH_FACTOR_LOWER_THAN_LIQUIDATION_THRESHOLD
   - `Error("35")` = HEALTH_FACTOR_NOT_BELOW_THRESHOLD
   - 0x08c379a0 prefix + ABI-encoded numeric string
   - Older Aave V3 versions still use this — same protocol family, different format

3. **Morpho Blue string-Errors** (Felix, Bend, Monad):
   - `Error("position is healthy")`
   - 0x08c379a0 prefix + decoded ASCII
   - Not the canonical `HealthyPosition()` custom error (selector `0x6593fd52`) — Morpho forks often wrap with strings

4. **Silo V2 custom errors** (Sonic):
   - `0x5e26aa2a UserIsSolvent()`
   - `0x2ffe1c12 NoDebtToCover()`
   - `0xd2281fd3 AmountExceedsSolvency()`
   - `0xd65db62d FullLiquidationRequired()`

### Sibling Aave V3 forks can run different protocol versions
HyperLend and HypurrFi both run on HyperEVM, both are Aave V3 forks. HyperLend uses modern
custom errors; HypurrFi uses legacy string errors. Don't assume same chain = same version.
Always test each fork's actual revert format before trusting a shared decoder.

### Morpho markets without DEX pools are NOT bugs
Monad has 12 Morpho markets, but 3 have no Uniswap V4 or V3 pool for their
collateral→loan pair. Position state is real, but executor must skip — no atomic
arb is possible. Production code's `findBestPool() returns null` handling is the
correct response. The dry-run will surface this if you try one of those markets.

### dRPC restricts eth_call gas more strictly than QuickNode
dRPC (Monad paid endpoint via `lb.drpc.live`) returns:
> "user-specified gas exceeds provider limit" (-32603)
for ethers' default eth_call gas auto-fill. QuickNode public RPC has no such cap.
For dry-run simulations, prefer public RPCs with rate-limit pacing over paid RPCs
with gas restrictions. dRPC is fine for normal transactions; just not for sims.

### Live-RPC dry-run is the only complete validation before first fire
- Indexer test: confirms read-side wiring
- Anvil fork smoketest: confirms contract logic + oracle-crash mechanics
- Live-RPC eth_call dry-run: confirms live chain state matches assumptions
  (oracle prices, pool depth, addresses, ABI version, revert format)

The third one catches what the first two miss: protocol version drift, address typos
that compiled fine but point to nothing on chain, ABI selector mismatches.

### Stale indexer state is normal; handle it
HypurrFi's healthfactors.json showed a borrower at HF=1.12 with $24k debt. Live re-read
showed HF=∞ (debt fully repaid since the indexer snapshot). Iterate through candidates
until you find one with confirmed-live debt. This is robust behavior, not an indexer bug.

## Proposed Skill Content

Add to `.claude/skills/defi-liquidations.md`:

### Multi-chain revert-decoder must pre-load 4 formats
| Format | Example | Selector/Prefix | Used by |
|---|---|---|---|
| Aave V3 custom error | `HealthFactorNotBelowThreshold()` | `0x930bb771` | Tydro, HyperLend |
| Aave V3 legacy string | `Error("45")` | `0x08c379a0` + "45" | HypurrFi |
| Morpho string | `Error("position is healthy")` | `0x08c379a0` + msg | Felix, Bend, Monad |
| Silo V2 custom | `UserIsSolvent()` | `0x5e26aa2a` | Sonic |

### Live-RPC dry-run before LIVE flip on every new chain
The cheapest, highest-signal validation. Pattern: pick the closest-to-cliff real borrower,
build production calldata, eth_call against live RPC, decode revert. Expected: protocol's
healthy-position guard. Any other revert is a real bug worth fixing before broadcast.

### dRPC gas caps break eth_call for sims; use public RPC + pacing
For Monad and similar paid endpoints, `eth_call` with ethers' default gas auto-fill exceeds
the provider's per-call gas cap. Use public RPC (rate-limited but no gas cap) and pace
calls with explicit sleeps (e.g., 300ms between calls on QuickNode 25/sec limit).
