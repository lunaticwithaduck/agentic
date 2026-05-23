---
title: Fix shared-wallet nonce collision between HyperLend + HypurrFi executors
created: 2026-05-20
completed: 2026-05-20
status: done
---

## Goal
Both HyperEVM executors use the same EOA `0x8Defac…`. They run in separate processes. If a HyperLend fire is in-flight and a HypurrFi fire arms within ~1s, both `getTransactionCount('pending')` queries can return the same nonce → second broadcast fails with "nonce too low" → we silently lose the second fire.

## Steps
- [x] Add a file-based mutex `/tmp/hyperevm-wallet.lock`
- [x] Wrap presign + broadcast critical section in both executors with the lock
- [x] Lock timeout: 30s; stale-lock auto-cleanup via mtime check
- [x] Restart both executors
- [x] Validation: integration test confirmed serialization (parallel locks waited 453ms as expected)

## Implementation
New shared module `/home/jojo/automation/lib/wallet-lock.js`:
- `mkdirSync` for atomic acquire (POSIX). EEXIST = held by other process; poll every 75ms.
- Stale-lock check via mtime — if older than 30s, force-remove (covers crashed process).
- `withLock(fn)` wrapper; auto-releases on completion or throw via try/finally.
- `waitedMs` returned to caller so we can log non-trivial waits.

Both executors now wrap `presign + pre-flight + broadcast` in `walletLock.withLock`. Confirmation wait (`provider.waitForTransaction`) runs *outside* the lock so the sibling can broadcast in parallel.

## Outcome
Completed 2026-05-20. Integration test:
```
A entered (waited 1ms) → A done
B entered (waited 453ms) → B done
total elapsed: 705 ms (~ A's 500ms + B's 200ms, serialized)
```
Confirms the mutex correctly serializes between concurrent callers. Both executors restarted cleanly in 🟢 LIVE mode. Next fire from either chain will exercise the lock for the first time in production.

## Completion
Run `/complete workflows/tasks/2026-05-20-fix-shared-wallet-nonce.md`.
