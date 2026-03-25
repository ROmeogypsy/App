/**
 * endpoint-mapper.js — Deep API Endpoint Mapper
 *
 * For each reachable endpoint:
 *   1. Records the HTTP status, response headers, and response body snippet
 *   2. Classifies the endpoint (auth-required, open, error-leaking, etc.)
 *   3. Captures full response as POC evidence
 *
 * AUTHORISED USE ONLY.
 */

import { mkdir, writeFile } from 'fs/promises';
import { join } from 'path';

const RESET  = '\x1b[0m';
const RED    = '\x1b[31m';
const YELLOW = '\x1b[33m';
const GREEN  = '\x1b[32m';
const CYAN   = '\x1b[36m';
const BOLD   = '\x1b[1m';

// Endpoint definitions with expected schema and risk notes
const OLLAMA_ENDPOINT_DEFS = [
  {
    method: 'GET',  path: '/api/tags',
    desc: 'List all installed models',
    risk: 'INFO-DISCLOSURE',
    riskNote: 'Reveals installed model names and sizes without authentication',
    body: null,
  },
  {
    method: 'GET',  path: '/api/version',
    desc: 'Ollama server version',
    risk: 'INFO-DISCLOSURE',
    riskNote: 'Exposes server version — aids targeted exploitation',
    body: null,
  },
  {
    method: 'GET',  path: '/api/ps',
    desc: 'Running model processes',
    risk: 'INFO-DISCLOSURE',
    riskNote: 'Shows active model processes and resource usage',
    body: null,
  },
  {
    method: 'GET',  path: '/v1/models',
    desc: 'OpenAI-compat model list',
    risk: 'INFO-DISCLOSURE',
    riskNote: 'Alternative enumeration path for installed models',
    body: null,
  },
  {
    method: 'POST', path: '/api/generate',
    desc: 'Text generation (streaming)',
    risk: 'UNAUTH-GENERATE',
    riskNote: 'Any caller can generate text using any installed model — compute abuse',
    body: JSON.stringify({ model: 'llama3', prompt: 'Say "PROBE_OK"', stream: false }),
  },
  {
    method: 'POST', path: '/api/chat',
    desc: 'Chat completion (streaming)',
    risk: 'UNAUTH-GENERATE',
    riskNote: 'Chat completions accessible without auth — used by NotesAI app',
    body: JSON.stringify({
      model: 'llama3',
      messages: [{ role: 'user', content: 'Say "PROBE_OK"' }],
      stream: false,
    }),
  },
  {
    method: 'POST', path: '/v1/chat/completions',
    desc: 'OpenAI-compat chat endpoint',
    risk: 'UNAUTH-GENERATE',
    riskNote: 'OpenAI-compatible endpoint; any OpenAI SDK client can use it unauthenticated',
    body: JSON.stringify({
      model: 'llama3',
      messages: [{ role: 'user', content: 'Say "PROBE_OK"' }],
    }),
  },
  {
    method: 'POST', path: '/api/pull',
    desc: 'Pull model from registry',
    risk: 'UNAUTH-WRITE',
    riskNote: 'Unauthenticated pull can download arbitrary models, consuming disk and bandwidth',
    body: JSON.stringify({ name: '_probe_nonexistent_model_xyz_' }),
  },
  {
    method: 'POST', path: '/api/delete',
    desc: 'Delete installed model',
    risk: 'UNAUTH-DESTRUCTIVE',
    riskNote: 'Can permanently delete installed models without authentication',
    body: JSON.stringify({ name: '_probe_nonexistent_model_xyz_' }),
  },
  {
    method: 'POST', path: '/api/copy',
    desc: 'Copy / rename a model',
    risk: 'UNAUTH-WRITE',
    riskNote: 'Can duplicate models without authentication',
    body: JSON.stringify({ source: '_probe_src_', destination: '_probe_dst_' }),
  },
  {
    method: 'POST', path: '/api/create',
    desc: 'Create model from Modelfile',
    risk: 'UNAUTH-WRITE',
    riskNote: 'Arbitrary model creation from a Modelfile, including system-prompt override',
    body: JSON.stringify({ name: '_probe_custom_', modelfile: 'FROM scratch' }),
  },
  {
    method: 'POST', path: '/api/show',
    desc: 'Show model details',
    risk: 'INFO-DISCLOSURE',
    riskNote: 'Returns full model metadata including system prompt and template',
    body: JSON.stringify({ name: '_probe_' }),
  },
  {
    method: 'POST', path: '/api/embeddings',
    desc: 'Generate embeddings (legacy)',
    risk: 'UNAUTH-GENERATE',
    riskNote: 'Unauth embedding generation; compute abuse vector',
    body: JSON.stringify({ model: '_probe_', prompt: 'test' }),
  },
  {
    method: 'POST', path: '/api/embed',
    desc: 'Generate embeddings (v2)',
    risk: 'UNAUTH-GENERATE',
    riskNote: 'Same as /api/embeddings but newer endpoint',
    body: JSON.stringify({ model: '_probe_', input: 'test' }),
  },
];

