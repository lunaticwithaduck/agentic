---
title: Harden HyperEVM executors — close audit gaps before sustained live operation
created: 2026-05-20
completed: 2026-05-20
status: done
---

## Goal
Apply four hardenings to both HyperLend + HypurrFi executors based on the post-go-live audit. None are correctness-critical (pre-flight catches everything), but each closes a specific failure mode or reduces Telegram noise / wasted gas / silent stalls.

## Steps
- [x] **Re-verify HF inside presignLiquidation** — fresh `getUserAccountData` query at fire time; abort silently if HF >= 1e18
- [x] **Bump gasLimit + gas-estimate pre-flight** — `provider.estimateGas` first; skip if >1.9M (HyperEVM small-block cap is 2M); set `gasLimit = estimate × 1.15`
- [x] **Pick collateral by USD value, not raw balance** — multiply balance × oracle price → sort; skip Pendle-PT + beHYPE
- [x] **Wallet balance watcher** — hourly `eth_getBalance` check on signer; Telegram alert when < 0.005 HYPE (6h dedup)
- [x] Validate via `test-preflight.js` against current whales on both chains — same Aave HF-gate revert as before, no regression
- [x] Restart executors + hyperlend-monitor — all 12 services active

## Outcome
Completed 2026-05-20. All four hardenings applied to both HyperLend + HypurrFi executors. Validation: `test-preflight.js` against HypurrFi whale (HF 1.30) returns "code 45"; HyperLend whale (HF 1.10) returns custom selector `0x930bb771` — both are the same Aave HF-gate semantic and confirm the encoding remains correct. Balance watcher fires its first read at monitor startup: signer at 0.012634 HYPE (above 0.005 threshold, no alert).

**Operational impact:**
- Telegram revert spam expected to drop near-zero (fresh-HF check filters out healed-back-up positions before the broadcast pre-flight runs)
- Cross-asset collateral selection now USD-aware (no more dusty-USOL-over-UBTC failure mode)
- Gas estimation prevents the rare 1.9M+ swap path from broadcasting and reverting on small-block cap
- 6 fires of wallet headroom; now monitored hourly with auto-Telegram at 0.005 HYPE threshold

**Notable non-hardening:** explicitly chose NOT to enable HyperEVM big-block opt-in for the race wallet. Big blocks have a 60-second confirmation interval — fatal for racing. The 1.9M gas cap + skip behavior is the correct tradeoff for race-tier liquidations.

## Completion
Run `/complete workflows/tasks/2026-05-20-harden-hyperevm-executors.md`.
