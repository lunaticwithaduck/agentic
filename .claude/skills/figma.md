---
name: figma
description: Design-to-code workflows using the Figma MCP server — extract design context, generate components, and stay in sync with designs
activation:
  keywords: ["figma", "design", "frame", "component", "design to code", "figma link", "figma url", "mockup", "wireframe", "dev mode", "design system", "figma mcp"]
  file_patterns: ["figma.config.*", "**/*.figma.*"]
---

# Figma MCP

## Setup

Two connection modes — pick one based on your workflow:

### Option A: Remote (OAuth) — Recommended
No desktop app required. Authenticates via Figma's OAuth flow.

```bash
# Project scope (this project only)
claude mcp add --transport http figma https://mcp.figma.com/mcp

# User scope (all your projects)
claude mcp add --scope user --transport http figma https://mcp.figma.com/mcp
```

Then authenticate inside Claude Code:
1. Run `/mcp` in Claude Code
2. Select **figma** → **Authenticate**
3. Allow access in the browser popup

### Option B: Desktop (Local)
Requires the Figma desktop app. No tokens needed — reads the currently open file.

```bash
claude mcp add --transport http figma-desktop http://127.0.0.1:3845/mcp
```

Prerequisites each session:
1. Open the Figma desktop app
2. Open a Design file → press `Shift+D` for Dev Mode
3. Enable **Desktop MCP server** in the Dev Mode panel

Or use the interactive setup script:
```bash
bash .claude/scripts/mcp-setup.sh
# Select option 3 (remote) or 4 (desktop)
```

---

## Working with Figma Designs

### Generate a component from a Figma frame

Paste the frame link and describe what you want:

```
Here is the Figma frame for the card component:
https://www.figma.com/design/XXXX/MyApp?node-id=123-456

Generate a React component that matches this design exactly.
Use Tailwind CSS for styling.
```

### Inspect a frame without generating code

```
What colors, typography, spacing, and layout does this Figma frame use?
https://www.figma.com/design/XXXX/MyApp?node-id=123-456
```

### Generate from currently selected frame (desktop mode)

With a frame selected in the Figma desktop app:

```
Generate a React component for the currently selected Figma frame.
Match the spacing, colors, and typography exactly.
```

---

## Best Practices

### Prompt patterns that work well

```
# Pixel-accurate implementation
Generate a [framework] component from this Figma frame: [url]
- Use [CSS approach] for styling
- Make it responsive at mobile (360px) and desktop (1280px)
- Add aria-label attributes matching the layer names in Figma

# Iterating on existing code
Here is my current [Component.tsx] and the Figma frame it should match: [url]
What needs to change to make the code match the design?

# Design token extraction
Extract all design tokens (colors, spacing, type scales) from this frame: [url]
Output them as CSS custom properties / Tailwind config / JS theme object.
```

### What Figma MCP provides to the model
- Layer names and hierarchy
- Exact measurements (width, height, padding, gap, border-radius)
- Color values (hex, rgba)
- Typography (font family, size, weight, line-height, letter-spacing)
- SVG icons and image assets
- Component annotations and notes

### Tips for clean output
- **Name your layers** in Figma — layer names become variable/prop names in generated code
- **Use Auto Layout** — it maps cleanly to flexbox/grid
- **Add annotations** in Dev Mode for behavior that isn't visible (hover states, interactions)
- **Select the right frame** — target the specific component, not the entire page

---

## Comparing Design vs Implementation

A useful pattern for design QA:

```
Here is the Figma design: [url]
Here is the current implementation: [paste component or file path]

List every visual difference between the design and the implementation,
grouped by: spacing, color, typography, layout.
```

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| "figma-desktop not responding" | Open Figma desktop, go to Dev Mode, re-enable the MCP server |
| "figma auth required" | Run `/mcp` in Claude Code and re-authenticate |
| Frame not found | Ensure you have view access to the Figma file |
| Generated code doesn't match | Provide the framework and CSS method explicitly in your prompt |
| Desktop server only sees old design | Re-select the frame or restart the MCP server toggle |
