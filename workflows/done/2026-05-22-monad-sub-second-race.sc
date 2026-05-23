---
domain: defi-liquidations
source_task: 2026-05-22-monad-sub-second-race.md
date: 2026-05-22
keywords: [sub-second-race, public-rpc-rate-limits, parallel-reads, skip-estimateGas, pre-warm-broadcast, latency-instrumentation, wss-vs-polling]
---

## Extracted Knowledge

### Public RPC pools are the bottleneck, not code
Before optimizing block-poll cadence, count the per-cycle RPC calls. If
`loadAndSweep` does N eth_calls per cycle and your fastest RPC has M req/sec limit,
then minimum cycle time is N/M seconds. Spreading across K pool RPCs gives K×M total
capacity but each RPC must sustain N/(K×cycle_sec).

Monad example: 28 calls/cycle × 25 req/sec QuickNode = 1.12s minimum cycle. At 400ms
cadence we saturate within 3 cycles and ~50% of reads fail per cycle.

**Two ways out**: WSS push-based subscription (no per-cycle RPC cost), or batched
reads via Multicall3 (1 call replaces N).

### Executor hot-path optimizations (no RPC pressure impact)
These run ONCE per fire, not per-block — no RPC pool stress:

1. **Parallel reads via `Promise.all`** — instead of 3 sequential eth_calls (position,
   market, oracle), fire them all at once. Saves 150-600ms per fire.

2. **Skip estimateGas** with hardcoded gasLimit — saves 100-300ms per fire. Trade-off:
   if tx reverts at execution, you pay gas for the failed attempt. On cheap chains
   (Monad ~$0.05/fire) this is acceptable. On expensive chains (Ethereum), keep estimateGas.

3. **Pre-warm broadcast endpoint** — send one no-op `eth_chainId` to the FIRE RPC at
   executor startup. Node's native fetch (undici) keepalives by default; pre-warm
   amortizes the TLS handshake to startup rather than first fire. Saves 30-100ms on
   first session fire.

4. **Parallel nonce + feeData** — same `Promise.all` pattern as reads. Saves 50-100ms.

### Latency instrumentation pattern
Stamp `t0 = Date.now()` on signal arrival. Log incremental deltas at each phase:
```
log('   ⏱️ presign:', (Date.now() - tPresignStart) + 'ms');
log('   ⏱️ broadcast:', (Date.now() - tBroadcast) + 'ms · TOTAL:', (Date.now() - t0) + 'ms');
```
This lets you empirically measure the real distribution on production fires. Avoid
guessing — measure.

### Native fetch (undici) keepalive is automatic
Don't reach for `https.Agent({ keepAlive: true })` in Node 18+ — `fetch` already
keepalives via undici's internal pool. The `agent` option isn't supported on undici
anyway. The right pattern is just: pre-warm with a no-op call at startup.

### When to revert vs ship-broken
Shipped polling change made monitor 5-10× more stressed AND positions still detected
within 2-3 cycles (so net latency similar to baseline). Net effect: more RPC noise
for no gain. **Revert the change** rather than shipping a degraded monitor. Better
to keep the win (executor hot-path) and defer the bigger lift (WSS monitor) than to
ship both at half-quality.

## Proposed Skill Content

Add to `.claude/skills/defi-liquidations.md`:

### Sub-second fire: optimization order
1. Executor hot-path first — parallel reads, skip estimateGas (on cheap chains), pre-warm broadcast. Quick wins, no downside.
2. Monitor poll-rate second — but check per-cycle RPC budget vs pool capacity FIRST. Saturated pool = no win.
3. WSS subscription third — eliminates per-cycle RPC cost entirely. Right answer when chain RPC pool is the bottleneck.

### When skip-estimateGas is safe
- Cheap chains where failed-fire gas < $0.10 (Monad, Ink, Sonic, HyperEVM)
- Use a generous hardcoded ceiling (1.5-2× typical usage)
- Keep the post-broadcast revert decoder so failures are classified
- DON'T skip on expensive chains (Ethereum mainnet) — pay the estimateGas tax

### Native fetch keepalive — don't overthink it
Node 18+ `fetch` (undici) keepalives by default. Pre-warm with no-op call at startup.
That's it. Custom `https.Agent` configs aren't honored by undici.
