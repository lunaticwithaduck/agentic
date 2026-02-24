---
name: accessibility-audit
description: Audit codebase for WCAG 2.1 AA accessibility compliance
activation:
  keywords: ["accessibility", "a11y", "wcag", "screen reader", "aria", "accessibility audit"]
  file_patterns: ["**/*.html", "**/*.tsx", "**/*.jsx", "**/*.vue", "**/*.svelte"]
---

# Accessibility Audit

## Purpose
Evaluate the codebase for WCAG 2.1 AA compliance, identifying accessibility
barriers and providing actionable fixes for each issue found.

## Instructions

1. **Semantic HTML**
   - Verify correct heading hierarchy (h1-h6 in order, no skips)
   - Check for semantic elements: nav, main, article, section, aside, footer
   - Ensure lists use ul/ol/li, not styled divs
   - Verify tables have proper headers (th, scope, caption)

2. **ARIA Implementation**
   - Check that ARIA roles match element behavior
   - Verify aria-label and aria-labelledby on interactive elements
   - Ensure aria-live regions for dynamic content updates
   - Check aria-expanded, aria-selected, aria-checked on stateful elements
   - Confirm no redundant ARIA (e.g., role="button" on a button element)

3. **Keyboard Navigation**
   - Verify all interactive elements are reachable via Tab
   - Check logical tab order matches visual order
   - Ensure Enter/Space activate buttons and links
   - Verify Escape closes modals, dropdowns, and overlays
   - Check arrow key navigation in menus, tabs, and lists
   - Ensure no keyboard traps exist

4. **Focus Management**
   - Verify visible focus indicators on all interactive elements
   - Check focus moves to modals on open, returns on close
   - Ensure focus is not lost after dynamic content changes
   - Review custom focus styles meet 3:1 contrast ratio

5. **Color and Contrast**
   - Text contrast: 4.5:1 minimum for normal text, 3:1 for large text
   - UI component contrast: 3:1 against adjacent colors
   - Information is never conveyed by color alone
   - Check for sufficient contrast in all states (hover, focus, active)

6. **Images and Media**
   - All images have alt text (or empty alt="" for decorative)
   - Complex images have extended descriptions
   - Videos have captions and transcripts
   - Audio has transcripts

7. **Forms**
   - Every input has a visible, associated label
   - Error messages are descriptive and programmatically associated
   - Required fields are indicated (not just by color)
   - Form validation errors are announced to screen readers
   - Group related fields with fieldset/legend

8. **Navigation**
   - Skip navigation link is present and functional
   - Page has a descriptive title
   - Link text is meaningful (no "click here")
   - Current page is indicated in navigation

## Output Format

```
# Accessibility Audit Report (WCAG 2.1 AA)

## Score: X/Y criteria passing

## Critical Issues (blocks users)
- [ ] Issue description | File:line | WCAG criterion | Fix

## Major Issues (significant barriers)
- [ ] Issue description | File:line | WCAG criterion | Fix

## Minor Issues (improvements)
- [ ] Issue description | File:line | WCAG criterion | Fix

## Passing Criteria
- [x] List of criteria that pass

## Testing Recommendations
- Screen readers to test with
- Browser extensions to use
- Manual testing steps
```
