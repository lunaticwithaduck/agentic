---
domain: satori
source_task: 2026-05-18-agentfi-x24-og-live-constitution.md
date: 2026-05-18
keywords: ["playwright", "webserver", "env", "byte-snapshot", "visual-regression", "deterministic"]
---

## Extracted Knowledge

### `playwright.config.ts.webServer.env` for visual-regression determinism

When Playwright spawns `pnpm start` (or any webServer command), it can set env vars that the server-under-test reads:

```ts
webServer: {
  command: "pnpm start",
  url: BASE_URL,
  env: {
    PIN_CONSTITUTION_SNIPPET: "Accumulate compute until 0.5 DIEM/day; then build.",
  },
},
```

Use this when any data source the page reads (live fetch, random IDs, system clock) would otherwise cause byte-snapshot drift.

### Visual-regression determinism checklist

For byte-comparable PNG snapshots from `next/og` / satori, EVERY source of non-determinism must be pinned:

1. **`Date.now()`** — pass a `nowMs` prop from the route handler with a fixed timestamp
2. **Network fetches** — short-circuit via env-driven pins (e.g. `if (process.env.PIN_X) return process.env.PIN_X`)
3. **Random IDs / UUIDs** — generally not needed in OG cards; if you have one, replace with a deterministic seed
4. **Cache state** — fresh cold caches between runs OR pin the cached value

The byte snapshot fails noisily on any drift, which is the point — but the failure must be intentional (you actually changed something) not incidental (upstream README changed).

### Truncate-on-word-boundary one-liner

For text destined for a fixed-width surface (OG cards, table cells, badges):

```ts
function truncateSnippet(s: string, max: number): string {
  if (s.length <= max) return s;
  const cut = s.slice(0, max);
  const lastSpace = cut.lastIndexOf(" ");
  return (lastSpace > 0 ? cut.slice(0, lastSpace) : cut) + "…";
}
```

Beats `.slice(0, n) + "…"` visually because it doesn't break mid-word. The `lastSpace > 0` guard handles the edge case of a single super-long word with no spaces in the first `max` chars.

### Optional satori props pattern

When extending an existing satori-safe component with a new optional prop:

```tsx
export interface AgentCardProps {
  /* existing props */
  /** Only rendered in og mode */
  constitutionSnippet?: string;
}

// in the component:
const snippet = constitutionSnippet && constitutionSnippet !== "—"
  ? truncateSnippet(constitutionSnippet, 110)
  : null;

{isOg && snippet && (
  <div style={{ display: "flex", fontSize: 14, fontStyle: "italic", /* ... */ }}>
    "{snippet}"
  </div>
)}
```

Three guards stack to keep the layout stable:
1. Optional prop (back-compat for existing call sites)
2. Truth check (handles empty string + the `"—"` fallback marker)
3. Mode guard (only renders in og — the live page has its own constitution surface in the sidebar)

## Proposed Skill Content

Extends `.claude/skills/satori.md`. Add a "Visual-regression determinism" section:
- The full pin checklist (Date.now / network / random / cache)
- `playwright.config.ts.webServer.env` pattern with example
- Per-data-source env-pin pattern (`if (process.env.PIN_X) return process.env.PIN_X`)
- Optional-prop guard pattern when extending an existing card with mode-gated content
