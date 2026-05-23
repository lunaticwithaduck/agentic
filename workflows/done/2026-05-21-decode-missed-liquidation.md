---
title: Retroactively decode last night's missed HyperLend liquidation
created: 2026-05-21
completed: 2026-05-21
status: done
---

## Goal
HyperLend executor saw a real liquidation opportunity at 2026-05-21T00:39:58Z (user `0x85b9899118fac92ca10a429361a13161af63f34e`, HF 0.995, $4,312 debt) but `estimateGas` reverted with an unknown custom selector. Replay historically to capture the raw revert data and decode it.

## Steps
- [x] Located HyperEVM block at 00:39:58Z → block 35653434
- [x] Rebuilt the liquidator's calldata from executor log values
- [x] Replayed via stakely.io archive RPC at block 35653433 (one block before revert)
- [x] Captured revert data: `0x930bb771 = HealthFactorNotBelowThreshold()`
- [x] Investigated why HF flipped from 0.995 to 1.2078 in <1s
- [x] Found the competitor liquidation event in block 35653432

## Findings
**The "unknown custom error" was `0x930bb771 = HealthFactorNotBelowThreshold()`** — already in our decoder table as `expected:true`. The gas-estimate path wasn't routing through the decoder yet (that was fixed earlier today in `decode-gas-estimate-reverts`).

**The real cause of the miss was speed, not encoding.** Sequence:
- 00:39:55.916Z — our monitor arms (HF below 1.02)
- 00:39:56.000Z — block 35653432: competitor `0xdd8692Bc25972DBa5906201960e2dbe783D460Fa` lands LiquidationCall
- 00:39:57.792Z — our executor runs fresh-HF check; sees HF 0.995 (the read was either of a slightly earlier block, or block 35653432 hadn't yet propagated to our RPC)
- 00:39:58.287Z — our `estimateGas` runs at block 35653434; Aave now sees HF 1.2078 → revert with 0x930bb771

**Competitor's fire details (block 35653432, tx `0x885224…`):**
- coll: USDC, debt: WHYPE
- debtToCover: 37.88 WHYPE (~$2,138)
- seized: 2,289 USDC
- gross profit: ~$151 before swap fees & gas

**`0xdd8692Bc…` is a known competitor** — was flagged as #9 by event count in the backtest with $1,271 profit on a previous fire of the same whale class. They appear to be specifically tracking this user. Almost certainly WSS-subscribed.

## Outcome
Completed 2026-05-21. We had one real fire opportunity in the bot's lifetime so far and lost it by 1-2 seconds to a faster competitor. Our flow was structurally correct — fresh-HF check passed, pair selection right, LIF math right, swap path right. The miss is pure speed.

**No decoder change needed** — the selector was already known.

**Architectural takeaway:** HTTP polling at 2s + multi-RPC fallback + estimateGas roundtrip adds up to a 2-3s response window. WSS subscriptions get sub-second. To win against `0xdd8692Bc…` we need:
1. WSS-driven monitor (eliminate poll-cycle latency)
2. Possibly pre-built calldata cache for repeated user/pair combos
3. Skip the multi-pair iteration when there's only one valid combo (this user always has USDC coll + WHYPE debt)

These are architectural changes, not bug fixes. The bot as-is will catch slower fires when no other liquidator notices first — which the backtest suggests is ~30-50% of catchable events.

## Completion
Run `/complete workflows/tasks/2026-05-21-decode-missed-liquidation.md`.
