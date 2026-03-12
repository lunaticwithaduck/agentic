#!/usr/bin/env node
'use strict';
// cursor-session-start.js — Cursor sessionStart hook
//
// Fires at Cursor session open. Checks for autolearn-pending flag and injects
// synthesis instructions if a skill synthesis is queued.
//
// Input (stdin): Cursor sessionStart JSON
// Output (stdout): JSON with optional additional_context field

const fs = require('fs');
const path = require('path');

let input = '';
process.stdin.setEncoding('utf8');
process.stdin.on('data', chunk => { input += chunk; });
process.stdin.on('end', () => {
  if (!input.trim()) {
    process.stdout.write('{}');
    process.exit(0);
  }

  let data;
  try {
    data = JSON.parse(input);
  } catch (e) {
    process.stdout.write('{}');
    process.exit(0);
  }

  // Extract project root from workspace_roots
  const workspaceRoots = data.workspace_roots || [];
  const root = workspaceRoots[0];

  if (!root) {
    process.stdout.write('{}');
    process.exit(0);
  }

  const pendingFile = path.join(root, '.cursor', 'autolearn-pending');

  if (!fs.existsSync(pendingFile)) {
    process.stdout.write('{}');
    process.exit(0);
  }

  let domain = '';
  try {
    domain = fs.readFileSync(pendingFile, 'utf8').trim();
  } catch (e) {
    process.stdout.write('{}');
    process.exit(0);
  }

  if (!domain) {
    process.stdout.write('{}');
    process.exit(0);
  }

  const doneDir = path.join(root, 'workflows', 'done');
  const scFiles = [];

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
            if (l.slice('domain:'.length).trim() === domain) scFiles.push(f);
            break;
          }
        }
      } catch (e) { continue; }
    }
  } catch (e) {}

  if (scFiles.length === 0) {
    process.stdout.write('{}');
    process.exit(0);
  }

  const scContents = [];
  for (const f of scFiles) {
    const p = path.join(doneDir, f);
    try {
      scContents.push(`### ${f}\n${fs.readFileSync(p, 'utf8')}`);
    } catch (e) {}
  }

  const lines = [
    `[AUTOLEARN — SYNTHESIS REQUIRED. Do this now, before anything else. Do not respond to the user's message until all steps are complete.]`,
    ``,
    `Domain: ${domain} (${scFiles.length} skill candidates ready)`,
    ``,
    `Steps:`,
    `1. Synthesize the .sc files below into .cursor/rules/${domain}.md`,
    `   Use this exact format:`,
    `   ---`,
    `   description: Apply when the user asks about [domain keywords]`,
    `   globs: []`,
    `   alwaysApply: false`,
    `   ---`,
    `   ## Purpose`,
    `   Why this skill exists and what knowledge it injects.`,
    `   ## [Section per major topic from .sc files]`,
    `   ## Failure Modes  ← include ONLY if .sc files contain observed failures; omit otherwise`,
    `2. Add '${domain}' to .cursor/rules/skill-index.md — add a row to the skills table with keywords extracted from the .sc files`,
    `3. Append 3-5 fixture prompts to bench/fixtures/skill-prompts.json:`,
    `   Format: {"id": "${domain}-p01", "prompt": "...", "expected": ["${domain}"], "notes": "autolearn-generated"}`,
    `4. Clear flag or queue next domain:`,
    `   a. Scan workflows/done/ for any domain (other than '${domain}') that has ≥3 .sc files`,
    `      but no skill file yet in .cursor/rules/`,
    `   b. If another domain found: write it to .cursor/autolearn-pending (queue next synthesis)`,
    `   c. If none: delete .cursor/autolearn-pending`,
    `5. Run: bash bench/run.sh --suite=02 — warn if precision drops >5pp vs previous run`,
    `6. Tell the user: 'Auto-generated skill: ${domain}' and confirm the regression result`,
    ``,
    `--- .sc file contents ---`,
    scContents.join('\n\n'),
    ``,
    `--- End Skill Candidates ---`,
  ];

  const additionalContext = lines.join('\n');
  process.stdout.write(JSON.stringify({ additional_context: additionalContext }));
  process.exit(0);
});
