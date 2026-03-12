#!/usr/bin/env node
'use strict';
// convert-skill.js — converts a single agentic skill .md to Cursor rule format
//
// Usage: node convert-skill.js <input-path> <output-path>
//
// Input (agentic format):
//   ---
//   name: skill-creator
//   description: One-line description
//   activation:
//     keywords: ["create a skill", "make a new skill"]
//     file_patterns: ["**/*.skill.md"]
//   ---
//   # Content...
//
// Output (Cursor rule format):
//   ---
//   description: Apply when the user asks about create a skill, make a new skill. Also applies when working with skill definition files.
//   globs: ["**/*.skill.md"]
//   alwaysApply: false
//   ---
//   # Content...

const fs = require('fs');
const path = require('path');

const [,, inputPath, outputPath] = process.argv;

if (!inputPath || !outputPath) {
  console.error('Usage: node convert-skill.js <input-path> <output-path>');
  process.exit(1);
}

const content = fs.readFileSync(inputPath, 'utf8');

// Parse frontmatter
const fmMatch = content.match(/^---\n([\s\S]*?)\n---\n([\s\S]*)$/);
if (!fmMatch) {
  console.error(`No frontmatter found in ${inputPath}`);
  process.exit(1);
}

const fmRaw = fmMatch[1];
const body = fmMatch[2];

// Parse key fields from frontmatter
function parseYamlField(fm, key) {
  const match = fm.match(new RegExp(`^${key}:\\s*(.+)$`, 'm'));
  return match ? match[1].trim() : null;
}

function parseYamlArray(fm, key) {
  // Match inline array: keywords: ["a", "b", "c"]
  const inlineMatch = fm.match(new RegExp(`^  ${key}:\\s*(\\[.*?\\])`, 'm'));
  if (inlineMatch) {
    try {
      return JSON.parse(inlineMatch[1]);
    } catch (e) {}
  }
  // Match multi-line array
  const multiMatch = fm.match(new RegExp(`^  ${key}:\\s*\\n((?:    - .+\\n?)*)`, 'm'));
  if (multiMatch) {
    return multiMatch[1]
      .split('\n')
      .filter(l => l.trim().startsWith('- '))
      .map(l => l.trim().slice(2).trim());
  }
  return [];
}

const keywords = parseYamlArray(fmRaw, 'keywords');
// Translate Claude Code paths in filePatterns to Cursor equivalents
const filePatterns = parseYamlArray(fmRaw, 'file_patterns')
  .map(p => p.replace(/^\.claude\/skills\//, '.cursor/rules/'));

// Build Cursor description from keywords
let description = '';
if (keywords.length > 0) {
  description = `Apply when the user asks about ${keywords.join(', ')}.`;
} else {
  const skillName = parseYamlField(fmRaw, 'name') || path.basename(inputPath, '.md');
  description = `Apply when working with ${skillName}.`;
}

if (filePatterns.length > 0) {
  // Humanize the file pattern description
  const patternDesc = filePatterns
    .map(p => {
      if (p.includes('skills')) return 'skill definition';
      if (p.includes('migrations') || p.includes('migrate')) return 'migration';
      if (p.includes('prisma')) return 'Prisma schema';
      if (p.includes('.sql')) return 'SQL';
      if (p.includes('.ts')) return 'TypeScript';
      if (p.includes('.py')) return 'Python';
      if (p.includes('.go')) return 'Go';
      if (p.includes('Dockerfile') || p.includes('docker')) return 'Docker';
      if (p.includes('.css') || p.includes('.scss') || p.includes('.less')) return 'CSS';
      if (p.includes('.tsx') || p.includes('.jsx')) return 'React component';
      if (p.includes('.stories')) return 'Storybook story';
      if (p.includes('.tf') || p.includes('.hcl') || p.includes('terraform')) return 'Terraform';
      if (p.includes('.github/workflows') || p.includes('ci')) return 'CI/CD workflow';
      if (p.includes('package.json') || p.includes('requirements.txt')) return 'dependency manifest';
      if (p.includes('.env')) return 'environment/secrets';
      if (p.includes('.pdf')) return 'PDF';
      if (p.includes('.md')) return 'Markdown';
      // Generic fallback
      const ext = p.replace(/\*\*\/\*/, '').replace(/\*/, '');
      return ext ? ext : p;
    })
    .filter((v, i, a) => a.indexOf(v) === i) // deduplicate
    .join(', ');
  description += ` Also applies when working with ${patternDesc} files.`;
}

// Build Cursor frontmatter
const globsStr = filePatterns.length > 0
  ? JSON.stringify(filePatterns)
  : '[]';

const cursorFm = [
  '---',
  `description: ${description}`,
  `globs: ${globsStr}`,
  `alwaysApply: false`,
  '---',
].join('\n');

// Rewrite body to use Cursor paths and formats
let convertedBody = body
  // Paths: .claude/skills/ → .cursor/rules/
  .replace(/\.claude\/skills\//g, '.cursor/rules/')
  // skill-rules.json reference → skill-index.md
  .replace(/skill-rules\.json/g, 'skill-index.md')
  // CLAUDE.md → agent-instructions.md (headings, backtick refs, inline refs)
  .replace(/`CLAUDE\.md`/g, '`agent-instructions.md`')
  .replace(/## Also Update CLAUDE\.md/g, '## Also Update agent-instructions.md')
  .replace(/in `CLAUDE\.md`/g, 'in `agent-instructions.md`')
  // Replace the agentic frontmatter template block with Cursor format
  .replace(
    /^(```markdown\n)---\nname: <skill-name>\ndescription: <one-line description of what domain knowledge this injects>\nactivation:\n  keywords: \["keyword1", "keyword2"\]\n  file_patterns: \["\*\*\/\*\.ext", "specific-file"\]\n---/m,
    '$1---\ndescription: Apply when the user asks about keyword1, keyword2. Also applies when working with .ext files.\nglobs: ["**/*.ext", "specific-file"]\nalwaysApply: false\n---'
  );

const output = cursorFm + '\n' + convertedBody;

// Ensure output directory exists
const outDir = path.dirname(outputPath);
fs.mkdirSync(outDir, { recursive: true });

fs.writeFileSync(outputPath, output);
console.log(`Converted: ${path.basename(inputPath)} -> ${outputPath}`);
