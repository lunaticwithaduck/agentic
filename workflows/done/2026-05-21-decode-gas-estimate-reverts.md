---
title: Decode revert selectors in gas-estimate error path
created: 2026-05-21
completed: 2026-05-21
status: done
---

## Goal
The gas-estimate skip path was surfacing ethers v6's fuzzy `e.shortMessage` ("execution reverted (unknown custom error)") which hid the actual revert selector. Route revert data through the existing `revertDecoder`.

## Steps
- [x] Update gas-estimate `catch (e)` block in both executors to call `revertDecoder.decode(e)`
- [x] If `expected=true`: silent log + quieter tgSkip reason
- [x] If `expected=false`: log + Telegram with the raw selector so we can grow the decoder table
- [x] Lint + restart

## Trigger event
User `0x85b9899118fac92ca10a429361a13161af63f34e` armed when HF briefly dipped below 1.02. By the time `estimateGas` ran, HF had ticked back above 1.0 (currently 1.2066). Aave reverted with a custom selector that wasn't in our decoder table — Telegram showed "(unknown custom error)" with no actionable info.

## Outcome
Completed 2026-05-21. Next time a gas-estimate revert happens, the Telegram message will include the raw 4-byte selector and the decoded name (if known). Format:
- Known/expected: `⏭️ HyperLend skip 0xabc… — health-factor-ok` (silent kind)
- Known/actionable: `⏭️ HyperLend skip 0xabc… — gas-estimate revert: ReserveFrozen() [0x6d305815]`
- Unknown: `⏭️ HyperLend skip 0xabc… — gas-estimate revert: unknown-selector [0xdeadbeef]`

When unknown selectors appear, we can add them to `/home/jojo/automation/lib/revert-decoder.js` `SELECTOR_TABLE` and they'll classify correctly on the next occurrence. Race-between-arm-and-fire flicker (the most common cause) will now show as `health-factor-ok` and dedup with the same kind.

## Completion
Run `/complete workflows/tasks/2026-05-21-decode-gas-estimate-reverts.md`.
