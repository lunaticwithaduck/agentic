---
domain: polymarket-api
source_task: 2026-05-23-fix-probbrain-polymarket-fetcher.md
date: 2026-05-23
keywords: ["polymarket", "gamma-api", "bestBid", "bestAsk", "outcomePrices", "builder-attribution", "clob"]
---

## Extracted Knowledge

### Polymarket Gamma API price fields — which one is the "signal" price

The Gamma `/markets` response exposes two distinct price surfaces, and they tell you different things:

- **`outcomePrices`** — a JSON-string-encoded array, e.g. `"[\"0.6905\", \"0.3095\"]"`. This is *last-trade-ish* / cached state. It only updates when someone actually crosses the book. For a scanner that wants to react to live conditions, this lags — sometimes by hours on thin markets.
- **`bestBid` / `bestAsk`** — raw numbers (NOT strings), e.g. `0.673`, `0.708`. Live CLOB top-of-book. Mid = `(bid + ask) / 2` is the right "what's the market saying right now" signal.

**Rule:** signal scanners should prefer `(bestBid + bestAsk) / 2`, with `outcomePrices[0]` as the fallback when the book is empty.

**Sanity guard:** only trust the mid when `bestAsk >= bestBid > 0`. A crossed or zero book is garbage; fall back to `outcomePrices`. Without this, occasional CLOB glitches feed nonsense into downstream Kelly/calibration math.

### Polymarket builder attribution — applies to CLOB orders, not Gamma reads

Builder attribution on Polymarket is paid out for **trades you route through the CLOB with your `Builder-Api-Key` header on the order submission**. It's a kickback on order flow.

It does **not** apply to read-side traffic against `gamma-api.polymarket.com`. Sending a `POLY_BUILDER_API_KEY` or `Builder-Api-Key` header on `/markets` GET calls accomplishes nothing — the field is silently ignored, and you accrue zero attribution from reads.

If you see a scanner / fetcher with a builder-attribution comment around a read-side header, it's almost certainly a misunderstanding by whoever wrote it. Delete the header and the comment together — leaving the comment in place is worse than the broken code because it spreads the misconception.

### Binary market YES/NO parity must come from one source

For a binary market, `yes_price + no_price` must equal 1 (modulo small float rounding). If you mix sources — e.g., derive YES from the book mid but read NO from `outcomePrices[1]` — the pair can desync, since the book and the cached outcomePrices can diverge significantly on illiquid markets.

**Rule:** whenever YES comes from the book, set `no_price = 1 - yes_price` from the same source. Only when both prices come from `outcomePrices` should you read both fields directly (they'll be consistent because Polymarket updates them together).

## Proposed Skill Content

Add to the existing `polymarket-api` skill (or a future one) the following sections:

- **Price source hierarchy**: bestBid/bestAsk mid → outcomePrices[0] fallback; crossed-book guard; YES/NO parity rule
- **Builder attribution scope**: order-submission only, not Gamma reads; the field name on orders
- **Field type gotchas**: `outcomePrices` is a JSON-encoded string; `bestBid`/`bestAsk` are raw numbers; `volume`/`liquidity` exist both as strings and `Num` variants
