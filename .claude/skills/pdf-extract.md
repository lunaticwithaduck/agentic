---
name: pdf-extract
description: Extract and structure content from PDF files, handling scanned docs, multi-column layouts, and embedded assets
activation:
  keywords: ["pdf", "extract pdf", "read pdf", "parse pdf", "pdf content", "pdf to markdown", "pdf to text"]
  file_patterns: ["**/*.pdf"]
---

# PDF Content Extractor

## Purpose
Extract structured content from PDF files, handling the full range of PDF types: text-based, scanned/image-only, multi-column, and password-protected.

## Step 1: Classify the PDF

Before extracting, determine the PDF type:

| Type | Detection | Approach |
|------|-----------|----------|
| **Text-based** | Read tool returns readable text | Direct extraction with structure preservation |
| **Scanned / image-only** | Read tool returns blank or garbled output | Note limitation; cannot extract without OCR |
| **Mixed** | Some pages have text, some don't | Extract text pages; flag image-only pages |
| **Password-protected** | Read tool returns access error | Report; ask user for decrypted copy |
| **Form PDF** | Contains fillable form fields | Extract field labels and values separately |

## Step 2: Extract with Structure

Use the `Read` tool with `pages` parameter for large files (never attempt >20 pages at once):

```
Read file.pdf pages="1-10"
Read file.pdf pages="11-20"
```

### Preserve Semantic Structure

- **Headings**: Infer from font size cues (ALL CAPS, numbered sections like `1.`, `1.1`, `Chapter X`)
- **Body text**: Flow paragraphs, joining hyphenated line-breaks (`end-` + newline + `ing` → `ending`)
- **Lists**: Preserve bullet and numbered list hierarchy
- **Tables**: Reconstruct as markdown tables; flag if columns are misaligned
- **Footnotes**: Collect at end of each page under `---`; do not inline mid-paragraph
- **Page numbers**: Strip `\d+` appearing alone on a line at top/bottom of pages
- **Headers/footers**: Remove repeated document title, company name, "CONFIDENTIAL" stamps

### Multi-Column Layout Detection

PDFs with two or more columns often extract as interleaved text. Signs:
- Sentences ending mid-phrase, then unrelated text, then the sentence resumes
- Short lines alternating between two topics

When detected: re-read in column order (left column fully, then right column). If the Read tool output is inextricably interleaved, flag the affected pages and provide raw text with a note.

## Step 3: Handle Special Content

### Tables
PDFs rarely export tables cleanly. Reconstruct by:
1. Identifying header row from ALL CAPS or bold indicators
2. Aligning columns by consistent whitespace gaps
3. Outputting as markdown table
4. If alignment is ambiguous, use a code block with fixed-width formatting

```markdown
| Column A | Column B | Column C |
|----------|----------|----------|
| value    | value    | value    |
```

### Embedded Images and Figures
- Note as: `[Figure X: description inferred from surrounding caption]`
- Do not attempt to describe image content unless it is a diagram with labeled elements

### Mathematical Formulas
- Preserve as plain text approximation: `E = mc²`, `∑(x_i) / n`
- Flag complex formulas: `[Formula — LaTeX rendering recommended]`

### Code Blocks
- Wrap in fenced code blocks with detected language
- Fix common PDF extraction artifacts: ligatures (`ﬁ` → `fi`), smart quotes (`"` → `"`)

## Step 4: Output Structure

```markdown
---
Source: filename.pdf
Pages: 24
Type: text-based | scanned | mixed
Extracted: [today's date]
Warnings: [list any extraction issues]
---

# [Document Title]

## [Section 1 heading]

[body text]

## [Section 2 heading]
...
```

## Common Extraction Artifacts to Fix

| Artifact | Example | Fix |
|----------|---------|-----|
| Hyphenated line break | `impor-\ntant` | `important` |
| Ligatures | `ﬁle`, `ﬀ` | `file`, `ff` |
| Curly quotes | `"text"` | `"text"` |
| Orphaned page numbers | `\n42\n` at page boundary | Remove |
| Running header bleed | `ACME CORP 2024 Q3 REPORT` mid-paragraph | Remove |
| Column interleave | Two topics alternating per line | Note and reorder if possible |

## Scanned PDF Limitation

If the PDF is image-only (scanned document):

> **Cannot extract**: This PDF contains scanned images with no embedded text layer. To extract content, run it through an OCR tool first:
> - Adobe Acrobat: Edit → Make PDF Searchable
> - CLI: `ocrmypdf input.pdf output.pdf` (installs a text layer)
> - Online: Adobe online tools, Google Drive (open PDF → auto-OCR)
>
> Provide the OCR'd PDF and re-run extraction.
