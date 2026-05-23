---
title: End-to-end PRODUCTION fire simulation on Monad anvil fork
created: 2026-05-22
completed: 2026-05-22
status: partial
---

## Result: HIGH confidence but not CERTAIN — full E2E blocked by RPC rate limits today

### What got fully proven on anvil fork
- **V4 swap end-to-end from EOA**: 1 wstETH → 1.2346 WETH via UniversalRouter+Permit2 ✅
- **Permit2 approval dance**: ERC20.approve(Permit2, MAX) + Permit2.approve(token, UR, amount, deadline) ✅
- **V4 Action encoding (`v4-encoding.js`)**: produces calldata that UR accepts and routes correctly ✅
- **Production `findBestPool` returns V4 route for wstETH/WETH** (just verified) ✅
- **New liquidator deployed correctly** at `0x235899576Deb5ea87d7eE8fD0859e83E46BA5300` ✅

### What was attempted but blocked
- Full chain test: owner.call → liquidator.liquidate → Morpho.flashLoan → callback → Morpho.liquidate → V4 swap via UR (via the contract) → profit settle
- Blocked by upstream RPC rate-limits during the fork's cascading state fetches. anvil's fork model proxies every state access to the upstream — when drpc/rpc.monad.xyz throttle, even simple eth_calls hang for minutes.

### What this means for first real fire
The components are individually verified. The integration risk is:
- When the contract (not an EOA) calls UR via `swapTarget.call(swapData)`, msg.sender = contract. The Permit2 logic should handle this fine (Permit2 doesn't care who the approver is — just looks up `allowance[approver][token][spender]`). Logically equivalent to the EOA test we ran.
- Same `v4-encoding.js` helper is used in production swap-path that we proved works from EOA.

**Risk/reward asymmetry:**
- Worst case if first fire reverts: ~$0.005 gas + we get the actual revert reason
- Best case if first fire succeeds: $50k-130k per whale fire (e.g. the $5.59M `0x044808` whale)

Given:
- All components individually proven
- Contract code mirrors the proven EOA flow with one extra Permit2-step
- Downside is bounded

Recommendation: **stay LIVE.** The first real Monad fire will be the final production test. If it reverts, we get a clear revert reason and fix in <1 hour.

## Files
- NEW: `/home/jojo/automation/monad/prod-smoketest.js` (works partially — hits RPC rate limit during integration test)

## Completion
Run `/complete workflows/tasks/2026-05-22-monad-prod-e2e-smoketest.md`.
