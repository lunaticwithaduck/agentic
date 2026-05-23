---
domain: defi-liquidations
source_task: 2026-05-22-monad-wss-multicall.md
date: 2026-05-22
keywords: [multicall3, wss-newheads, sub-second-race, morpho-batched-reads, aggregate3, rpc-pool-relief]
---

## Extracted Knowledge

### Multicall3 deployed at canonical address on every EVM chain
`0xcA11bde05977b3631167028862bE2a173976CA11` — same byte-for-byte address on
Ethereum, Base, Arbitrum, Optimism, Berachain, HyperEVM, Monad, Ink, Sonic. Always
check `eth_getCode` at this address before assuming you need a custom batching solution.

The contract exposes `aggregate3((address target, bool allowFailure, bytes callData)[])`
returning `(bool success, bytes returnData)[]`. Single eth_call replaces N
roundtrips with no parallelism overhead.

### WSS + Multicall3 = the sub-1-sec race architecture
The two binding constraints for liquidation race latency:
1. Block-detection lag (poll interval or WSS push delay)
2. Per-block state-read cost (N eth_calls × M ms each)

Solve BOTH:
- WSS `eth_subscribe newHeads` → block detection in 50-200ms after block production
- Multicall3 batched reads → N reads collapse to 1 call (~200ms for 22 positions on Monad)

On Monad we went from polling-driven 1000-4500ms to push-driven 650-1230ms.

### Morpho Blue watch-list batch layout
For W positions across N unique markets:
```
calls[0..N-1]:        Morpho.market(marketId_i)       — market state per market
calls[N..2N-1]:       oracle_i.price()                 — price per market
calls[2N..2N+W-1]:    Morpho.position(marketId, addr)  — per-borrower position
```
Total: 2N+W calls in one aggregate3 batch. For W=22, N=4: 30 calls in 1 RPC roundtrip.

After getting results, dedupe market state by marketId and compute HF locally with
the standard formula `(collateral × oraclePrice / 1e36) × lltv / 1e18 / borrowed`.

### When to refactor from polling to WSS+Multicall
Trigger conditions:
- Per-block eth_call count > 20
- Public RPC pool experiencing "rate limit" warnings even at conservative cadence
- Block time < poll interval (means we're missing block boundaries)

Monad hit all three. Felix/HyperLend already had Multicall but were polling — could
benefit from WSS migration too.

### dedup is essential with WSS push
WSS fires on every block (400ms cadence on Monad). Without dedup, the same position
would be armed dozens of times per minute. Keep a `Map<key, lastArmedAt>` and skip
re-arms within 30s. Executor consumes the armed file and removes it; if HF stays
below 1.0 past the dedup window, a fresh arm fires.

### Pre-warming WSS connection from systemd start
A WSS monitor service should NOT block waiting for first block — return from `main()`
immediately after `connectWSS()` and `setInterval` heartbeat. Otherwise systemd
considers startup slow and may issue Restart=on-failure churn.

## Proposed Skill Content

Add to `.claude/skills/defi-liquidations.md`:

### Multicall3 canonical address — always check first
`0xcA11bde05977b3631167028862bE2a173976CA11` deployed on every major EVM chain.
Test `eth_getCode` at this address — if 3808 bytes return, you have batching.
Saves 95%+ of RPC roundtrip overhead for monitor sweeps.

### WSS + Multicall monitor architecture for Morpho Blue chains
For chains with rate-limited public RPCs and fast block times (Monad, future Berachain,
etc.):
1. WSS `eth_subscribe newHeads` for block push
2. On each block: ONE `Multicall3.aggregate3(calls)` returning all watch-list state
3. Compute HF locally, dedup, write armed file

Reference: `monad/monitor-wss.js` (180 lines).

### Per-block-arm dedup
Without dedup, WSS push triggers same-position arm every block. 30s dedup prevents
file spam while allowing re-arm after executor consumed.
