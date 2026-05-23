---
domain: hyperevm
source_task: 2026-05-20-fix-shared-wallet-nonce.md
date: 2026-05-20
keywords: ["nonce", "mutex", "wallet-lock", "race condition", "hyperevm", "executor", "presign"]
---

## Extracted Knowledge

### Shared-wallet nonce collision in multi-bot setups
When multiple executor processes share one EOA (e.g. HyperLend + HypurrFi both signing as `0x8Defac…` on HyperEVM):
- Both processes call `provider.getTransactionCount(addr, 'pending')` at presign time
- If the first executor's tx hasn't yet propagated to the RPC's mempool view, the second gets the same nonce
- First broadcasts → succeeds. Second broadcasts → rejected with `nonce too low`. Silent fire loss; user thinks bot is healthy.

### File-based mutex pattern (Node stdlib, no deps)
```js
fs.mkdirSync(lockPath, { recursive: false })  // atomic POSIX; EEXIST = locked
```
- Acquire: poll mkdirSync with 75ms backoff until success
- Stale detection: if existing lock's mtime > 30s old, force-rmdir and retry
- Release: rmdirSync in finally block
- Always pass through a `withLock(fn)` wrapper so the release is guaranteed even on throw

### Critical-section boundary
Wrap only `presign + pre-flight + broadcast`. **Move `waitForTransaction` OUTSIDE the lock** — confirmation can take 60s and shouldn't block the sibling executor. The lock's job is to serialize nonce-claiming + broadcast acceptance, not the confirmation tail.

### Test the lock with a deliberate concurrent pair
```js
await Promise.all([
  withLock(slowWork(500)),
  new Promise(r => setTimeout(r, 50)).then(() => withLock(fastWork(200))),
]);
// total elapsed ≈ 700ms (serialized) — not 500ms (parallel)
```
If elapsed is closer to max() than sum(), the mutex isn't working.

## Proposed Skill Content
This belongs in the existing `hyperevm` skill under a new section "Multi-bot wallet coordination":
- When two HyperEVM bots share an EOA (HyperLend + HypurrFi pattern), serialize the presign + broadcast critical section via a file-based mutex
- The mutex MUST exclude `waitForTransaction` — confirmation can take 60s and shouldn't block siblings
- Stale-lock detection (mtime > 30s) prevents deadlock if owner crashes
- Integration-test with two concurrent calls offset by 50ms; total elapsed should be sum, not max
