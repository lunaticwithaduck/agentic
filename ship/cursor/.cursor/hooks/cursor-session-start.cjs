#!/usr/bin/env node
'use strict';
// cursor-session-start.js — Cursor sessionStart hook
//
// Fires at Cursor session open. Does two things:
//   1. Workflow state check: injects creation requirement (tasks/ empty) or
//      open-task nudge (tasks/ non-empty) to enforce the pipeline.
//   2. Autolearn synthesis: if autolearn-pending exists, injects synthesis
//      instructions so the model synthesises the queued skill.
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

  const parts = [];

  // --- 2. Autolearn synthesis check (must come first to guard workflow block) ---
  const pendingFile = path.join(root, '.cursor', 'autolearn-pending');
  const synthesisPending = fs.existsSync(pendingFile);

  // --- 1. Workflow state check (skip when synthesis is pending to avoid conflicting directives) ---
  if (!synthesisPending) {
    const tasksDir = path.join(root, 'workflows', 'tasks');
    try {
      if (fs.existsSync(tasksDir)) {
        const taskFiles = fs.readdirSync(tasksDir).filter(f => !f.startsWith('.'));
        if (taskFiles.length === 0) {
          parts.push([
            '[REQUIRED — Before writing any code or files]',
            'workflows/tasks/ is empty. You MUST do this first:',
            '1. Break the work into logical units and create one task .md file per unit in workflows/tasks/',
            '2. Implement one task at a time',
            '3. Run /complete after each task before starting the next',
            'Do not write any implementation files until at least one task file exists in workflows/tasks/',
          ].join('\n'));
        } else {
          const names = taskFiles.join(', ');
          parts.push(`Open task(s) in workflows/tasks/: ${names}\nRun /complete on any finished tasks before starting new work.`);
        }
      }
    } catch (e) {}
  }

  if (synthesisPending) {
    let domain = '';
    try {
      domain = fs.readFileSync(pendingFile, 'utf8').trim();
    } catch (e) {}

    if (domain) {
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

      if (scFiles.length > 0) {
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
          `1. Synthesize the .sc files below into .cursor/rules/${domain}.mdc`,
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
          `2. Add '${domain}' to .cursor/rules/skill-index.mdc — add a row to the skills table with keywords extracted from the .sc files`,
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
        parts.push(lines.join('\n'));
      }
    }
  }

  if (parts.length === 0) {
    process.stdout.write('{}');
    process.exit(0);
  }

  process.stdout.write(JSON.stringify({ additional_context: parts.join('\n\n') }));
  process.exit(0);
});
