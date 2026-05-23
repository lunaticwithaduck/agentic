---
title: Re-test dRPC WSS for HyperEVM — earlier test showed 0 blocks
created: 2026-05-21
completed: 2026-05-21
status: done
---

## Goal
User shared documentation that dRPC free tier supports WSS newHeads on HyperEVM. Re-test more carefully.

## Findings
Anonymous dRPC WSS does **NOT** deliver newHeads on HyperEVM. Confirmed via 60s tests on 4 URL variants:

| URL | Result |
|---|---|
| `wss://hyperliquid.drpc.org` | Connects, sub-handshake returns `code 23: Unsupported subscription: newHeads` |
| `wss://hyperliquid-mainnet.drpc.org` | Same error |
| `wss://lb.drpc.org/ogws?network=hyperliquid` | HTTP 403 (needs auth key) |
| `wss://hyperliquid.drpc.org/ws` | HTTP 404 |

## Interpretation
- dRPC's docs claim WSS support across all listed chains
- For HyperEVM specifically, anonymous endpoints reject `newHeads`
- The 403 on `lb.drpc.org/ogws` is the real signal — dRPC's WSS appears to require a free-tier API key (dkey)
- With a dkey, it MIGHT work — untested without signup

## Outcome
Completed 2026-05-21. dRPC anonymous WSS is not a viable replacement for Alchemy WSS. Options remaining:
1. Sign up for free dRPC account, get a dkey, retest authenticated WSS. ~5 min user action.
2. Pay Alchemy WSS (~$30-100/mo per backtest)
3. Stick with HTTP polling on public pool (current state — works, just slower detection)

## Completion
Run `/complete workflows/tasks/2026-05-21-drpc-wss-investigation.md`.
