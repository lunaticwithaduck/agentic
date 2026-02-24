---
name: technical-writing
description: Apply technical documentation standards and best practices
activation:
  keywords: ["technical writing", "tech docs", "documentation standards", "write docs", "doc style"]
  file_patterns: ["**/docs/**", "**/*.md"]
---

# Technical Writing Standards

## Purpose
Guide the creation of clear, consistent technical documentation for any audience: developers, end users, or operations teams.

## Instructions

1. **Identify the audience**:
   - **Developers**: Include code examples, architecture details, API references. Assume technical vocabulary.
   - **End users**: Focus on tasks and outcomes. Avoid jargon. Include screenshots or UI references.
   - **Operations**: Cover deployment, monitoring, troubleshooting, runbooks. Include commands and expected outputs.

2. **Writing principles**:
   - Use active voice ("The server returns a 200 response" not "A 200 response is returned")
   - Lead with the most important information
   - One idea per paragraph, keep paragraphs to 3-5 sentences
   - Use second person ("you") for instructions
   - Define acronyms on first use: "Content Delivery Network (CDN)"
   - Be specific: "takes ~200ms" not "is fast"

3. **Structure**:
   - Start with a one-line summary of what the document covers
   - Use hierarchical headings (H2 for sections, H3 for subsections)
   - Use numbered lists for sequential steps
   - Use bullet lists for non-ordered items
   - Add a "Prerequisites" section before any tutorial
   - End with "Next steps" or "Related resources" where appropriate

4. **Code examples**:
   - Every concept should have a code example where applicable
   - Show both the command and its expected output
   - Use realistic values, not "foo" and "bar"
   - Mark placeholder values clearly: `<your-api-key>`
   - Include error cases and how to handle them

5. **Visual aids**:
   - Use Mermaid diagrams for architecture and flows
   - Use tables for comparisons and reference data
   - Use admonitions (Note, Warning, Tip) for callouts
   - Keep diagrams simple; split complex ones into multiple

## Output Format

GitHub-flavored markdown with clear heading hierarchy, code blocks with language tags, and tables where data is comparative or reference-oriented.
