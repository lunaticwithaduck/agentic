#!/usr/bin/env node
'use strict';
// cursor-skill-injector.js — Cursor afterFileEdit hook
//
// Fires on every file edit. Does two things:
//   1. File-pattern skill injection: scans .cursor/rules/*.mdc for rules with
//      matching globs, injects relevant rule content as additional_context.
//   2. Synthesis re-injection: if autolearn-pending exists, injects synthesis
//      instructions so they persist throughout the session.
//
// Input (stdin): Cursor afterFileEdit JSON
// Output (stdout): JSON with optional additional_context field

const fs = require('fs');
const path = require('path');

// Simple glob matcher — no external dependencies
function matchesGlob(filePath, pattern) {
  const normalized = filePath.replace(/\\/g, '/');
  const pat = pattern.replace(/\\/g, '/');

  if (pat.startsWith('**/')) {
    const suffix = pat.slice(3);
    if (suffix.startsWith('*.')) {
      // **/*.ext — match any file with that extension
      const ext = suffix.slice(1); // e.g. .py
      return normalized.endsWith(ext);
    }
    if (suffix.includes('/')) {
      // **/dir/file — match any path ending with dir/file
      return normalized.endsWith('/' + suffix) || normalized === suffix;
    }
    // **/filename — match any file with that exact basename
    const base = path.basename(normalized);
    return base === suffix || normalized.endsWith('/' + suffix);
  }

  if (pat.includes('/**')) {
    // dir/** — match anything inside dir/
    const prefix = pat.slice(0, pat.indexOf('/**'));
    return normalized.includes('/' + prefix + '/') || normalized.startsWith(prefix + '/');
  }

  if (pat.includes('*')) {
    // e.g. Dockerfile.* or *.config.js
    const escapedPat = pat
      .replace(/[.+^${}()|[\]\\]/g, '\\$&') // escape regex special chars except *
      .replace(/\\\*/g, '.*');               // unescape * back to .*
    const re = new RegExp('(^|/)' + escapedPat + '$');
    return re.test(normalized);
  }

  // Exact or suffix match (e.g. "package.json", "Dockerfile")
  return normalized.endsWith('/' + pat) || normalized === pat;
}

// Parse globs array from Cursor rule frontmatter
function parseGlobs(content) {
  const fmMatch = content.match(/^---\n([\s\S]*?)\n---/);
  if (!fmMatch) return [];
  const fm = fmMatch[1];
  const globsMatch = fm.match(/^globs:\s*(\[.*?\])/m);
  if (!globsMatch) return [];
  try {
    return JSON.parse(globsMatch[1]);
  } catch (e) {
    return [];
  }
}

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

  const filePath = data.file_path || '';
  const workspaceRoots = data.workspace_roots || [];
  const root = workspaceRoots[0];

  if (!root) {
    process.stdout.write('{}');
    process.exit(0);
  }

  const rulesDir = path.join(root, '.cursor', 'rules');
  const pendingFile = path.join(root, '.cursor', 'autolearn-pending');

  // Workflow state check — skip if the edit itself is creating a task file
  let workflowContext = '';
  const isTaskFile = filePath && filePath.replace(/\\/g, '/').includes('workflows/tasks/');
  if (!isTaskFile) {
    try {
      const tasksDir = path.join(root, 'workflows', 'tasks');
      if (fs.existsSync(tasksDir)) {
        const taskFiles = fs.readdirSync(tasksDir).filter(f => !f.startsWith('.'));
        if (taskFiles.length === 0) {
          workflowContext = [
            '[STOP — No task file exists]',
            'You edited a file without first creating a task.',
            'Create workflows/tasks/YYYY-MM-DD-descriptive-name.md now, then continue.',
            'Do not make further changes until a task file exists in workflows/tasks/.',
          ].join('\n');
        } else {
          const names = taskFiles.join(', ');
          workflowContext = `Open task(s): ${names}. Run /complete when done before starting new work.`;
        }
      }
    } catch (e) {}
  }

  // Scan .cursor/rules/*.mdc for rules with matching globs
  const skillParts = [];
  if (filePath) {
    let ruleFiles = [];
    try {
      ruleFiles = fs.readdirSync(rulesDir).filter(f => f.endsWith('.mdc'));
    } catch (e) {}

    for (const ruleFile of ruleFiles) {
      const rulePath = path.join(rulesDir, ruleFile);
      let content = '';
      try { content = fs.readFileSync(rulePath, 'utf8'); } catch (e) { continue; }

      const globs = parseGlobs(content);
      if (globs.length === 0) continue;

      if (globs.some(pat => matchesGlob(filePath, pat))) {
        const skillName = ruleFile.replace(/\.mdc$/, '');
        skillParts.push(`[AUTO-ACTIVATED SKILL: ${skillName}]\n${content}\n[END SKILL: ${skillName}]`);
      }
    }
  }

  // Check for synthesis pending
  const synthesisPending = fs.existsSync(pendingFile);
  let synthesisContext = '';

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
        synthesisContext = lines.join('\n');
      }
    }
  }

  // Build output
  const parts = [];

  if (workflowContext) {
    parts.push(workflowContext);
  }

  if (skillParts.length > 0) {
    const label = skillParts.length === 1 ? 'skill was' : 'skills were';
    parts.push(`The following ${label} automatically activated based on the file you are editing. Apply the domain knowledge in your response:\n\n${skillParts.join('\n\n')}`);
  }

  if (synthesisContext) {
    parts.push(synthesisContext);
  }

  if (parts.length === 0) {
    process.stdout.write('{}');
    process.exit(0);
  }

  process.stdout.write(JSON.stringify({ additional_context: parts.join('\n\n') }));
  process.exit(0);
});
