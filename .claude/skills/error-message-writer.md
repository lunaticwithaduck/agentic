---
name: error-message-writer
description: Write clear, actionable error messages that help users understand and fix problems
activation:
  keywords: ["error message", "error text", "user-facing error", "error copy", "error wording"]
  file_patterns: ["**/errors/**", "**/messages/**", "**/i18n/**", "**/locales/**"]
---

# Error Message Writer

## Purpose
Write clear, actionable error messages that tell users what happened,
why it happened, and how to fix it. Replace cryptic or vague errors
with human-friendly messages.

## Instructions

1. **Identify the Error Condition**
   - What operation was the user attempting?
   - What specifically went wrong?
   - What is the system state now?
   - Is this a user error, system error, or external dependency error?

2. **Write the Error Message**
   Follow this pattern: **"Could not [action] because [reason]. [Solution]."**

   - **What happened**: state the failed action in the user's terms
   - **Why it happened**: explain the specific cause (not the technical exception)
   - **How to fix it**: give a concrete, actionable next step

3. **Be Specific**
   - Include actual values: "File 'report.csv' not found" not "File not found"
   - Include limits: "Username must be 3-20 characters (yours is 2)"
   - Include context: "Could not save changes to 'Project Alpha'"
   - State the constraint: "Email must contain @ symbol"

4. **Be Actionable**
   - Tell the user exactly what to do: "Check the file path and try again"
   - Provide alternatives: "Try signing in with your email instead"
   - Link to help when applicable: "See docs.example.com/auth for setup"
   - For system errors: "This is on our end. Try again in a few minutes."

5. **Be Human**
   - Use plain language, not jargon or error codes alone
   - Write in sentence case, not ALL CAPS
   - Do not blame the user ("Invalid input" -> "This field needs a number")
   - Keep it brief but not cryptic
   - Use contractions naturally (can't, won't, isn't)

6. **Handle Different Severities**
   - **Blocking errors**: clear action to unblock
   - **Warnings**: explain risk but let user proceed
   - **Info**: note what happened, no action needed
   - **Validation**: explain exactly what is wrong with the input

7. **Error Message Anti-Patterns to Avoid**
   - "An error occurred" (says nothing)
   - "Error: 500" (meaningless to users)
   - "Invalid input" (which input? what is wrong?)
   - "Something went wrong" (what? how to fix?)
   - "Contact support" as the only guidance
   - Technical stack traces shown to end users
   - Inconsistent tone across the application

## Output Format

For each error condition, provide:

```
## Error: [condition name]

### Before (bad)
> [existing unclear message]

### After (good)
> [improved message]

### Variants
- Short (toast/snackbar): "brief version"
- Full (inline/page): "detailed version with guidance"
- Accessible (screen reader): "version optimized for screen readers"

### Implementation Notes
- When to show this error
- Logging level for this error
- Whether to include retry action
```
