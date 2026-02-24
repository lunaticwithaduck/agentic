---
name: component-design
description: Guide UI component design with proper patterns, accessibility, and state management
activation:
  keywords: ["component design", "ui component", "component architecture", "component pattern", "design component"]
  file_patterns: ["**/components/**", "**/*.component.*", "**/*.tsx", "**/*.vue", "**/*.svelte"]
---

# UI Component Design

## Purpose
Guide the design and implementation of reusable, accessible, and well-structured
UI components following established patterns and best practices.

## Instructions

1. **Analyze Requirements**
   - Clarify the component's purpose and responsibilities
   - Identify the data it needs (props/inputs) and events it emits
   - Determine where it fits in the component hierarchy
   - List all states: default, loading, error, empty, disabled

2. **Choose Component Pattern**
   - **Presentational vs Container**: separate display from logic
   - **Controlled vs Uncontrolled**: decide who owns the state
   - **Compound Components**: for related component groups (tabs, accordions)
   - **Render Props / Slots**: for flexible content injection
   - Favor composition over inheritance in all cases

3. **Define Props Interface**
   - Use strict typing for all props
   - Provide sensible defaults where appropriate
   - Document each prop with description and constraints
   - Keep the API surface small; prefer composition for variants
   - Use consistent naming conventions (onX for callbacks, isX for booleans)

4. **Handle State Management**
   - Keep state as close to where it is used as possible
   - Lift state only when necessary for sibling communication
   - Derive computed values instead of storing redundant state
   - Handle async state transitions explicitly

5. **Implement Accessibility**
   - Use semantic HTML elements as the foundation
   - Add ARIA roles, labels, and descriptions where semantics fall short
   - Ensure keyboard navigation (Tab, Enter, Escape, Arrow keys)
   - Manage focus correctly on open/close/navigate actions
   - Support screen readers with live regions for dynamic content

6. **Responsive Design**
   - Design mobile-first, enhance for larger viewports
   - Use relative units for sizing and spacing
   - Handle touch and pointer interactions appropriately
   - Test at common breakpoints

7. **Handle All States**
   - Loading: skeleton or spinner with accessible announcement
   - Error: clear message with retry action
   - Empty: helpful message with call to action
   - Disabled: visual indicator with aria-disabled
   - Overflow: handle long text and large datasets

## Output Format

Provide the component design as:
1. Props/API interface definition with types and descriptions
2. State diagram showing all possible states and transitions
3. Implementation code with inline comments
4. Usage examples showing common scenarios
5. Accessibility checklist specific to this component