const RISK_SEVERITY = {
  'UNAUTH-DESTRUCTIVE': 'HIGH',
  'UNAUTH-WRITE':       'HIGH',
  'UNAUTH-GENERATE':    'HIGH',
  'INFO-DISCLOSURE':    'MED',
};

async function probeEndpoint(baseUrl, def, timeoutMs = 8000) {
  const ctrl = new AbortController();
  const timer = setTimeout(() => ctrl.abort(), timeoutMs);
  const url = `${baseUrl}${def.path}`;
  const startMs = Date.now();
  try {
    const res = await fetch(url, {
      method: def.method,
      signal: ctrl.signal,
      headers: { 'Content-Type': 'application/json' },
      ...(def.body ? { body: def.body } : {}),
    });
    clearTimeout(timer);
    const durationMs = Date.now() - startMs;

    const rawBody = await res.text().catch(() => '');
    const bodySnippet = rawBody.slice(0, 500);

    // Classify response
    let classification = 'BLOCKED';
    if (res.status === 200 || res.status === 201) classification = 'OPEN';
    else if (res.status === 401 || res.status === 403) classification = 'AUTH-REQUIRED';
    else if (res.status === 404) classification = 'NOT-FOUND';
    else if (res.status === 405) classification = 'METHOD-NOT-ALLOWED';
    else if (res.status >= 400 && res.status < 500) classification = 'CLIENT-ERROR';
    else if (res.status >= 500) classification = 'SERVER-ERROR';

    return {
      ...def,
      url,
      status: res.status,
      durationMs,
      classification,
      headers: Object.fromEntries(res.headers.entries()),
      bodySnippet,
      severity: classification === 'OPEN' ? (RISK_SEVERITY[def.risk] ?? 'LOW') : 'INFO',
      timestamp: new Date().toISOString(),
    };
  } catch (err) {
    clearTimeout(timer);
    return {
      ...def,
      url,
      status: null,
      durationMs: Date.now() - startMs,
      classification: 'UNREACHABLE',
      headers: {},
      bodySnippet: err.message,
      severity: 'INFO',
      timestamp: new Date().toISOString(),
    };
  }
}

function severityColor(s) {
  if (s === 'HIGH') return RED;
  if (s === 'MED')  return YELLOW;
  if (s === 'LOW')  return CYAN;
  return '\x1b[90m';
}

function classColor(c) {
  if (c === 'OPEN')              return GREEN;
  if (c === 'AUTH-REQUIRED')     return YELLOW;
  if (c === 'SERVER-ERROR')      return RED;
  if (c === 'UNREACHABLE')       return '\x1b[90m';
  return '\x1b[37m';
}

export async function mapEndpoints(ollamaBase, outDir) {
  console.log('\n\x1b[1m\x1b[36m▶ Endpoint Mapper\x1b[0m');
  console.log(`  Target: ${ollamaBase}`);
  console.log('  ' + '─'.repeat(65));

  const results = [];

  for (const def of OLLAMA_ENDPOINT_DEFS) {
    const r = await probeEndpoint(ollamaBase, def);
    results.push(r);

    const sc    = severityColor(r.severity);
    const cc    = classColor(r.classification);
    const sev   = r.severity !== 'INFO' ? ` ${sc}[${r.severity}]${RESET}` : '';
    const http  = r.status ? `HTTP ${r.status}` : 'TIMEOUT';
    console.log(
      `  ${r.method.padEnd(5)} ${r.path.padEnd(30)} ` +
      `${cc}${r.classification.padEnd(18)}${RESET}` +
      `${http.padEnd(10)}${sev}`
    );
    if (r.classification === 'OPEN' && r.bodySnippet) {
      console.log(`         ${'\x1b[90m'}snippet: ${r.bodySnippet.slice(0, 80).replace(/\n/g,' ')}${RESET}`);
    }
  }

  const open = results.filter(r => r.classification === 'OPEN');
  console.log(`\n  Open/accessible endpoints: ${open.length} / ${results.length}`);

  if (outDir) {
    await mkdir(outDir, { recursive: true });
    await writeFile(join(outDir, 'endpoint-map.json'),
      JSON.stringify({ timestamp: new Date().toISOString(), ollamaBase, results }, null, 2)
    );
    console.log(`  Saved → ${outDir}/endpoint-map.json`);
  }

  return results;
}
