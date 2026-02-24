---
name: meeting-notes
description: Structure meeting notes with action items, decisions, and owners
activation:
  keywords: ["meeting notes", "meeting summary", "action items", "meeting minutes", "standup notes"]
  file_patterns: []
---

# Meeting Notes Structurer

## Purpose
Take raw meeting notes, transcripts, or recordings and organize them into a structured format with clear action items, decisions, and owners.

## Instructions

1. **Parse the raw input**:
   - Accept unstructured notes, bullet points, or transcript text
   - Identify speakers/participants from context clues
   - Detect timestamps if present
   - Note the meeting type (standup, planning, retro, 1:1, decision meeting)

2. **Extract and organize into sections**:

   **Meeting metadata**:
   - Date and time
   - Attendees (extract from mentions in notes)
   - Meeting type or title

   **Agenda / Topics discussed**:
   - List each topic covered
   - Keep to factual summary of what was discussed

   **Decisions made**:
   - State each decision clearly and unambiguously
   - Note the rationale or context behind the decision
   - Note who made or approved the decision
   - Flag any decisions that were tentative or conditional

   **Action items**:
   - One line per action item
   - Include: what, who (owner), when (deadline)
   - Mark priority if mentioned (high/medium/low)
   - Format as checkboxes for easy tracking

   **Open questions / Parking lot**:
   - Items raised but not resolved
   - Questions needing follow-up
   - Topics deferred to future meetings

3. **Clean up**:
   - Remove filler and off-topic conversation
   - Consolidate duplicate points
   - Clarify ambiguous references
   - Use consistent terminology throughout

## Output Format

```markdown
# Meeting: [Title/Type]
**Date**: YYYY-MM-DD | **Attendees**: Name1, Name2, Name3

## Agenda
1. Topic A
2. Topic B

## Discussion
### Topic A
Summary of discussion points.

### Topic B
Summary of discussion points.

## Decisions
- **[Decision]**: Rationale. (Decided by: Name)

## Action Items
- [ ] [Task description] -- **Owner**: Name, **Due**: Date
- [ ] [Task description] -- **Owner**: Name, **Due**: Date

## Open Questions
- Question that needs follow-up?
```
