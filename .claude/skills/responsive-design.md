---
name: responsive-design
description: Guide responsive design implementation with mobile-first approach and modern CSS patterns
activation:
  keywords: ["responsive", "mobile-first", "breakpoints", "media queries", "responsive design", "viewport"]
  file_patterns: ["**/*.css", "**/*.scss", "**/*.less", "**/*.styled.*", "**/*.module.css"]
---

# Responsive Design

## Purpose
Guide the implementation of responsive layouts that work seamlessly across
all device sizes using mobile-first methodology and modern CSS techniques.

## Instructions

1. **Mobile-First Approach**
   - Start with styles for the smallest viewport
   - Add complexity with `min-width` media queries as viewport grows
   - Ensure core content and functionality work without any media queries
   - Test on real devices, not just browser resize

2. **Define Breakpoints**
   - Use content-driven breakpoints, not device-specific ones
   - Common starting points (adjust to content):
     - Small: 640px
     - Medium: 768px
     - Large: 1024px
     - Extra large: 1280px
   - Define breakpoints as variables/tokens for consistency
   - Avoid too many breakpoints; 3-4 is usually sufficient

3. **Use Relative Units**
   - `rem` for font sizes and spacing (predictable scaling)
   - `%` or `fr` for layout widths
   - `vh`/`dvh` for viewport-relative heights
   - `ch` for text-width containers (60-80ch for readability)
   - Avoid `px` except for borders and fine details

4. **Flexible Layouts**
   - Use CSS Grid for two-dimensional layouts
   - Use Flexbox for one-dimensional alignment
   - Use `clamp()` for fluid typography: `clamp(1rem, 2.5vw, 1.5rem)`
   - Use `min()`, `max()` for fluid spacing
   - Leverage `auto-fit` and `minmax()` in grid for automatic responsiveness

5. **Optimize Images**
   - Use `srcset` and `sizes` for resolution switching
   - Use `<picture>` element for art direction
   - Implement lazy loading with `loading="lazy"`
   - Serve modern formats (WebP, AVIF) with fallbacks
   - Set explicit `width` and `height` to prevent layout shift

6. **Touch Targets**
   - Minimum 44x44px touch targets (WCAG 2.5.8)
   - Adequate spacing between targets (8px minimum)
   - Increase padding rather than overall element size
   - Consider thumb zones for mobile navigation

7. **Test Across Viewports**
   - Test at each breakpoint boundary
   - Test between breakpoints (not just at the exact values)
   - Verify with zoom at 200% and 400%
   - Check landscape and portrait orientations
   - Test with dynamic viewport units for mobile browsers

## Output Format

Provide responsive implementation including:
1. Breakpoint token definitions
2. Layout code using modern CSS (Grid/Flexbox)
3. Media query patterns for the specific use case
4. Image optimization markup
5. Testing checklist for the specific layout
