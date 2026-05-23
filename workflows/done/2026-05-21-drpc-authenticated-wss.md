---
title: Test dRPC authenticated WSS for HyperEVM with user's dkey + wire if works
created: 2026-05-21
completed: 2026-05-21
status: done
---

## Goal
User's dRPC dkey + multiple URL patterns tested. Confirm if authenticated dRPC WSS delivers HyperEVM newHeads.

## Findings
Even with valid dkey:
- `lb.drpc.org/ogws?network=hyperliquid&dkey=…` → code 23 "Unsupported subscription: newHeads"
- `lb.drpc.live/hyperliquid/…` → same code 23
- `hyperliquid.drpc.org/ogws?dkey=…` → connects + subscribes + delivers blocks BUT block numbers are ~25M at 5-15s intervals (HyperEVM is at 35.7M with 1s blocks — this is a DIFFERENT chain, not HyperEVM)
- `hyperliquid.drpc.org?dkey=…` → HTTP 404

## Interpretation
dRPC's HyperEVM endpoint appears misconfigured or proxied to another chain entirely. The "working" subscription delivers some other chain's blocks. Either way, dRPC cannot serve HyperEVM newHeads at this time.

## Outcome
Completed 2026-05-21. dRPC dkey stashed in .env for future use on other chains. **No working free WSS for HyperEVM exists.** Sticking with HTTP polling on public RPC pool. To regain sub-second detection on HyperEVM, the only viable path is paying Alchemy (~$30-100/mo per backtest projections).

User flagged with "sorry" — confirming they understand the result is negative through no fault of theirs; dRPC's marketing material claims support that their actual endpoint doesn't deliver.

## Completion
Run `/complete workflows/tasks/2026-05-21-drpc-authenticated-wss.md`.
