---
name: skill-creator
description: Create new Claude Code skill files with proper structure and activation triggers
activation:
  keywords: ["create skill", "new skill", "skill creator", "add skill", "make skill"]
  file_patterns: ["**/.claude/skills/**"]
---

# Skill Creator

## Purpose
Create new Claude Code skill files that follow the standard template format
with appropriate activation triggers, clear instructions, and useful output formats.

## Instructions

1. **Gather Requirements**
   - Ask what the skill should help with
   - Identify the target audience (developers, reviewers, architects)
   - Determine if the skill is language-specific or generic
   - Understand the expected input and output

2. **Define Activation Triggers**
   - Choose 4-6 descriptive keywords that users would naturally use
   - Define file patterns that indicate relevance (e.g., `**/*.test.*`)
   - Ensure keywords do not overlap too much with existing skills
   - Use specific phrases over single generic words

3. **Write Clear Instructions**
   - Break the task into numbered steps
   - Each step should be specific and actionable
   - Include what to check, what to look for, what to produce
   - Order steps logically (analysis before implementation)
   - Include both the "what" and the "why" for each step
   - Keep total line count between 40-80 lines

4. **Define Output Format**
   - Specify the exact structure of the expected output
   - Include a template or example
   - Use markdown formatting for readability
   - Ensure the output is actionable, not just informational

5. **Follow Template Structure**
   Every skill file must have:
   ```markdown
   ---
   name: skill-name
   description: One-line description
   activation:
     keywords: ["keyword1", "keyword2"]
     file_patterns: ["**/*.ext"]
   ---
   # Skill Title
   ## Purpose
   ## Instructions
   ## Output Format
   ```

6. **Validate the Skill**
   - Verify the YAML frontmatter is valid
   - Ensure instructions are clear without prior context
   - Check that output format matches what instructions produce
   - Confirm keywords are discoverable and unambiguous

## Output Format

Generate the complete skill file content in markdown with YAML frontmatter.
The file should be saved to `.claude/skills/skill-name.md`.

```
File: .claude/skills/<skill-name>.md
Content: [complete skill file]
```

Confirm the skill was created and summarize its purpose and triggers.
