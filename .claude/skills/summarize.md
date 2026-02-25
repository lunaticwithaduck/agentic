---
name: summarize
description: Create summaries at configurable depth (brief, standard, detailed)
activation:
  keywords: ["summarize", "summary", "tldr", "key points", "brief summary", "recap"]
  file_patterns: []
---

# Content Summarizer

## Purpose
Read content (documents, code, conversations, articles) and produce summaries at a requested depth level, preserving key facts and actionable information.

## Instructions

1. **Read and understand the content**:
   - Read the full source material
   - Identify the content type (technical doc, article, conversation, code, report)
   - Note the main topic, key arguments, and supporting evidence
   - Identify key facts, figures, dates, and names

2. **Determine summary depth** (ask if not specified):
   - **Brief**: 1-3 sentences capturing the single most important point
   - **Standard**: 1-2 paragraphs covering main points and key supporting details
   - **Detailed**: Section-by-section breakdown preserving structure and nuance

3. **Create the summary**:
   - Lead with the most important information (inverted pyramid)
   - Use concrete language; preserve specific numbers and facts
   - Attribute claims and decisions to their sources
   - Distinguish facts from opinions or recommendations
   - Preserve technical accuracy; do not oversimplify critical details
   - Omit filler, repetition, and tangential content

4. **Add key takeaways**:
   - List 3-5 bullet points of the most actionable or important items
   - For decision documents: highlight the decision and rationale
   - For technical content: highlight breaking changes, requirements, or deadlines
   - For discussions: highlight agreements, disagreements, and open questions

5. **Quality check**:
   - Verify the summary could stand alone without the source
   - Ensure no key information is lost at the chosen depth
   - Check that the tone matches the source material

## Output Format

```markdown
## Summary
[Summary text at the requested depth]

## Key Takeaways
- Takeaway 1
- Takeaway 2
- Takeaway 3
```

For detailed summaries, use section headings matching the source structure.

---

## Meeting Notes

When the input is meeting notes, a transcript, or a recording summary, use this structured output instead:

```markdown
# Meeting: [Title or Type — standup / planning / retro / decision / 1:1]
**Date**: YYYY-MM-DD | **Attendees**: Name1, Name2, Name3

## Discussion
### [Topic A]
Factual summary of what was discussed.

## Decisions
- **[Decision]**: Rationale. *(Decided by: Name)*

## Action Items
- [ ] [Task] — **Owner**: Name, **Due**: Date
- [ ] [Task] — **Owner**: Name, **Due**: Date

## Open Questions
- Question that needs follow-up (owner if known)
```

Extraction rules:
- Pull action items even if phrased informally ("John will handle X by Friday")
- Flag tentative decisions: "Tentative: [decision] — pending [condition]"
- Move off-topic conversation to Open Questions or omit entirely
- If no deadline was stated for an action item, write "Due: TBD"

---

## Weekly Activity Summary

When asked for a weekly summary of project activity, run these first:

```bash
# Commit overview
git log --since="1 week ago" --oneline --all

# Per-contributor breakdown
git log --since="1 week ago" --format="%an" | sort | uniq -c | sort -rn

# File change stats
git log --since="1 week ago" --stat | tail -3
```

Then output:

```markdown
# Weekly Summary: [Mon DD] – [Sun DD]

## Highlights
- [Most impactful change or milestone]

## Changes by Category
**Features**: [feat commits]
**Fixes**: [fix commits]
**Refactoring / Chores**: [other commits]

## Metrics
| Commits | Files changed | Lines +/- | Contributors |
|---------|--------------|-----------|--------------|
| N | N | +N / -N | N |

## Contributors
- Name: N commits
```
