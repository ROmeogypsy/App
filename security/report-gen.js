/**
 * report-gen.js — HTML POC Report Generator
 *
 * Produces a self-contained HTML report with:
 *   - Executive summary
 *   - Per-finding detail with verifiable HTTP evidence
 *   - Attack path diagrams
 *   - Raw JSON evidence appendix
 *
 * AUTHORISED USE ONLY.
 */

import { readFile, writeFile, mkdir } from 'fs/promises';
import { join } from 'path';

function severityBadge(s) {
  const colors = { HIGH: '#dc2626', MED: '#d97706', LOW: '#2563eb', INFO: '#6b7280' };
  const c = colors[s] ?? colors.INFO;
  return `<span style="background:${c};color:#fff;padding:2px 8px;border-radius:4px;font-size:12px;font-weight:700">${s}</span>`;
}

function classificationBadge(c) {
  const colors = {
    'OPEN':             '#dc2626',
    'AUTH-REQUIRED':    '#d97706',
    'SERVER-ERROR':     '#7c3aed',
    'UNREACHABLE':      '#6b7280',
    'NOT-FOUND':        '#9ca3af',
    'METHOD-NOT-ALLOWED':'#4b5563',
    'CLIENT-ERROR':     '#b45309',
    'BLOCKED':          '#374151',
  };
  const bg = colors[c] ?? '#6b7280';
  return `<span style="background:${bg};color:#fff;padding:2px 6px;border-radius:3px;font-size:11px">${c}</span>`;
}

