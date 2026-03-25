#!/usr/bin/env node
/**
 * Security Assessment Script — NotesAI / Ollama Attack Surface
 *
 * Scope:
 *   - Local Ollama API endpoint reachability and access controls
 *   - Unauthenticated endpoint enumeration (GET + POST)
 *   - CORS policy evaluation
 *   - Security header audit on the Vite dev server
 *   - Client-side localStorage exposure check (static analysis)
 *
 * Usage:
 *   node security-assessment.js [--ollama-host <host:port>] [--app-host <host:port>]
 *
 * Defaults:
 *   --ollama-host  localhost:11434
 *   --app-host     localhost:5173
 *
 * AUTHORISED USE ONLY — run only against systems you own or have written
 * permission to test.
 */

import { readFileSync } from 'fs';
import { resolve } from 'path';

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

// ── Helpers ───────────────────────────────────────────────────────────────────
const RESET  = '\x1b[0m';
const RED    = '\x1b[31m';
const YELLOW = '\x1b[33m';
const GREEN  = '\x1b[32m';
const CYAN   = '\x1b[36m';
const BOLD   = '\x1b[1m';

function pass(msg)  { console.log(`  ${GREEN}[PASS]${RESET}  ${msg}`); }
function fail(msg)  { console.log(`  ${RED}[FAIL]${RESET}  ${msg}`); }
function warn(msg)  { console.log(`  ${YELLOW}[WARN]${RESET}  ${msg}`); }
function info(msg)  { console.log(`  ${CYAN}[INFO]${RESET}  ${msg}`); }
function head(msg)  { console.log(`\n${BOLD}${CYAN}▶ ${msg}${RESET}`); }

const findings = [];
function finding(severity, id, title, detail, recommendation) {
  findings.push({ severity, id, title, detail, recommendation });
  const color = severity === 'HIGH' ? RED : severity === 'MED' ? YELLOW : CYAN;
  console.log(`  ${color}[${severity}]${RESET}  ${BOLD}${id}${RESET}: ${title}`);
  if (detail)         console.log(`           Detail: ${detail}`);
  if (recommendation) console.log(`           Fix:    ${recommendation}`);
}

async function tryFetch(url, options = {}, timeoutMs = 5000) {
  const ctrl = new AbortController();
  const timer = setTimeout(() => ctrl.abort(), timeoutMs);
  try {
    const res = await fetch(url, { ...options, signal: ctrl.signal });
    clearTimeout(timer);
    return res;
  } catch (err) {
    clearTimeout(timer);
    return null;   // null = unreachable / timeout
  }
}

// ── 1. Ollama Reachability ────────────────────────────────────────────────────
async function checkOllamaReachability() {
  head('1. Ollama API Reachability');

  const res = await tryFetch(`${OLLAMA_BASE}/api/tags`);
  if (!res) {
    warn(`Ollama not reachable at ${OLLAMA_HOST} — skipping Ollama checks`);
    return false;
  }
  if (res.ok) {
    info(`Ollama is UP at ${OLLAMA_HOST} (HTTP ${res.status})`);
    return true;
  }
  warn(`Ollama responded with HTTP ${res.status}`);
  return true;
}

// ── 2. Ollama Endpoint Enumeration ───────────────────────────────────────────
const OLLAMA_ENDPOINTS = [
  // Read / enumeration
  { method: 'GET',  path: '/api/tags',             desc: 'List installed models'   },
  { method: 'GET',  path: '/api/version',           desc: 'Ollama version info'     },
  { method: 'GET',  path: '/api/ps',                desc: 'Running model processes' },
  // Write / dangerous
  { method: 'POST', path: '/api/pull',              desc: 'Pull a model from registry', body: JSON.stringify({ name: '_probe_' }) },
  { method: 'POST', path: '/api/delete',            desc: 'Delete a model',             body: JSON.stringify({ name: '_probe_' }) },
  { method: 'POST', path: '/api/copy',              desc: 'Copy a model',               body: JSON.stringify({ source: '_a_', destination: '_b_' }) },
  { method: 'POST', path: '/api/create',            desc: 'Create a model',             body: JSON.stringify({ name: '_probe_', modelfile: 'FROM scratch' }) },
  { method: 'POST', path: '/api/push',              desc: 'Push model to registry',     body: JSON.stringify({ name: '_probe_' }) },
  // Blob / model data
  { method: 'GET',  path: '/api/blobs/sha256:0000', desc: 'Raw blob access'         },
  // Generate / chat (app-used)
  { method: 'POST', path: '/api/chat',              desc: 'Chat (used by app)',       body: JSON.stringify({ model: '_probe_', messages: [] }) },
  { method: 'POST', path: '/api/generate',          desc: 'Text generation',          body: JSON.stringify({ model: '_probe_', prompt: '' }) },
  // OpenAI-compatible shim (Ollama ≥0.1.24)
  { method: 'GET',  path: '/v1/models',             desc: 'OpenAI-compat model list' },
];

