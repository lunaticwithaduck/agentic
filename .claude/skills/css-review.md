---
name: css-review
description: Review CSS and styling code for quality, performance, and maintainability issues
activation:
  keywords: ["css review", "styling review", "css audit", "css quality", "style review"]
  file_patterns: ["**/*.css", "**/*.scss", "**/*.less", "**/*.styled.*", "**/*.module.css", "**/tailwind.config*"]
---

# CSS/Styling Code Review

## Purpose
Review CSS and styling code for common issues including specificity problems,
performance concerns, maintainability, and accessibility compliance.

## Instructions

1. **Specificity Issues**
   - Check for overly specific selectors (more than 3 levels deep)
   - Flag use of `!important` (except for utility classes)
   - Identify ID selectors used for styling (prefer classes)
   - Look for selector chains that fight each other
   - Recommend BEM, utility classes, or CSS Modules to reduce conflicts

2. **Unused Styles**
   - Identify CSS rules that do not match any elements
   - Check for dead code from removed features
   - Look for duplicated rule blocks
   - Flag vendor prefixes that are no longer needed

3. **Magic Numbers**
   - Flag hardcoded values without explanation (e.g., `margin-top: 37px`)
   - Recommend design tokens or CSS custom properties instead
   - Check for inconsistent spacing values (should follow a scale)

4. **Responsive Design Gaps**
   - Verify media queries cover necessary breakpoints
   - Check for fixed widths that break on small screens
   - Look for overflow issues (horizontal scroll)
   - Ensure text remains readable at all sizes

5. **Design Token Usage**
   - Verify colors use variables/tokens (not hardcoded hex values)
   - Check spacing follows a consistent scale
   - Ensure typography uses defined type scale
   - Look for inconsistent border-radius, shadows, transitions

6. **Accessibility Concerns**
   - Check that focus styles are visible and sufficient
   - Verify information is not conveyed by color alone
   - Ensure sufficient color contrast in all themes
   - Check that animations respect `prefers-reduced-motion`
   - Verify text is not clipped or hidden inaccessibly

7. **Performance**
   - Flag expensive selectors (universal *, deep nesting)
   - Check for properties that trigger layout thrashing
   - Look for large shadows or filters on frequently repainted elements
   - Verify animations use `transform` and `opacity` (GPU-accelerated)
   - Check for excessive use of `calc()` in hot paths

8. **Maintainability**
   - Verify consistent naming convention throughout
   - Check for logical property usage (for RTL support)
   - Ensure comments explain non-obvious decisions
   - Verify styles are co-located with their components

## Output Format

```
# CSS Review Report

## Critical Issues
- Issue, location, and recommended fix

## Improvements
- Suggested improvements with before/after examples

## Design Token Opportunities
- Values that should be extracted to tokens

## Summary
- Overall quality assessment and top 3 priorities
```
