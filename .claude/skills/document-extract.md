---
name: document-extract
description: Extract and structure content from any document format
activation:
  keywords: ["extract document", "parse document", "convert document", "document to markdown", "extract content"]
  file_patterns: ["**/*.pdf", "**/*.docx", "**/*.html", "**/*.csv", "**/*.json", "**/*.xml"]
---

# Document Content Extractor

## Purpose
Auto-detect document format and extract structured content into clean markdown, handling PDFs, HTML, CSV, JSON, XML, and other common formats.

## Instructions

1. **Detect the format**:
   - Check file extension and content structure
   - Supported formats: PDF, DOCX, HTML, CSV, JSON, XML, YAML, plain text
   - For ambiguous files, inspect the first few lines for format markers

2. **Format-specific extraction**:
   - **PDF**: Use the Read tool with page ranges for large files. Extract text preserving structure.
   - **HTML**: Strip tags, preserve semantic structure (headings, lists, tables, links). Remove scripts, styles, and nav elements.
   - **CSV**: Convert to markdown table. Detect delimiter (comma, tab, semicolon). Handle quoted fields.
   - **JSON**: Pretty-print and convert to readable format. For arrays of objects, consider table format. For nested structures, use indented sections.
   - **XML**: Extract meaningful content, ignore schema declarations. Convert to readable structure.
   - **YAML**: Preserve hierarchy, convert to documented sections if complex.

3. **Handle edge cases**:
   - Large files: Process in chunks, summarize if over 1000 lines
   - Encoding issues: Detect and handle UTF-8, Latin-1, etc.
   - Nested structures: Flatten with clear hierarchy indicators
   - Binary content: Note as `[Binary content - not extractable]`
   - Empty or corrupt files: Report clearly

4. **Structure the output**:
   - Add a metadata header (source file, format, size, date if available)
   - Convert to logical sections with markdown headings
   - Preserve data relationships and hierarchy
   - Add a table of contents for long documents

## Output Format

Clean markdown with:
- Metadata block at the top (source, format, extraction date)
- Logical heading structure reflecting document organization
- Tables for tabular data
- Code blocks for structured data (JSON, XML snippets)
- Notes on any content that could not be extracted