const DANGEROUS_PATHS = new Set(['/api/pull', '/api/delete', '/api/copy', '/api/create', '/api/push']);

async function enumerateOllamaEndpoints() {
  head('2. Ollama Endpoint Enumeration (no auth probe)');

  const results = [];
  for (const ep of OLLAMA_ENDPOINTS) {
    const opts = { method: ep.method, headers: { 'Content-Type': 'application/json' } };
    if (ep.body) opts.body = ep.body;

    const res = await tryFetch(`${OLLAMA_BASE}${ep.path}`, opts);
    if (!res) {
      info(`${ep.method.padEnd(4)} ${ep.path.padEnd(35)} → unreachable`);
      results.push({ ...ep, status: null });
      continue;
    }

    const reachable = res.status !== 404 && res.status !== 0;
    const status    = res.status;
    info(`${ep.method.padEnd(4)} ${ep.path.padEnd(35)} → HTTP ${status}  (${ep.desc})`);
    results.push({ ...ep, status });

    // Flag unauthenticated access to dangerous endpoints
    if (DANGEROUS_PATHS.has(ep.path) && status < 500) {
      finding(
        'HIGH',
        `OLLAMA-UNAUTH-${ep.path.replace(/\//g, '-').toUpperCase().slice(1)}`,
        `Unauthenticated access to ${ep.path}`,
        `HTTP ${status} — endpoint accepted request without credentials`,
        'Set OLLAMA_HOST=127.0.0.1 (bind loopback only) and add a reverse proxy ' +
        'with authentication if remote access is needed.'
      );
    }
  }
  return results;
}

// ── 3. CORS Policy ────────────────────────────────────────────────────────────
async function checkCORS() {
  head('3. CORS Policy on Ollama API');

  const origins = [
    'https://attacker.example.com',
    'http://evil.local',
    'null',
  ];

  for (const origin of origins) {
    const res = await tryFetch(`${OLLAMA_BASE}/api/tags`, {
      headers: { Origin: origin, 'Access-Control-Request-Method': 'POST' },
    });
    if (!res) { warn(`Could not reach Ollama for CORS probe (origin: ${origin})`); continue; }

    const acao  = res.headers.get('access-control-allow-origin')      ?? '(not set)';
    const acac  = res.headers.get('access-control-allow-credentials')  ?? '(not set)';
    info(`Origin: ${origin.padEnd(35)} ACAO: ${acao}  ACAC: ${acac}`);

    if (acao === '*') {
      finding(
        'HIGH', 'CORS-WILDCARD',
        'Ollama allows any origin (Access-Control-Allow-Origin: *)',
        `Any web page can make cross-origin requests to the Ollama API.`,
        'Restrict CORS to trusted origins or disable remote access entirely.'
      );
    } else if (acao === origin || acao === 'null') {
      finding(
        'MED', 'CORS-REFLECTED',
        `Ollama reflects supplied Origin header (${origin})`,
        `The API echoes back the caller's Origin, permitting cross-site requests.`,
        'Set an explicit allowlist of trusted origins.'
      );
    }
  }
}

// ── 4. Security Headers on Vite Dev Server ────────────────────────────────────
const EXPECTED_HEADERS = {
  'content-security-policy':          { severity: 'HIGH', rec: "Add a Content-Security-Policy header to restrict script/connect sources." },
  'x-content-type-options':           { severity: 'MED',  rec: "Set X-Content-Type-Options: nosniff." },
  'x-frame-options':                  { severity: 'MED',  rec: "Set X-Frame-Options: DENY to prevent clickjacking." },
  'strict-transport-security':        { severity: 'LOW',  rec: "Set HSTS when serving over HTTPS." },
  'permissions-policy':               { severity: 'LOW',  rec: "Add Permissions-Policy to limit browser feature access." },
  'referrer-policy':                  { severity: 'LOW',  rec: "Set Referrer-Policy: strict-origin-when-cross-origin." },
};

async function checkSecurityHeaders() {
  head('4. Security Headers — Vite Dev Server');

  const res = await tryFetch(APP_BASE);
  if (!res) {
    warn(`App dev server not reachable at ${APP_HOST} — skipping header checks`);
    return;
  }

  for (const [header, meta] of Object.entries(EXPECTED_HEADERS)) {
    const val = res.headers.get(header);
    if (!val) {
      finding(
        meta.severity,
        `HEADER-MISSING-${header.toUpperCase().replace(/-/g, '_')}`,
        `Missing ${header}`,
        `Header not present in response from ${APP_BASE}`,
        meta.rec
      );
    } else {
      pass(`${header}: ${val}`);
    }
  }

  // Check for debug/dev info leakage
  const server = res.headers.get('server') ?? '';
  const via    = res.headers.get('via')    ?? '';
  if (server) warn(`Server header exposed: "${server}" — reveals stack info`);
  if (via)    warn(`Via header exposed: "${via}"`);
}

