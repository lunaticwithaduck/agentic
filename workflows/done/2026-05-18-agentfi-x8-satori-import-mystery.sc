---
domain: satori
source_task: 2026-05-18-agentfi-x8-satori-import-mystery.md
date: 2026-05-18
keywords: ["satori", "next/og", "ImageResponse", "trim undefined", "style object", "react.CSSProperties"]
---

## Extracted Knowledge

### `undefined` in inline style objects is the #1 satori crash
The error message:
```
TypeError: Cannot read properties of undefined (reading 'trim')
    at ignore-listed frames
```

Root cause: TypeScript's `React.CSSProperties` type accepts `string | number | undefined` for most properties. Code like:
```ts
const style: React.CSSProperties = {
  width: 1200,
  height: isOg ? 630 : undefined,        // ← present, but undefined
  minHeight: isOg ? undefined : 340,     // ← present, but undefined
};
```
passes type checks. At runtime the keys exist in the JS object with the value `undefined`. Satori iterates style keys and calls `.trim()` on the value → crash.

**This is invisible to React's render** — React handles `undefined` style values fine, skipping them. Only satori's stricter parser crashes. So `renderToStaticMarkup` tests pass, browser pages render correctly, vitest tests pass, but the OG route crashes when actually consumed.

### Fix: conditional spread
Convert ternary-with-undefined to conditional spread so undefined keys are NEVER PRESENT in the final object:
```ts
// ❌ BROKEN
const style: React.CSSProperties = {
  width: 1200,
  height: isOg ? 630 : undefined,
  minHeight: isOg ? undefined : 340,
};

// ✅ Satori-safe
const style: React.CSSProperties = {
  width: 1200,
  ...(isOg ? { height: 630 } : { minHeight: 340 }),
};
```

This applies to ANY mode-conditional style property, not just sizing: `padding: isOg ? 48 : undefined`, `border: showFrame ? "1px solid red" : undefined`, etc. The pattern is universally fixable with conditional spread.

### Why "imported component vs inlined JSX" looked different
JSX literals like `<div style={{width: 1200}}>` never include unwanted keys — you don't write `style={{height: undefined}}`. Programmatic style objects built from props/conditionals do. That's why the same React tree appeared to behave differently: the inlined version's style objects didn't have undefined keys, the component's computed style objects did. Same renderer, different inputs.

### Debugging signature
If you see `Cannot read '...trim()'` from satori and ALL of:
- All CSS properties are in the allowed subset (no `inline-block`, no `position:absolute/relative`, no `flexBasis:"auto"`)
- The component renders fine via `renderToStaticMarkup`
- Inlining the JSX in the route works
- Importing the component fails

→ Grep the imported component's source for `: undefined` in style objects:
```bash
grep -n ": undefined" components/<ComponentName>.tsx
```
Any hit is a candidate. Apply the conditional-spread fix.

## Proposed Skill Content

Already added to `.claude/skills/satori.md` as a dedicated "NEVER put undefined values in inline style objects" section plus a Failure Modes entry. Future synthesis runs pulling from this `.sc` should keep the section verbatim.

## Failure Modes Observed

**X6's `.sc` claim that "imported component vs inlined JSX can behave differently in satori (open mystery)" — RESOLVED.** The root cause was `undefined`-valued keys in the component's programmatic style object. The "mystery" was actually a deterministic bug with a specific signature. X6's `.sc` and the corresponding section in `satori.md` have been updated to reflect the resolution rather than leaving readers chasing the wrong hypothesis (imports vs compilation).
