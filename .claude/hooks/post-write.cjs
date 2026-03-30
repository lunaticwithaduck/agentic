#!/usr/bin/env node
'use strict';
// post-write.js - PostToolUse hook for Write and Edit operations
// Runs after files are written or edited.
// - Validates JSON files automatically
// - Detects .sc (skill candidate) files and flags domains for synthesis at N=3

const fs = require('fs');
const path = require('path');

const ROOT_DIR = path.resolve(__dirname, '../..');

let input = '';
process.stdin.setEncoding('utf8');
process.stdin.on('data', chunk => { input += chunk; });
process.stdin.on('end', () => {
  let filePath = '';
  try {
    const d = JSON.parse(input);
    const payload = d.tool_input || d;
    filePath = payload.file_path || payload.filePath || payload.path || '';
  } catch (e) {
    process.exit(0);
  }

  if (!filePath || !fs.existsSync(filePath)) process.exit(0);

  // Validate JSON files after write
  if (filePath.endsWith('.json')) {
    try {
      JSON.parse(fs.readFileSync(filePath, 'utf8'));
    } catch (e) {
      process.stderr.write(`WARNING: ${filePath} is not valid JSON\n`);
    }
  }

  // Skill candidating: detect .sc file writes in workflows/done/
  const normalizedPath = filePath.replace(/\\/g, '/');
  if (!normalizedPath.match(/workflows\/done\/[^/]+\.sc$/)) process.exit(0);

  const doneDir = path.join(ROOT_DIR, 'workflows', 'done');
  // Detect platform: running from .cursor/hooks/, .github/hooks/, or .claude/hooks/
  const _hooksParent = path.basename(path.dirname(__dirname));
  const isCursor = _hooksParent === '.cursor';
  const isCopilot = _hooksParent === '.github';
  const platformDir = isCursor ? '.cursor' : isCopilot ? '.github' : '.claude';
  const pendingFile = path.join(ROOT_DIR, platformDir, 'autolearn-pending');
  const THRESHOLD = 3;

  let scPath = filePath;
  if (!path.isAbsolute(scPath)) scPath = path.join(ROOT_DIR, scPath);

  // Extract domain from the new .sc file
  let domain = null;
  try {
    const lines = fs.readFileSync(scPath, 'utf8').split('\n');
    let inFrontmatter = false;
    for (const line of lines) {
      const l = line.trim();
      if (l === '---') {
        if (!inFrontmatter) { inFrontmatter = true; continue; }
        else { break; }
      }
      if (inFrontmatter && l.startsWith('domain:')) {
        domain = l.slice('domain:'.length).trim();
        break;
      }
    }
  } catch (e) {
    process.exit(0);
  }

  if (!domain) process.exit(0);

  // Count .sc files with this same domain
  let count = 0;
  try {
    for (const f of fs.readdirSync(doneDir)) {
      if (!f.endsWith('.sc')) continue;
      const p = path.join(doneDir, f);
      try {
        const lines = fs.readFileSync(p, 'utf8').split('\n');
        let inFm = false;
        for (const line of lines) {
          const l = line.trim();
          if (l === '---') {
            if (!inFm) { inFm = true; continue; } else { break; }
          }
          if (inFm && l.startsWith('domain:')) {
            if (l.slice('domain:'.length).trim() === domain) count++;
            break;
          }
        }
      } catch (e) { continue; }
    }
  } catch (e) {}

  if (count >= THRESHOLD) {
    fs.writeFileSync(pendingFile, domain + '\n');
  }

  process.exit(0);
});
