---
title: Fix ProbBrain Polymarket fetcher — price source + bogus builder header
created: 2026-05-23
completed: 2026-05-23
status: done
---

## Goal
The Polymarket Gamma fetcher at `/home/jojo/Documents/ProbBrain/scanner/polymarket.py` had two correctness issues for use as a live signal scanner:

1. **Builder attribution header is misconceived.** It sent `POLY_BUILDER_API_KEY` on Gamma read calls, with a comment claiming it "attributes scanned traffic to ProbBrain for weekly USDC rewards." Polymarket's builder attribution is a CLOB **order-submission** feature (header is `Builder-Api-Key` on order placement), not a Gamma read-side feature. The header is silently dropped by Gamma; no rewards accrue from market reads.
2. **Stale price source.** Used `outcomePrices[0]` only (last-trade-ish from Gamma). The code comment said "YES price comes from outcomePrices[0] or bestBid/bestAsk" but the bestBid/bestAsk fallback was never wired. For a signal scanner this means triggers lag the actual book.

Fix: remove the dead builder header + comment; prefer Gamma's `bestBid`/`bestAsk` (mid) when present, fall back to `outcomePrices[0]`.

## Steps
- [x] Remove `BUILDERS_API_KEY` env var, header send, and the misleading comment
- [x] Wire `bestBid`/`bestAsk` → mid as the primary YES price source; `outcomePrices[0]` becomes the fallback
- [x] Keep `_parse_price` clamping semantics so any malformed value still returns a sane number
- [x] Verify by importing the module and parsing a synthetic market dict with both `bestBid`/`bestAsk` and `outcomePrices` set

## Outcome

Completed on 2026-05-23. Removed the phantom `BUILDERS_API_KEY` env var, header injection, and unused `os` import. Switched the YES price source to mid `(bestBid + ask) / 2` when the book is sane (`ask >= bid > 0`), falling back to `outcomePrices[0]` when the book is empty or crossed. Made `no_price` derive from `1 - yes` whenever yes came from the book, so the pair stays consistent (was previously a latent bug: yes from book + no from stale `outcomePrices[1]` could desync). Verified four cases by importing `_parse_market`: book-present uses mid (0.6905 vs stale 0.50 outcomePrices); book-absent falls back; crossed book falls back; empty input defaults to 0.5. File compiles clean.

**Skill candidate evaluation:**
- Technologies/frameworks touched in this task: Polymarket Gamma API, Polymarket CLOB API (builder attribution), Python httpx
- Domain-specific knowledge involved (concrete facts, patterns, anti-patterns):
  - Gamma `outcomePrices` is JSON-string-encoded last-trade-ish state, lags the book until someone crosses
  - Gamma `bestBid`/`bestAsk` are raw numbers (not strings); mid is the right signal-scanner source
  - Polymarket builder attribution applies to CLOB order submission (`Builder-Api-Key` header on orders), NOT to Gamma reads — sending it on `/markets` does nothing
  - Binary YES/NO parity (must sum to 1) requires both prices to come from the same source; mixing book mid with stale `outcomePrices[1]` desyncs them
  - Crossed book sanity guard: only trust mid when `ask >= bid > 0`
- Verdict: GENERATE
- Reason: Non-obvious Polymarket-specific gotchas (where builder attribution actually applies, which fields are strings vs numbers, when to trust the book vs the cached prices) — exactly the kind of knowledge a future task would re-derive painfully.