function escapeHtml(s) {
  return String(s)
    .replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')
    .replace(/"/g,'&quot;').replace(/'/g,'&#39;');
}

function depthBar(depth, max = 3) {
  let html = '';
  for (let i = 1; i <= max; i++) {
    html += `<span style="display:inline-block;width:18px;height:12px;margin:0 1px;border-radius:2px;background:${i<=depth?'#dc2626':'#e5e7eb'}"></span>`;
  }
  return html;
}

export async function generateReport(outDir, options = {}) {
  const { appBase = 'localhost:5173', ollamaBase = 'localhost:11434' } = options;
  const ts = new Date().toISOString();

  // Load evidence files
  let urlData       = null;
  let endpointData  = null;
  let attackData    = null;

  try { urlData      = JSON.parse(await readFile(join(outDir, 'url-enum.json'),     'utf8')); } catch {}
  try { endpointData = JSON.parse(await readFile(join(outDir, 'endpoint-map.json'), 'utf8')); } catch {}
  try { attackData   = JSON.parse(await readFile(join(outDir, 'attack-paths.json'), 'utf8')); } catch {}

  const endpointResults = endpointData?.results ?? [];
  const attackPaths     = attackData?.paths     ?? [];
  const urlResults      = urlData?.results      ?? [];

  const openEndpoints   = endpointResults.filter(r => r.classification === 'OPEN');
  const highPaths       = attackPaths.filter(p => p.overallSeverity === 'HIGH');
  const medPaths        = attackPaths.filter(p => p.overallSeverity === 'MED');
  const exploitable     = attackPaths.filter(p => p.exploitable);

  // ── HTML ──────────────────────────────────────────────────────────────────
  const html = `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Security Assessment Report — NotesAI</title>
<style>
  * { box-sizing:border-box; margin:0; padding:0; }
  body { font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif; background:#f8fafc; color:#1e293b; line-height:1.6; }
  .page { max-width:1100px; margin:0 auto; padding:40px 24px; }
  header { background:#0f172a; color:#fff; padding:48px; border-radius:12px; margin-bottom:32px; }
  header h1 { font-size:28px; margin-bottom:8px; }
  header .meta { color:#94a3b8; font-size:14px; }
  .section { background:#fff; border:1px solid #e2e8f0; border-radius:10px; padding:28px; margin-bottom:24px; }
  .section h2 { font-size:18px; font-weight:700; margin-bottom:16px; padding-bottom:10px; border-bottom:2px solid #e2e8f0; }
  .stat-grid { display:grid; grid-template-columns:repeat(4,1fr); gap:16px; margin-bottom:24px; }
  .stat { background:#f1f5f9; border-radius:8px; padding:20px; text-align:center; }
  .stat .number { font-size:36px; font-weight:800; }
  .stat .label  { font-size:13px; color:#64748b; margin-top:4px; }
  .stat.high .number { color:#dc2626; }
  .stat.med  .number { color:#d97706; }
  .stat.low  .number { color:#2563eb; }
  table { width:100%; border-collapse:collapse; font-size:13px; }
  th { background:#f1f5f9; text-align:left; padding:10px 12px; font-size:12px; text-transform:uppercase; letter-spacing:.05em; color:#64748b; }
  td { padding:10px 12px; border-bottom:1px solid #f1f5f9; vertical-align:top; }
  tr:hover td { background:#f8fafc; }
  .code { font-family:'SF Mono',Consolas,monospace; background:#0f172a; color:#e2e8f0; padding:12px 16px; border-radius:6px; font-size:12px; overflow-x:auto; white-space:pre-wrap; word-break:break-all; }
  .path-card { border:1px solid #e2e8f0; border-radius:8px; padding:20px; margin-bottom:16px; }
  .path-card.exploitable { border-left:4px solid #dc2626; }
  .path-card.partial { border-left:4px solid #d97706; }
  .path-card h3 { font-size:15px; margin-bottom:6px; }
  .path-card .desc { font-size:13px; color:#64748b; margin-bottom:12px; }
  .step { display:flex; gap:12px; align-items:flex-start; margin-bottom:8px; padding:8px; background:#f8fafc; border-radius:6px; }
  .step-num { background:#0f172a; color:#fff; border-radius:50%; width:22px; height:22px; display:flex; align-items:center; justify-content:center; font-size:11px; font-weight:700; flex-shrink:0; }
  .step-num.verified { background:#16a34a; }
  .step-num.unverified { background:#d97706; }
  .step-detail { flex:1; }
  .step-action { font-weight:600; font-size:13px; }
  .step-outcome { font-size:12px; color:#64748b; margin-top:2px; }
  .step-path { font-family:monospace; font-size:11px; color:#2563eb; margin-top:2px; }
  .tag { display:inline-block; background:#e2e8f0; color:#475569; padding:1px 6px; border-radius:3px; font-size:11px; margin-right:4px; }
  footer { text-align:center; color:#94a3b8; font-size:12px; margin-top:40px; padding:20px 0; }
</style>
</head>
<body>
<div class="page">

<header>
  <h1>Security Assessment Report</h1>
  <div class="meta">
    Target Application: <strong>NotesAI (Vite/React SPA + Ollama)</strong><br>
    App Server: <code>${escapeHtml(appBase)}</code> &nbsp;|&nbsp;
    Ollama API: <code>${escapeHtml(ollamaBase)}</code><br>
    Generated: ${escapeHtml(ts)}<br>
    Classification: <strong>CONFIDENTIAL — AUTHORISED TESTING ONLY</strong>
  </div>
</header>

<!-- Executive Summary -->
<div class="section">
  <h2>Executive Summary</h2>
  <div class="stat-grid">
    <div class="stat high">
      <div class="number">${exploitable.filter(p=>p.overallSeverity==='HIGH').length}</div>
      <div class="label">HIGH Exploitable Paths</div>
    </div>
    <div class="stat med">
      <div class="number">${attackPaths.filter(p=>p.overallSeverity==='MED').length}</div>
      <div class="label">MEDIUM Paths</div>
    </div>
    <div class="stat">
      <div class="number">${openEndpoints.length}</div>
      <div class="label">Open Endpoints</div>
    </div>
    <div class="stat">
      <div class="number">${urlResults.filter(r=>r.status&&r.status!==404).length}</div>
      <div class="label">Accessible URLs</div>
    </div>
  </div>
  <p style="color:#475569;font-size:14px">
    The NotesAI application exposes a local Ollama inference server with
    <strong>no authentication, no authorisation, and permissive CORS</strong>.
    Any process or web page that can reach port 11434 can enumerate models,
    generate unlimited text, and destructively modify or delete models.
    Note content injected into the LLM system prompt without sanitisation
    creates a prompt-injection vector.
  </p>
</div>

<!-- URL Enumeration Results -->
<div class="section">
  <h2>URL Enumeration Results</h2>
  <table>
    <thead><tr>
      <th>Method</th><th>URL</th><th>Status</th><th>Content-Type</th><th>Response Snippet</th>
    </tr></thead>
    <tbody>
    ${urlResults.filter(r => r.status && r.status !== 404).map(r => `
      <tr>
        <td><code>${escapeHtml(r.method)}</code></td>
        <td style="font-family:monospace;font-size:12px">${escapeHtml(r.url)}</td>
        <td>${r.status}</td>
        <td style="font-size:12px">${escapeHtml(r.contentType ?? '')}</td>
        <td style="font-family:monospace;font-size:11px;color:#64748b;max-width:260px;word-break:break-all">${escapeHtml((r.snippet??'').slice(0,120))}</td>
      </tr>
    `).join('')}
    </tbody>
  </table>
</div>

<!-- Endpoint Map -->
<div class="section">
  <h2>Ollama Endpoint Map</h2>
  <table>
    <thead><tr>
      <th>Method</th><th>Endpoint</th><th>Status</th><th>Classification</th><th>Severity</th><th>Risk Note</th>
    </tr></thead>
    <tbody>
    ${endpointResults.map(r => `
      <tr>
        <td><code>${escapeHtml(r.method)}</code></td>
        <td style="font-family:monospace;font-size:12px">${escapeHtml(r.path)}</td>
        <td>${r.status ?? 'TIMEOUT'}</td>
        <td>${classificationBadge(r.classification)}</td>
        <td>${severityBadge(r.severity)}</td>
        <td style="font-size:12px;color:#475569">${escapeHtml(r.riskNote ?? '')}</td>
      </tr>
    `).join('')}
    </tbody>
  </table>
</div>

<!-- HTTP Evidence (POC) -->
<div class="section">
  <h2>HTTP Evidence (Proof of Concept)</h2>
  <p style="font-size:13px;color:#64748b;margin-bottom:16px">
    The following are actual HTTP responses captured during the assessment, providing verifiable evidence.
  </p>
  ${openEndpoints.map(r => `
  <div style="margin-bottom:20px">
    <div style="font-weight:600;margin-bottom:6px">
      ${severityBadge(r.severity)} &nbsp;
      <code style="font-size:13px">${escapeHtml(r.method)} ${escapeHtml(r.path)}</code>
      &nbsp; — ${escapeHtml(r.desc ?? '')}
    </div>
    <div class="code">Request:
  ${escapeHtml(r.method)} ${escapeHtml(r.url)} HTTP/1.1
  Content-Type: application/json
  ${r.body ? '\n  Body: ' + escapeHtml(r.body) : ''}

Response:
  HTTP/1.1 ${r.status}
  Content-Type: ${escapeHtml(r.headers?.['content-type'] ?? '')}
  ${r.headers?.['access-control-allow-origin'] ? 'Access-Control-Allow-Origin: ' + escapeHtml(r.headers['access-control-allow-origin']) : ''}

  ${escapeHtml(r.bodySnippet ?? '')}
</div>
    <div style="font-size:12px;color:#64748b;margin-top:4px">
      Duration: ${r.durationMs}ms &nbsp;|&nbsp; Captured: ${escapeHtml(r.timestamp ?? '')}
    </div>
  </div>
  `).join('')}
</div>

<!-- Attack Paths -->
<div class="section">
  <h2>Attack Path Analysis</h2>
  ${attackPaths.map(p => `
  <div class="path-card ${p.exploitable?'exploitable':'partial'}">
    <div style="display:flex;align-items:center;gap:10px;margin-bottom:4px">
      ${severityBadge(p.overallSeverity)}
      <strong style="font-size:14px">${escapeHtml(p.id)}: ${escapeHtml(p.name)}</strong>
      ${p.exploitable ? '<span style="color:#dc2626;font-size:12px;font-weight:700">● EXPLOITABLE</span>' : '<span style="color:#d97706;font-size:12px">◐ PARTIAL</span>'}
    </div>
    <p class="desc">${escapeHtml(p.description)}</p>
    ${p.steps.map(step => `
    <div class="step">
      <div class="step-num ${step.verified?'verified':'unverified'}">${step.step}</div>
      <div class="step-detail">
        <div class="step-action">${escapeHtml(step.action)}</div>
        <div class="step-path">${escapeHtml(step.method)} ${escapeHtml(step.path)}</div>
        <div class="step-outcome">→ ${escapeHtml(step.outcome)}</div>
        <div style="margin-top:4px">
          Access depth: ${depthBar(step.depth, p.maxDepth)}
          &nbsp;<span style="font-size:11px;color:#64748b">Unlocks: ${escapeHtml(step.unlocks)}</span>
        </div>
      </div>
    </div>
    `).join('')}
    <div style="font-size:12px;color:#64748b;margin-top:8px">
      Verified steps: ${p.verifiedSteps}/${p.steps.length}
    </div>
  </div>
  `).join('')}
</div>

<!-- Recommendations -->
<div class="section">
  <h2>Remediation Recommendations</h2>
  <table>
    <thead><tr><th>Priority</th><th>Control</th><th>Detail</th></tr></thead>
    <tbody>
      <tr><td>${severityBadge('HIGH')}</td><td>Bind Ollama to loopback only</td><td>Set <code>OLLAMA_HOST=127.0.0.1</code> in the Ollama service environment to prevent network access.</td></tr>
      <tr><td>${severityBadge('HIGH')}</td><td>Add authentication proxy</td><td>If remote access is required, place Ollama behind a reverse proxy (nginx/Caddy) with mutual TLS or API-key auth.</td></tr>
      <tr><td>${severityBadge('HIGH')}</td><td>Sanitise note content before LLM injection</td><td>In <code>useChat.js:37</code>, wrap note context in delimiters and strip/encode prompt-injection patterns before building the system prompt.</td></tr>
      <tr><td>${severityBadge('MED')}</td><td>Restrict CORS on Ollama</td><td>Set <code>OLLAMA_ORIGINS</code> to the specific app origin rather than the default wildcard.</td></tr>
      <tr><td>${severityBadge('MED')}</td><td>Encrypt localStorage notes</td><td>Use <code>window.crypto.subtle</code> to encrypt note content before writing to <code>localStorage</code>.</td></tr>
      <tr><td>${severityBadge('MED')}</td><td>Add Content-Security-Policy</td><td>Configure Vite to serve a strict CSP that limits <code>connect-src</code> to <code>localhost:11434</code> only.</td></tr>
      <tr><td>${severityBadge('LOW')}</td><td>Add security headers</td><td>Add <code>X-Content-Type-Options</code>, <code>X-Frame-Options</code>, and <code>Referrer-Policy</code> via Vite plugin or proxy.</td></tr>
    </tbody>
  </table>
</div>

<!-- Raw Evidence Appendix -->
<div class="section">
  <h2>Evidence Appendix — Raw JSON Dumps</h2>
  <p style="font-size:13px;color:#64748b;margin-bottom:12px">
    All evidence files are saved to the output directory for chain-of-custody purposes.
  </p>
  <div class="code">Evidence files:
  ${escapeHtml(outDir)}/url-enum.json       — all URL probe results
  ${escapeHtml(outDir)}/endpoint-map.json   — full endpoint classification + HTTP responses
  ${escapeHtml(outDir)}/attack-paths.json   — attack path verification data
  ${escapeHtml(outDir)}/report.html         — this report</div>
</div>

<footer>
  Security Assessment — NotesAI &nbsp;|&nbsp; ${escapeHtml(ts)}<br>
  CONFIDENTIAL — For authorised use only. Do not distribute.
</footer>
</div>
</body>
</html>`;

  await mkdir(outDir, { recursive: true });
  const reportPath = join(outDir, 'report.html');
  await writeFile(reportPath, html);
  console.log(`\n  \x1b[32m[SAVED]\x1b[0m HTML report → ${reportPath}`);
  return reportPath;
}
