/**
 * url-enum.js — URL Path Enumerator
 *
 * Probes both the Vite app server and the Ollama API for accessible paths
 * using a built-in wordlist of common routes, admin panels, debug endpoints,
 * and framework-specific paths.
 *
 * AUTHORISED USE ONLY.
 */

import { mkdir, writeFile } from 'fs/promises';
import { join } from 'path';

export const APP_WORDLIST = [
  // SPA / React common
  '/', '/index.html', '/manifest.json', '/robots.txt', '/sitemap.xml',
  '/favicon.ico', '/assets/', '/static/',
  // Common admin / debug
  '/admin', '/admin/', '/admin/login', '/dashboard', '/debug',
  '/status', '/health', '/healthz', '/ping', '/metrics',
  '/env', '/.env', '/.env.local', '/.env.production',
  // Common framework files
  '/vite.config.js', '/package.json', '/webpack.config.js',
  '/config.js', '/config.json', '/settings.json',
  // Source disclosure
  '/src/', '/src/main.jsx', '/src/App.jsx',
  // Backup / log files
  '/backup', '/logs', '/error.log', '/access.log',
  // Auth endpoints
  '/login', '/logout', '/register', '/auth', '/api/auth',
  '/api/login', '/api/token', '/api/user', '/api/users',
  // Common API paths
  '/api/', '/api/v1/', '/api/v2/', '/graphql', '/api/graphql',
  // Vite-specific
  '/@vite/client', '/@react-refresh', '/node_modules/.vite/',
];

export const OLLAMA_WORDLIST = [
  // Official Ollama REST API
  '/api/tags',
  '/api/version',
  '/api/ps',
  '/api/chat',
  '/api/generate',
  '/api/pull',
  '/api/push',
  '/api/delete',
  '/api/copy',
  '/api/create',
  '/api/show',
  '/api/embeddings',
  '/api/embed',
  // OpenAI-compatible shim (Ollama >= 0.1.24)
  '/v1/models',
  '/v1/chat/completions',
  '/v1/completions',
  '/v1/embeddings',
  // Debug / undocumented
  '/debug',
  '/metrics',
  '/health',
  '/healthz',
  '/',
];

const STATUS_LABELS = {
  200: 'OK',
  201: 'Created',
  204: 'No Content',
  301: 'Redirect',
  302: 'Redirect',
  400: 'Bad Request',
  401: 'Unauthorised',
  403: 'Forbidden',
  404: 'Not Found',
  405: 'Method Not Allowed',
  500: 'Server Error',
};

function statusColor(s) {
  if (s === null)        return '\x1b[90m';  // grey  — unreachable
  if (s < 300)          return '\x1b[32m';  // green — success
  if (s < 400)          return '\x1b[36m';  // cyan  — redirect
  if (s === 401 || s === 403) return '\x1b[33m'; // yellow — auth-gated
  if (s >= 500)         return '\x1b[31m';  // red   — server error
  return '\x1b[90m';
}
const RESET = '\x1b[0m';

async function probe(baseUrl, path, method = 'GET', body = null, timeoutMs = 5000) {
  const ctrl = new AbortController();
  const timer = setTimeout(() => ctrl.abort(), timeoutMs);
  const url = `${baseUrl}${path}`;
  try {
    const opts = { method, signal: ctrl.signal, headers: {} };
    if (body) {
      opts.headers['Content-Type'] = 'application/json';
      opts.body = body;
    }
    const res = await fetch(url, opts);
    clearTimeout(timer);
    const contentType = res.headers.get('content-type') ?? '';
    let snippet = '';
    try {
      const text = await res.text();
      snippet = text.slice(0, 200).replace(/\n/g, ' ');
    } catch { /* ignore */ }
    return {
      url, method, status: res.status,
      contentType, snippet,
      headers: Object.fromEntries(res.headers.entries()),
    };
  } catch (err) {
    clearTimeout(timer);
    return { url, method, status: null, contentType: '', snippet: err.message, headers: {} };
  }
}

function isInteresting(result) {
  const { status, path } = result;
  if (status === null) return false;
  if (status === 404)  return false;
  return true;
}

export async function enumerateURLs(appBase, ollamaBase, outDir) {
  console.log('\n\x1b[1m\x1b[36m▶ URL Enumeration\x1b[0m');
  console.log(`  App:    ${appBase}`);
  console.log(`  Ollama: ${ollamaBase}`);

  const appResults    = [];
  const ollamaResults = [];

  // ── App server ──────────────────────────────────────────────────────────────
  console.log('\n  \x1b[1mApp Server\x1b[0m');
  for (const path of APP_WORDLIST) {
    const r = await probe(appBase, path);
    r.path = path;
    r.target = 'app';
    appResults.push(r);
    const c = statusColor(r.status);
    const label = r.status ? `${r.status} ${STATUS_LABELS[r.status] ?? ''}` : 'TIMEOUT';
    console.log(`  ${c}${label.padEnd(20)}${RESET} ${path}`);
  }

  // ── Ollama API ──────────────────────────────────────────────────────────────
  console.log('\n  \x1b[1mOllama API\x1b[0m');
  for (const path of OLLAMA_WORDLIST) {
    // Try GET first, then POST for write endpoints
    const isWritePath = ['/api/pull','/api/push','/api/delete','/api/copy','/api/create',
                         '/api/chat','/api/generate','/api/show','/api/embeddings','/api/embed',
                         '/v1/chat/completions','/v1/completions','/v1/embeddings'].includes(path);
    const method = isWritePath ? 'POST' : 'GET';
    const body   = isWritePath ? JSON.stringify({ model: '_probe_', name: '_probe_', messages: [] }) : null;
    const r      = await probe(ollamaBase, path, method, body);
    r.path = path;
    r.target = 'ollama';
    ollamaResults.push(r);
    const c = statusColor(r.status);
    const label = r.status ? `${r.status} ${STATUS_LABELS[r.status] ?? ''}` : 'TIMEOUT';
    console.log(`  ${c}${method.padEnd(5)} ${label.padEnd(20)}${RESET} ${path}`);
  }

  const allResults = [...appResults, ...ollamaResults];
  const interesting = allResults.filter(isInteresting);

  console.log(`\n  Interesting paths (non-404, reachable): ${interesting.length}`);

  // Save raw JSON evidence
  if (outDir) {
    await mkdir(outDir, { recursive: true });
    await writeFile(join(outDir, 'url-enum.json'),
      JSON.stringify({ timestamp: new Date().toISOString(), results: allResults }, null, 2)
    );
    console.log(`  Saved → ${outDir}/url-enum.json`);
  }

  return { appResults, ollamaResults, interesting };
}
