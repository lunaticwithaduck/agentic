#!/usr/bin/env node
'use strict';
// convert-to-agent.js — converts a .claude/agents/*.md file to a Copilot .agent.md file
//
// Usage: node convert-to-agent.js <input-path> <output-path>
//
// Input: .claude/agents/foo.md
//   # Title
//   ## Role
//   [First paragraph — the description]
//   ...full file content...
//
// Output: .github/agents/foo.agent.md
//   ---
//   description: [first paragraph after ## Role heading, condensed to one sentence]
//   tools: []
//   ---
//
//   [full file content, unchanged]

const fs = require('fs');
const path = require('path');

const [,, inputPath, outputPath] = process.argv;

if (!inputPath || !outputPath) {
  console.error('Usage: node convert-to-agent.js <input-path> <output-path>');
  process.exit(1);
}

const content = fs.readFileSync(inputPath, 'utf8');
const lines = content.split('\n');

// Extract description from the first paragraph after ## Role heading
let description = '';
let inRole = false;
let paragraphLines = [];
let foundParagraph = false;

for (const line of lines) {
  if (/^## Role\s*$/.test(line)) {
    inRole = true;
    continue;
  }

  if (inRole) {
    // Stop if we hit another heading
    if (/^##/.test(line) && paragraphLines.length > 0) {
      break;
    }
    if (/^##/.test(line)) {
      // Another heading before finding content — stop
      break;
    }

    // Skip blank lines before the paragraph starts
    if (!foundParagraph && line.trim() === '') {
      continue;
    }

    // Once we start a paragraph, collect until blank line or next heading
    if (line.trim() !== '') {
      foundParagraph = true;
      paragraphLines.push(line.trim());
    } else if (foundParagraph) {
      // End of first paragraph
      break;
    }
  }
}

if (paragraphLines.length > 0) {
  // Join paragraph lines into a single sentence, normalizing whitespace
  description = paragraphLines.join(' ').replace(/\s+/g, ' ').trim();
  // Ensure it ends with a period
  if (!description.endsWith('.')) {
    description += '.';
  }
} else {
  // Fallback: use the filename
  description = `${path.basename(inputPath, '.md')} agent.`;
}

// Build output with frontmatter
const frontmatter = [
  '---',
  `description: ${description}`,
  'tools: []',
  '---',
].join('\n');

const output = frontmatter + '\n\n' + content;

// Ensure output directory exists
const outDir = path.dirname(outputPath);
fs.mkdirSync(outDir, { recursive: true });

fs.writeFileSync(outputPath, output);
console.log(`Converted: ${path.basename(inputPath)} -> ${outputPath}`);
