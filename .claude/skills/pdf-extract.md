---
name: pdf-extract
description: Extract and structure content from PDF files into clean markdown
activation:
  keywords: ["pdf", "extract pdf", "read pdf", "parse pdf", "pdf to markdown"]
  file_patterns: ["**/*.pdf"]
---

# PDF Content Extractor

## Purpose
Read PDF files and extract their content into well-structured markdown, preserving headings, tables, lists, and document hierarchy.

## Instructions

1. **Read the PDF**:
   - Use the Read tool to open the PDF file
   - For large PDFs (10+ pages), process in chunks using the `pages` parameter
   - Note the total page count and structure

2. **Identify document structure**:
   - Detect headings by font size, weight, or formatting cues
   - Identify body text, captions, footnotes
   - Locate tables and their boundaries
   - Find bulleted and numbered lists
   - Note any multi-column layouts

3. **Extract and convert**:
   - Convert headings to appropriate markdown heading levels (H1, H2, H3)
   - Preserve paragraph breaks
   - Convert tables to markdown table syntax, aligning columns
   - Convert lists to markdown bullet or numbered lists
   - Handle multi-column text by reading left-to-right, top-to-bottom
   - Preserve emphasis (bold, italic) where detectable

4. **Handle special content**:
   - Note images with `[Image: description if available]` placeholders
   - Preserve hyperlinks where detected
   - Convert footnotes to inline references or a footnotes section
   - Handle headers and footers (typically exclude page numbers)

5. **Clean up**:
   - Remove artifacts from PDF extraction (stray characters, broken words)
   - Fix hyphenation at line breaks
   - Normalize whitespace
   - Verify table alignment

## Output Format

Clean markdown with:
- Document title as H1
- Logical heading hierarchy
- Properly formatted tables
- Preserved lists and emphasis
- Image placeholders where applicable
- A note at the top indicating source file and page count
