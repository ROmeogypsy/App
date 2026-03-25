#!/usr/bin/env node
/**
 * run-assessment.js — Full Assessment Orchestrator
 *
 * Runs all modules in sequence and produces a timestamped results folder:
 *   results/<timestamp>/
 *     url-enum.json
 *     endpoint-map.json
 *     attack-paths.json
 *     report.html
 *
 * Usage:
 *   node security/run-assessment.js [--ollama-host HOST:PORT] [--app-host HOST:PORT]
 *
 * Defaults:
 *   --ollama-host  localhost:11434
 *   --app-host     localhost:5173
 *
 * AUTHORISED USE ONLY.
 */

import { join } from 'path';
import { mkdir } from 'fs/promises';

import { enumerateURLs }    from './url-enum.js';
import { mapEndpoints }     from './endpoint-mapper.js';
import { buildAttackPaths } from './attack-path-mapper.js';
import { generateReport }   from './report-gen.js';

// ── CLI args ──────────────────────────────────────────────────────────────────
const args = process.argv.slice(2);
function getArg(flag, fallback) {
  const idx = args.indexOf(flag);
  return idx !== -1 && args[idx + 1] ? args[idx + 1] : fallback;
}
const OLLAMA_HOST = getArg('--ollama-host', 'localhost:11434');
const APP_HOST    = getArg('--app-host',    'localhost:5173');
const OLLAMA_BASE = `http://${OLLAMA_HOST}`;
const APP_BASE    = `http://${APP_HOST}`;

// ── Output directory ──────────────────────────────────────────────────────────
const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19);
const outDir    = join(process.cwd(), 'security', 'results', timestamp);

// ── Banner ────────────────────────────────────────────────────────────────────
console.log('\x1b[1m\x1b[36m');
console.log('╔══════════════════════════════════════════════════════════════╗');
console.log('║           NotesAI Security Assessment Toolkit               ║');
console.log('║           AUTHORISED USE ONLY — CONFIDENTIAL                ║');
console.log('╚══════════════════════════════════════════════════════════════╝');
console.log('\x1b[0m');
console.log(`Ollama: ${OLLAMA_BASE}`);
console.log(`App:    ${APP_BASE}`);
console.log(`Output: ${outDir}`);
console.log('─'.repeat(66));

await mkdir(outDir, { recursive: true });

// ── Phase 1: URL Enumeration ──────────────────────────────────────────────────
const { interesting } = await enumerateURLs(APP_BASE, OLLAMA_BASE, outDir);

// ── Phase 2: Deep Endpoint Mapping ───────────────────────────────────────────
const endpointResults = await mapEndpoints(OLLAMA_BASE, outDir);

// ── Phase 3: Attack Path Analysis ────────────────────────────────────────────
const attackPaths = await buildAttackPaths(endpointResults, outDir);

// ── Phase 4: HTML Report ──────────────────────────────────────────────────────
const reportPath = await generateReport(outDir, { appBase: APP_BASE, ollamaBase: OLLAMA_BASE });

// ── Final Summary ─────────────────────────────────────────────────────────────
const exploitable = attackPaths.filter(p => p.exploitable);
const highOpen    = endpointResults.filter(r => r.classification === 'OPEN' && r.severity === 'HIGH');

console.log('\n\x1b[1m\x1b[36m▶ Assessment Complete\x1b[0m');
console.log('─'.repeat(66));
console.log(`  Accessible URLs:          ${interesting.length}`);
console.log(`  Open Ollama endpoints:    ${endpointResults.filter(r=>r.classification==='OPEN').length}`);
console.log(`  HIGH-severity open:       \x1b[31m${highOpen.length}\x1b[0m`);
console.log(`  Fully exploitable paths:  \x1b[31m${exploitable.length}\x1b[0m`);
console.log(`\n  \x1b[32mHTML Report:\x1b[0m  ${reportPath}`);
console.log(`  \x1b[32mEvidence dir:\x1b[0m ${outDir}`);
console.log('─'.repeat(66));
