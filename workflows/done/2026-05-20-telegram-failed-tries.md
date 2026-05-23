---
title: Telegram alerts for failed/skipped liquidation attempts
created: 2026-05-20
completed: 2026-05-20
status: done
---

## Goal
Currently Telegram fires only on broadcast outcomes (FIRING, WON, reverted) and on unexpected pre-flight reverts. Many skip paths were silent. User wants visibility on ALL evaluated outcomes during the initial monitoring phase.

## Steps
- [x] Add `tgSkip(user, reason)` helper to both executors with 5-min dedup by (user, reason)
- [x] Wire into 5 skip paths: HF healed, no debt/coll, only unswappable coll, no viable pair, LIF=0, gas-estimate revert, gas > 1.9M cap
- [x] Use `⏭️` prefix for skip messages (distinct from 🔥/🎉/💀/🛑 existing meanings)
- [x] Lint + restart both executors

## Outcome
Completed 2026-05-20. Skip paths now surface to Telegram with concise one-liners:
```
⏭️ HypurrFi skip 0xb5c46131… — HF healed to 1.305
⏭️ HyperLend skip 0x095c9387… — no viable pair (1 tried)
⏭️ HypurrFi skip 0x… — LIF=0 for USDe
⏭️ HyperLend skip 0x… — gas 2100000 > 1.9M cap
⏭️ HyperLend skip 0x… — only unswappable collateral
```

5-min dedup per `(user, reason)` keeps a flapping HF position from spamming. Existing emojis preserved:
- 🔥 FIRING — broadcast in progress
- 🎉 WON — tx confirmed status=1
- 💀 reverted — tx confirmed status=0
- 🛑 pre-flight revert (actionable, not auto-classified as expected)
- ⏭️ skip — new, all silent-skip paths
- ⚠️ balance low — periodic gas watcher alert

Note: this reverses the earlier "actions + errors only" policy because "failed tries" turned out to be high-signal during the bring-up phase. Once we've seen patterns stabilize, can dial back via a `EXECUTOR_VERBOSE_SKIPS=0` env var if desired.

## Completion
Run `/complete workflows/tasks/2026-05-20-telegram-failed-tries.md`.
