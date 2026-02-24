---
name: storybook
description: Create Storybook stories for UI components following CSF3 format
activation:
  keywords: ["storybook", "story", "stories", "csf3", "component story"]
  file_patterns: ["**/*.stories.*", "**/.storybook/**", "**/storybook-*"]
---

# Storybook Story Creation

## Purpose
Create comprehensive Storybook stories for UI components that document all
states, enable interactive testing, and serve as living documentation.

## Instructions

1. **Identify Component States**
   - Default/happy path state
   - Loading state (skeleton, spinner)
   - Error state (with error message)
   - Empty state (no data)
   - Populated state (with realistic data)
   - Disabled state
   - Interactive states (hover, focus, active)
   - Edge cases (long text, many items, special characters)

2. **Create Meta Configuration (CSF3)**
   - Define the component meta with title following folder hierarchy
   - Set up argTypes for each prop with controls
   - Configure decorators for context providers, layout wrappers
   - Add component-level parameters (layout, backgrounds)
   - Include JSDoc or description for the component

3. **Write Individual Stories**
   - One story per meaningful state
   - Use descriptive story names (not "Story1")
   - Define args for each story to show prop variations
   - Use `play` functions for interaction testing
   - Group related stories with naming conventions

4. **Add Controls and Args**
   - Map props to appropriate control types (text, select, boolean, number)
   - Set default args at the meta level
   - Override specific args per story
   - Use argTypes to constrain options for enum-like props
   - Hide internal props from controls panel

5. **Write Documentation**
   - Add a Docs page or use autodocs
   - Include usage examples with code snippets
   - Document do's and don'ts
   - Show composition patterns with other components
   - Note accessibility considerations

6. **Accessibility Testing**
   - Enable the a11y addon in story parameters
   - Add specific a11y rules to disable false positives (with justification)
   - Include stories that test keyboard navigation via play functions
   - Test high-contrast and forced-colors modes

7. **Organize Story Hierarchy**
   - Group by feature or atomic design level
   - Use consistent naming: `Category/Component`
   - Place documentation stories first
   - Order states logically: default, variations, states, edge cases

## Output Format

Provide complete story files in CSF3 format:

```typescript
import type { Meta, StoryObj } from '@storybook/framework';
import { ComponentName } from './ComponentName';

const meta: Meta<typeof ComponentName> = {
  title: 'Category/ComponentName',
  component: ComponentName,
  tags: ['autodocs'],
  argTypes: { /* controls */ },
};
export default meta;

type Story = StoryObj<typeof ComponentName>;

export const Default: Story = { args: { /* default props */ } };
export const Loading: Story = { args: { isLoading: true } };
export const Error: Story = { args: { error: 'Something went wrong' } };
export const Empty: Story = { args: { items: [] } };
```

Include play functions for stories that need interaction testing.
