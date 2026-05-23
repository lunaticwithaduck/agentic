---
title: Unify revert-decoder across fleet so 4 formats don't break production + dRPC Monad finding
created: 2026-05-22
completed: 2026-05-22
status: done — decoder unified, 5 executors reloaded, dRPC limitation documented
---

## Result: shared revert-decoder now handles all 4 fleet revert formats

### The gap
`lib/revert-decoder.js` (imported by Tydro, HyperLend, HypurrFi, Felix, Monad executors)
had two design flaws:
1. **Error(string) reverts weren't ABI-decoded** — the regex over `err.message` only fired
   for some ethers error shapes. When `err.data` was `0x08c379a0...`, the SELECTOR branch
   hit first, looked up `0x08c379a0` in the table (not present), returned `unknown-selector`
   → flagged as `actionable` → would have Telegram-spammed every healthy-position pre-flight
   on HypurrFi (legacy `Error("45")`), Felix, Monad (`Error("position is healthy")`).
2. **Missing healthy-position selectors** — no `0x6593fd52 HealthyPosition()`,
   no `0x5e26aa2a UserIsSolvent()` (Silo V2).

### The fix
Three changes to `lib/revert-decoder.js`:
1. `decodeErrorString(data)` — explicit ABI parse of `0x08c379a0`-prefixed data, returns the
   underlying UTF-8 string.
2. `STRING_REASON_TABLE` — maps both numeric codes ("35", "45") and string messages
   ("position is healthy", "healthy position") to expected/actionable classification.
3. Added Morpho Blue + Silo V2 selectors to `SELECTOR_TABLE` (HealthyPosition, UserIsSolvent,
   NoDebtToCover, plus a dozen actionable kinds like ReserveFrozen, FullLiquidationRequired).

### Verification
10/10 unit tests pass — every fleet revert format classifies correctly:
| Format | Bytes | Decoded | Classified |
|--------|-------|---------|------------|
| Aave V3 modern | `0x930bb771` | HealthFactorNotBelowThreshold() | ✅ silent-skip |
| Aave V3 legacy | `Error("45")` | health-factor-ok | ✅ silent-skip |
| Aave V3 legacy | `Error("35")` | health-factor-ok | ✅ silent-skip |
| Morpho fork | `Error("position is healthy")` | health-factor-ok | ✅ silent-skip |
| Silo V2 | `0x5e26aa2a` | UserIsSolvent() | ✅ silent-skip |
| Liquidator NoProfit | `0x31708d59` | NoProfit(uint256,uint256) | ✅ actionable |
| Aave ReserveFrozen | `0x6d305815` | ReserveFrozen() | ✅ actionable |
| Unknown selector | `0xdeadbeef` | unknown | ✅ actionable |

Re-ran HyperLend + HypurrFi live-dryruns — both pass. Restarted all 5 executors that
use the shared decoder so they load the new code.

## Result #2: dRPC Monad endpoint blocks ALL eth_call methods (service-side)

User asked if dRPC could be used for Monad dryruns. Direct curl confirmed:
- `eth_call` with any gas value (including absent) → `"user-specified gas exceeds provider limit"`
- `eth_call` with empty calldata → same error
- `eth_blockNumber`, `eth_chainId`, `eth_sendRawTransaction` → all work fine

**Conclusion**: dRPC's Monad endpoint is configured to reject eth_call regardless of params.
Not a client-side issue. Workaround: production already splits correctly — reads on
QuickNode (HTTP_RPC), writes on dRPC (HTTP_RPC_FIRE). For dryruns, must use HTTP_RPC.

Saved as memory `reference_drpc_monad_eth_call.md` for future expansion work.

## Files modified
- `lib/revert-decoder.js` — added decodeErrorString, STRING_REASON_TABLE, Morpho + Silo selectors
- `monad/live-dryrun.js` — rawCall via fetch (no ethers gas auto-fill), QuickNode RPC,
  positions.json fallback for when no risky position is in a liquid market

## Files added
- (none — fix was in existing decoder)

## Executors restarted (loaded new decoder code)
- tydro-executor ✓
- hyperlend-executor ✓
- hypurrfi-executor ✓
- felix-executor ✓
- monad-executor ✓

## Outcome
Completed 2026-05-22. Production decoder gap closed — no chain in the fleet will mis-flag
a healthy-position revert as an actionable anomaly anymore. dRPC Monad limitation documented.
HyperLend + HypurrFi pre-flight paths verified end-to-end with their actual production
revert formats.