// ── 5. Static Analysis — localStorage Exposure ───────────────────────────────
async function checkLocalStorageExposure() {
  head('5. Static Analysis — localStorage & Data Exposure');

  const filesToCheck = [
    'src/hooks/useNotes.js',
    'src/hooks/useChat.js',
    'src/App.jsx',
  ];

  for (const rel of filesToCheck) {
    let src;
    try { src = readFileSync(resolve(rel), 'utf8'); } catch { continue; }

    // localStorage with no encryption
    if (/localStorage\.setItem/.test(src) && !/encrypt|crypto|btoa/.test(src)) {
      finding(
        'MED', `LOCALSTORAGE-PLAINTEXT-${rel.replace(/\W/g, '_').toUpperCase()}`,
        `Plaintext localStorage writes in ${rel}`,
        'Note content is stored unencrypted in the browser. Anyone with physical ' +
        'or extension-level access to the browser can read all notes.',
        'Consider encrypting note content with SubtleCrypto before storage, ' +
        'especially if notes contain sensitive data.'
      );
    }

    // Ollama base URL hardcoded to localhost — not a vulnerability but noteworthy
    if (/localhost:\d+/.test(src)) {
      const matches = src.match(/localhost:\d+/g) ?? [];
      info(`${rel}: hardcoded localhost reference(s): ${[...new Set(matches)].join(', ')}`);
    }

    // Note context injected into LLM prompt without sanitisation
    if (/noteContext/.test(src) && /systemPrompt/.test(src) && !/sanitize|escape/.test(src)) {
      finding(
        'MED', 'PROMPT-INJECTION',
        `Unsanitised note content injected into LLM system prompt (${rel})`,
        'User-controlled note text is embedded directly into the Ollama system prompt. ' +
        'A malicious note could attempt to override AI instructions (prompt injection).',
        'Treat note content as untrusted user input; consider delimiters, ' +
        'role-separation, or validation before injecting into the prompt.'
      );
    }
  }

  pass('Static analysis complete');
}

// ── 6. Model Enumeration ──────────────────────────────────────────────────────
async function enumerateModels() {
  head('6. Model Enumeration (information disclosure)');

  const res = await tryFetch(`${OLLAMA_BASE}/api/tags`);
  if (!res || !res.ok) { warn('Could not reach /api/tags'); return; }

  let data;
  try { data = await res.json(); } catch { warn('Non-JSON response from /api/tags'); return; }

  const models = (data.models ?? []);
  if (models.length === 0) {
    info('No models installed (or empty response)');
    return;
  }

  finding(
    'LOW', 'MODEL-ENUM',
    `${models.length} model(s) enumerable without authentication`,
    models.map(m => `${m.name} (${Math.round((m.size ?? 0) / 1e9 * 10) / 10} GB)`).join(', '),
    'If Ollama must be network-accessible, add a reverse-proxy auth layer.'
  );
}

// ── Report ────────────────────────────────────────────────────────────────────
function printReport() {
  head('Assessment Summary');

  const bySeverity = { HIGH: [], MED: [], LOW: [] };
  for (const f of findings) (bySeverity[f.severity] ?? bySeverity.LOW).push(f);

  console.log(`\n  ${RED}HIGH${RESET}  ${bySeverity.HIGH.length} finding(s)`);
  console.log(`  ${YELLOW}MED${RESET}   ${bySeverity.MED.length} finding(s)`);
  console.log(`  ${CYAN}LOW${RESET}   ${bySeverity.LOW.length} finding(s)`);
  console.log(`\n  Total findings: ${findings.length}`);

  if (findings.length === 0) {
    console.log(`\n  ${GREEN}No issues found within scope.${RESET}`);
    return;
  }

  console.log(`\n${BOLD}Findings Detail${RESET}`);
  console.log('─'.repeat(70));
  for (const f of findings) {
    const color = f.severity === 'HIGH' ? RED : f.severity === 'MED' ? YELLOW : CYAN;
    console.log(`\n  [${color}${f.severity}${RESET}] ${BOLD}${f.id}${RESET}`);
    console.log(`  Title:  ${f.title}`);
    console.log(`  Detail: ${f.detail}`);
    console.log(`  Fix:    ${f.recommendation}`);
  }
  console.log('\n' + '─'.repeat(70));
}

// ── Main ──────────────────────────────────────────────────────────────────────
(async () => {
  console.log(`\n${BOLD}NotesAI / Ollama — Security Assessment${RESET}`);
  console.log(`Target Ollama: ${OLLAMA_BASE}`);
  console.log(`Target App:    ${APP_BASE}`);
  console.log('─'.repeat(70));

  const ollamaUp = await checkOllamaReachability();
  if (ollamaUp) {
    await enumerateOllamaEndpoints();
    await checkCORS();
    await enumerateModels();
  }

  await checkSecurityHeaders();
  await checkLocalStorageExposure();

  printReport();
})();
