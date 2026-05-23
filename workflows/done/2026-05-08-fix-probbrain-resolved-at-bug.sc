---
domain: polymarket-api
source_task: 2026-05-08-fix-probbrain-resolved-at-bug.md
date: 2026-05-08
keywords: ["polymarket", "gamma-api", "uma", "resolved_at", "prediction market", "umaEndDate"]
---

## Extracted Knowledge

### Polymarket gamma-api `/markets` endpoint — resolution timestamp fields

Choosing the right field for "when did this market actually resolve?" is non-obvious
and the wrong choice silently produces dates that can be off by months.

| Field | Format | What it actually is |
|---|---|---|
| `endDate` | ISO-8601 with `Z` (`2026-06-30T23:55:00Z`) | The market's **scheduled deadline**. NOT the resolution time. Markets routinely settle earlier via UMA dispute. |
| `endDateIso` | Date-only (`2026-06-30`) | Convenience date version of `endDate`. Same caveat. |
| `umaEndDate` | ISO-8601 with `Z` (`2026-05-01T23:09:26Z`) | **Actual UMA settlement timestamp** — the field you want for "resolved at". |
| `closedTime` | Postgres-style (`'2026-05-01 23:09:26+00'`) — space separator, `+00` not `+00:00`/`Z` | Same instant as `umaEndDate` but in postgres TIMESTAMPTZ string form. NOT directly usable as ISO-8601 — needs parsing before re-serialization. |
| `closed` | bool | Whether the market is settled. |
| `resolved` | bool | Same idea; both must be checked depending on Polymarket version. |
| `resolutionTime` | — | **Does not exist on `/markets`.** Common bad guess. `market.get("resolutionTime")` returns None. |
| `outcomePrices` | Stringified JSON array (`'["1","0"]'`) — string, not list | Index 0 = YES price, index 1 = NO. Must be `json.loads`'d. After resolution, the winner is `"1"` and the loser is `"0"`. |
| `umaResolutionStatuses` | List (`["proposed", "disputed", "proposed"]`) | UMA dispute lifecycle. Presence of `"disputed"` tells you the market resolved adversarially, often well before `endDate`. |

### Polymarket-specific behavioral facts

- **Markets can settle before `endDate`.** UMA's optimistic oracle lets resolvers propose
  an outcome any time the resolution criteria are objectively met. SIG-067 (`Trump
  announces end of military operations against Iran by June 30th?`) had `endDate
  2026-06-30T23:55Z` but `closedTime 2026-05-01T23:09:26Z` — a ~60-day early call.
  Code that uses `endDate` for resolved markets will be wildly wrong on these.

- **Slug lookups stop returning resolved markets.** After settlement, the
  `gamma-api.polymarket.com/markets?slug=...` query may return `[]`. Use the numeric
  market_id form `gamma-api.polymarket.com/markets/{id}` instead — that keeps working.

- **Polymarket's `:55` minute convention.** Many `endDate` values end in `T23:55:00Z`,
  not `T23:59:59Z` or midnight. Don't assume midnight when synthesizing fallback timestamps.

### Common bug pattern

```python
# BUG: resolutionTime is not a real field, falls through to deadline
resolved_at = (
    market.get("resolutionTime")
    or market.get("endDate")
    or datetime.now(timezone.utc).isoformat()
)
```

```python
# CORRECT: umaEndDate is the actual settlement instant
resolved_at = (
    market.get("umaEndDate")
    or market.get("endDate")  # only as last-resort approximation
    or datetime.now(timezone.utc).isoformat()
)
```

## Proposed Skill Content

A `polymarket-api` skill should cover:

1. **Field hierarchy for resolution time** (the table above) — primary knowledge.
2. **`outcomePrices` is a stringified JSON array** — most common Polymarket gotcha,
   trips up every first-time integrator.
3. **Slug vs market_id stability** — slugs disappear post-resolution, market_ids don't.
4. **UMA early-resolution behavior** — `umaResolutionStatuses` lifecycle, why
   timestamps drift from `endDate`.
5. **`closedTime` is postgres format, not ISO-8601** — parse before reuse.
6. **Activation keywords:** polymarket, gamma-api, prediction market, umaEndDate, resolved_at, outcomePrices.
