#!/usr/bin/env python3
"""
termux-assess.py — URL Security Assessment Tool for Termux
===========================================================
Designed to run in Termux (Android) with zero external dependencies.
Uses only Python 3 standard library.

USAGE
-----
  # Single URL from browser:
  python3 security/termux-assess.py https://target.example.com

  # Interactive multi-URL session:
  python3 security/termux-assess.py

  # Pipe in a list of URLs:
  echo "https://example.com" | python3 security/termux-assess.py

AUTHORISED USE ONLY — only test systems you own or have written permission
to assess.

OUTPUT
------
  security/results/<sanitised-host>-<timestamp>/
    report.html   — self-contained HTML report with all evidence
    evidence.json — machine-readable findings for chain of custody
"""

import sys
import os
import json
import re
import time
import socket
from datetime import datetime
from urllib.request import urlopen, Request
from urllib.error import URLError, HTTPError
from urllib.parse import urlparse, urljoin
from pathlib import Path
from http.client import HTTPResponse

# ── ANSI colours (Termux supports these) ─────────────────────────────────────
R   = "\033[0m"
RED = "\033[31m"; YEL = "\033[33m"; GRN = "\033[32m"
CYN = "\033[36m"; GRY = "\033[90m"; BLD = "\033[1m"

def c(color, text): return f"{color}{text}{R}"
def head(t):        print(f"\n{BLD}{CYN}▶ {t}{R}")
def info(t):        print(f"  {CYN}[·]{R} {t}")
def ok(t):          print(f"  {GRN}[✓]{R} {t}")
def warn(t):        print(f"  {YEL}[!]{R} {t}")
def err(t):         print(f"  {RED}[✗]{R} {t}")

# ── Path wordlists ────────────────────────────────────────────────────────────
COMMON_PATHS = [
    # Root / standard
    "/", "/index.html", "/index.php", "/index.asp", "/index.aspx",
    "/robots.txt", "/sitemap.xml", "/favicon.ico", "/manifest.json",
    # Admin / control panels
    "/admin", "/admin/", "/admin/login", "/admin/login.php",
    "/administrator", "/administrator/", "/wp-admin/", "/wp-login.php",
    "/dashboard", "/panel", "/cpanel", "/manager",
    # Common APIs
    "/api", "/api/", "/api/v1", "/api/v1/", "/api/v2/", "/api/v3/",
    "/graphql", "/api/graphql", "/rest", "/rest/",
    # Auth endpoints
    "/login", "/logout", "/register", "/signup", "/signin",
    "/auth", "/auth/login", "/auth/token", "/oauth",
    "/api/auth", "/api/login", "/api/token",
    "/api/user", "/api/users", "/api/me", "/api/profile",
    # Config / secrets
    "/.env", "/.env.local", "/.env.production", "/.env.backup",
    "/config", "/config.json", "/config.js", "/config.php",
    "/settings.json", "/settings.php", "/configuration.php",
    "/wp-config.php", "/wp-config.php.bak",
    # Debug / health
    "/debug", "/status", "/health", "/healthz", "/health-check",
    "/ping", "/metrics", "/actuator", "/actuator/health",
    "/actuator/env", "/actuator/mappings", "/server-status",
    # Backup / logs
    "/backup", "/backup.zip", "/backup.tar.gz", "/backup.sql",
    "/.git", "/.git/HEAD", "/.git/config",
    "/logs", "/log", "/error.log", "/access.log",
    # Common CMS / frameworks
    "/phpmyadmin", "/phpmyadmin/", "/pma/",
    "/wordpress", "/drupal", "/joomla",
    "/.htaccess", "/.htpasswd",
    "/web.config", "/server.xml",
    # APIs common endpoints
    "/api/docs", "/swagger", "/swagger-ui.html", "/openapi.json",
    "/api-docs", "/swagger/index.html", "/redoc",
]

# Security headers to check
SEC_HEADERS = {
    "content-security-policy":         ("HIGH",  "Prevents XSS and data injection attacks"),
    "x-content-type-options":           ("MED",   "Prevents MIME-type sniffing"),
    "x-frame-options":                  ("MED",   "Prevents clickjacking"),
    "strict-transport-security":        ("HIGH",  "Enforces HTTPS connections"),
    "permissions-policy":               ("LOW",   "Controls browser feature access"),
    "referrer-policy":                  ("LOW",   "Controls referrer information leakage"),
    "x-xss-protection":                 ("LOW",   "Legacy XSS filter (informational)"),
    "access-control-allow-origin":      ("INFO",  "CORS policy — check for wildcard (*)"),
    "server":                           ("INFO",  "Server banner — may leak stack info"),
    "x-powered-by":                     ("INFO",  "Technology disclosure header"),
}

TIMEOUT = 8   # seconds per request

# ── HTTP probe ────────────────────────────────────────────────────────────────
def probe(url: str, method: str = "GET", data: bytes = None,
          extra_headers: dict = None) -> dict:
    headers = {
        "User-Agent":       "Mozilla/5.0 (Security-Assessment/1.0)",
        "Accept":           "*/*",
        "Connection":       "close",
    }
    if extra_headers:
        headers.update(extra_headers)

    start = time.time()
    result = {
        "url": url, "method": method,
        "status": None, "headers": {}, "body_snippet": "",
        "duration_ms": 0, "error": None, "timestamp": datetime.utcnow().isoformat(),
    }
    try:
        req = Request(url, data=data, headers=headers, method=method)
        with urlopen(req, timeout=TIMEOUT) as resp:
            result["status"]  = resp.status
            result["headers"] = dict(resp.headers)
            try:
                body = resp.read(512)
                result["body_snippet"] = body.decode("utf-8", errors="replace")
            except Exception:
                pass
    except HTTPError as e:
        result["status"]  = e.code
        result["headers"] = dict(e.headers) if e.headers else {}
        try:
            result["body_snippet"] = e.read(256).decode("utf-8", errors="replace")
        except Exception:
            pass
    except URLError as e:
        result["error"] = str(e.reason)
    except socket.timeout:
        result["error"] = "timeout"
    except Exception as e:
        result["error"] = str(e)
    result["duration_ms"] = int((time.time() - start) * 1000)
    return result

# ── 1. URL Path Enumeration ───────────────────────────────────────────────────
def enumerate_paths(base_url: str) -> list:
    head("URL Path Enumeration")
    info(f"Target: {base_url}")
    results = []
    for path in COMMON_PATHS:
        url = base_url.rstrip("/") + path
        r   = probe(url)
        r["path"] = path
        results.append(r)
        status = r["status"]
        if status is None:
            print(f"  {GRY}TIMEOUT   {R}  {path}")
        elif status == 404:
            print(f"  {GRY}404       {R}  {path}", end="\r")
            sys.stdout.flush()
        elif status < 300:
            print(f"  {GRN}{status:<9}{R}  {path}")
        elif status in (301, 302, 307, 308):
            loc = r["headers"].get("Location", "")
            print(f"  {CYN}{status:<9}{R}  {path}  → {loc}")
        elif status in (401, 403):
            print(f"  {YEL}{status:<9}{R}  {path}  (auth-gated — interesting)")
        elif status >= 500:
            print(f"  {RED}{status:<9}{R}  {path}  (server error — may leak info)")
        else:
            print(f"  {GRY}{status:<9}{R}  {path}")
    print()   # clear \r line
    interesting = [r for r in results if r["status"] and r["status"] != 404]
    info(f"Interesting paths: {len(interesting)}")
    return results

# ── 2. Security Header Audit ──────────────────────────────────────────────────
def audit_headers(base_url: str) -> list:
    head("Security Header Audit")
    r = probe(base_url)
    if r["status"] is None:
        warn(f"Could not reach {base_url}: {r['error']}")
        return []

    info(f"HTTP {r['status']}  ({r['duration_ms']}ms)")
    findings = []

    for header, (severity, purpose) in SEC_HEADERS.items():
        # headers dict keys from http.client are title-cased
        val = r["headers"].get(header) or r["headers"].get(header.title())
        if header in ("server", "x-powered-by"):
            if val:
                warn(f"{header}: {val}  ← information disclosure")
                findings.append({"header": header, "value": val, "severity": "INFO",
                                  "issue": "Technology/version disclosure", "purpose": purpose})
            continue
        if header == "access-control-allow-origin":
            if val == "*":
                err(f"CORS wildcard — Access-Control-Allow-Origin: *  ← HIGH risk")
                findings.append({"header": header, "value": val, "severity": "HIGH",
                                  "issue": "CORS wildcard allows any origin", "purpose": purpose})
            elif val:
                ok(f"{header}: {val}")
            continue
        if not val:
            color = RED if severity == "HIGH" else YEL if severity == "MED" else GRY
            print(f"  {color}[{severity}]{R}  MISSING  {header}  — {purpose}")
            findings.append({"header": header, "value": None, "severity": severity,
                              "issue": f"Header not present", "purpose": purpose})
        else:
            ok(f"{header}: {val}")

    return findings

# ── 3. CORS Probe ─────────────────────────────────────────────────────────────
def probe_cors(base_url: str) -> list:
    head("CORS Policy Probe")
    test_origins = [
        "https://attacker.example.com",
        "http://evil.local",
        "null",
    ]
    findings = []
    for origin in test_origins:
        r = probe(base_url, extra_headers={
            "Origin": origin,
            "Access-Control-Request-Method": "POST",
        })
        acao = (r["headers"].get("access-control-allow-origin") or
                r["headers"].get("Access-Control-Allow-Origin") or "(not set)")
        acac = (r["headers"].get("access-control-allow-credentials") or
                r["headers"].get("Access-Control-Allow-Credentials") or "(not set)")
        info(f"Origin: {origin:<40}  ACAO: {acao}  ACAC: {acac}")
        if acao == "*":
            findings.append({"origin": origin, "acao": acao, "acac": acac,
                              "severity": "HIGH", "issue": "Wildcard CORS — any origin accepted"})
        elif acao == origin or acao == "null":
            findings.append({"origin": origin, "acao": acao, "acac": acac,
                              "severity": "MED", "issue": "Origin reflected — attacker origin accepted"})
    return findings

# ── 4. Technology Fingerprint ─────────────────────────────────────────────────
def fingerprint(base_url: str) -> dict:
    head("Technology Fingerprint")
    r = probe(base_url)
    if r["status"] is None:
        warn("Unreachable"); return {}

    tech = {}
    h = {k.lower(): v for k, v in r["headers"].items()}
    body = r["body_snippet"].lower()

    # Server / framework
    if h.get("server"):          tech["server"]          = h["server"]
    if h.get("x-powered-by"):    tech["x-powered-by"]    = h["x-powered-by"]
    if h.get("x-generator"):     tech["x-generator"]     = h["x-generator"]
    if h.get("x-drupal-cache"):  tech["cms"]             = "Drupal"
    if h.get("x-wp-total"):      tech["cms"]             = "WordPress"

    # Body heuristics
    sigs = {
        "WordPress":  ["wp-content", "wp-includes", "xmlrpc"],
        "Drupal":     ["drupal", "sites/all", "sites/default"],
        "Joomla":     ["joomla", "/components/", "/modules/"],
        "React":      ["__react", "react-root", "_reactfiber"],
        "Vue":        ["__vue", "data-v-"],
        "Angular":    ["ng-version", "_nghost", "ng-app"],
        "Laravel":    ["laravel", "laravel_session"],
        "Django":     ["csrfmiddlewaretoken", "django"],
        "Rails":      ["_rails_", "x-rails-version"],
        "Express":    ["express", "x-express"],
        "Next.js":    ["__next", "_next/static"],
    }
    for name, patterns in sigs.items():
        if any(p in body for p in patterns):
            tech.setdefault("frameworks", []).append(name)

    for k, v in tech.items():
        info(f"{k}: {v}")

    if not tech:
        info("No obvious technology fingerprints detected")

    return tech

# ── Report generation ─────────────────────────────────────────────────────────
def save_report(out_dir: Path, target_url: str, path_results: list,
                header_findings: list, cors_findings: list, tech: dict):
    evidence = {
        "tool":     "termux-assess.py",
        "target":   target_url,
        "timestamp": datetime.utcnow().isoformat(),
        "url_enum": path_results,
        "header_findings": header_findings,
        "cors_findings":   cors_findings,
        "technology":      tech,
    }
    out_dir.mkdir(parents=True, exist_ok=True)
    (out_dir / "evidence.json").write_text(
        json.dumps(evidence, indent=2), encoding="utf-8"
    )

    interesting  = [r for r in path_results if r["status"] and r["status"] != 404]
    open_paths   = [r for r in interesting if r["status"] and r["status"] < 300]
    auth_gated   = [r for r in interesting if r["status"] in (401, 403)]
    high_headers = [f for f in header_findings if f["severity"] == "HIGH"]
    high_cors    = [f for f in cors_findings   if f["severity"] == "HIGH"]

    def badge(sev):
        colors = {"HIGH":"#dc2626","MED":"#d97706","LOW":"#2563eb","INFO":"#6b7280"}
        c = colors.get(sev, "#6b7280")
        return f'<span style="background:{c};color:#fff;padding:2px 7px;border-radius:4px;font-size:11px;font-weight:700">{sev}</span>'

    def esc(s):
        return str(s).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")

    def status_color(s):
        if s is None:           return "#9ca3af"
        if s < 300:             return "#16a34a"
        if s < 400:             return "#2563eb"
        if s in (401,403):      return "#d97706"
        if s >= 500:            return "#dc2626"
        return "#6b7280"

    rows_interesting = "".join(
        f'<tr><td><code>{esc(r["method"])}</code></td>'
        f'<td style="font-family:monospace;font-size:12px">{esc(r["path"])}</td>'
        f'<td style="color:{status_color(r["status"])};font-weight:700">{r["status"]}</td>'
        f'<td style="font-size:11px;color:#64748b;max-width:300px;word-break:break-all">'
        f'{esc(r["body_snippet"][:120])}</td></tr>'
        for r in interesting
    ) or "<tr><td colspan=4 style='color:#9ca3af;text-align:center'>None found</td></tr>"

    rows_headers = "".join(
        f'<tr><td>{badge(f["severity"])}</td>'
        f'<td style="font-family:monospace;font-size:12px">{esc(f["header"])}</td>'
        f'<td style="font-size:12px">{esc(f["value"] or "— MISSING —")}</td>'
        f'<td style="font-size:12px;color:#64748b">{esc(f["issue"])}</td></tr>'
        for f in header_findings
    ) or "<tr><td colspan=4 style='color:#16a34a;text-align:center'>No issues found</td></tr>"

    rows_cors = "".join(
        f'<tr><td>{badge(f["severity"])}</td>'
        f'<td style="font-family:monospace;font-size:12px">{esc(f["origin"])}</td>'
        f'<td style="font-family:monospace;font-size:12px">{esc(f["acao"])}</td>'
        f'<td style="font-size:12px;color:#64748b">{esc(f["issue"])}</td></tr>'
        for f in cors_findings
    ) or "<tr><td colspan=4 style='color:#16a34a;text-align:center'>No CORS issues found</td></tr>"

    tech_rows = "".join(
        f'<tr><td style="font-size:12px;font-weight:600">{esc(k)}</td>'
        f'<td style="font-family:monospace;font-size:12px">{esc(v)}</td></tr>'
        for k, v in tech.items()
    ) or "<tr><td colspan=2 style='color:#9ca3af'>None detected</td></tr>"

    html = f"""<!DOCTYPE html>
<html lang="en"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Security Assessment — {esc(target_url)}</title>
<style>
*{{box-sizing:border-box;margin:0;padding:0}}
body{{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;background:#f8fafc;color:#1e293b;line-height:1.6;font-size:14px}}
.page{{max-width:1100px;margin:0 auto;padding:32px 20px}}
header{{background:#0f172a;color:#fff;padding:40px;border-radius:10px;margin-bottom:24px}}
header h1{{font-size:24px;margin-bottom:6px}}
header .sub{{color:#94a3b8;font-size:13px}}
.section{{background:#fff;border:1px solid #e2e8f0;border-radius:8px;padding:24px;margin-bottom:20px}}
.section h2{{font-size:16px;font-weight:700;margin-bottom:14px;padding-bottom:8px;border-bottom:2px solid #f1f5f9}}
.stats{{display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin-bottom:20px}}
.stat{{background:#f1f5f9;border-radius:8px;padding:16px;text-align:center}}
.stat .n{{font-size:32px;font-weight:800}}
.stat .l{{font-size:12px;color:#64748b;margin-top:2px}}
.stat.h .n{{color:#dc2626}}.stat.m .n{{color:#d97706}}.stat.g .n{{color:#16a34a}}
table{{width:100%;border-collapse:collapse;font-size:13px}}
th{{background:#f1f5f9;text-align:left;padding:8px 10px;font-size:11px;text-transform:uppercase;letter-spacing:.05em;color:#64748b}}
td{{padding:8px 10px;border-bottom:1px solid #f1f5f9;vertical-align:top}}
tr:hover td{{background:#f8fafc}}
.code{{font-family:'SF Mono',Consolas,monospace;background:#0f172a;color:#e2e8f0;padding:10px 14px;border-radius:6px;font-size:11px;overflow-x:auto;white-space:pre-wrap;word-break:break-all;margin-top:8px}}
footer{{text-align:center;color:#94a3b8;font-size:11px;margin-top:32px;padding:16px 0}}
</style></head><body><div class="page">
<header>
  <h1>Security Assessment Report</h1>
  <div class="sub">
    Target: <strong>{esc(target_url)}</strong><br>
    Generated: {esc(datetime.utcnow().isoformat())} UTC<br>
    Tool: termux-assess.py &nbsp;|&nbsp; <strong>CONFIDENTIAL — AUTHORISED USE ONLY</strong>
  </div>
</header>
<div class="stats">
  <div class="stat g"><div class="n">{len(open_paths)}</div><div class="l">Open Paths</div></div>
  <div class="stat m"><div class="n">{len(auth_gated)}</div><div class="l">Auth-Gated</div></div>
  <div class="stat h"><div class="n">{len(high_headers)+len(high_cors)}</div><div class="l">HIGH Findings</div></div>
  <div class="stat"><div class="n">{len(header_findings)+len(cors_findings)}</div><div class="l">Total Findings</div></div>
</div>
<div class="section">
  <h2>Technology Fingerprint</h2>
  <table><thead><tr><th>Signal</th><th>Value</th></tr></thead><tbody>{tech_rows}</tbody></table>
</div>
<div class="section">
  <h2>Accessible URL Paths (non-404)</h2>
  <table><thead><tr><th>Method</th><th>Path</th><th>Status</th><th>Response Snippet</th></tr></thead>
  <tbody>{rows_interesting}</tbody></table>
</div>
<div class="section">
  <h2>Security Header Audit</h2>
  <table><thead><tr><th>Severity</th><th>Header</th><th>Value</th><th>Issue</th></tr></thead>
  <tbody>{rows_headers}</tbody></table>
</div>
<div class="section">
  <h2>CORS Policy Probe</h2>
  <table><thead><tr><th>Severity</th><th>Test Origin</th><th>ACAO Response</th><th>Issue</th></tr></thead>
  <tbody>{rows_cors}</tbody></table>
</div>
<div class="section">
  <h2>Evidence Files</h2>
  <div class="code">evidence.json  — full HTTP responses, all probe data (chain of custody)
report.html    — this report
Directory: {esc(str(out_dir))}</div>
</div>
<footer>termux-assess.py &nbsp;|&nbsp; {esc(target_url)} &nbsp;|&nbsp; {esc(datetime.utcnow().isoformat())} UTC</footer>
</div></body></html>"""

    report_path = out_dir / "report.html"
    report_path.write_text(html, encoding="utf-8")
    return report_path

# ── Main ──────────────────────────────────────────────────────────────────────
def run_assessment(target_url: str):
    parsed = urlparse(target_url)
    if not parsed.scheme:
        target_url = "https://" + target_url
        parsed     = urlparse(target_url)
    base = f"{parsed.scheme}://{parsed.netloc}"

    safe_host = re.sub(r"[^\w.-]", "_", parsed.netloc)
    ts        = datetime.utcnow().strftime("%Y%m%d-%H%M%S")
    out_dir   = Path(__file__).parent / "results" / f"{safe_host}-{ts}"

    print(f"\n{BLD}{'─'*60}{R}")
    print(f"{BLD}Target: {CYN}{target_url}{R}")
    print(f"Output: {out_dir}")
    print(f"{'─'*60}{R}")

    path_results   = enumerate_paths(base)
    header_findings = audit_headers(base)
    cors_findings   = probe_cors(base)
    tech           = fingerprint(base)
    report_path    = save_report(out_dir, target_url, path_results,
                                 header_findings, cors_findings, tech)

    # Summary
    interesting = [r for r in path_results if r["status"] and r["status"] != 404]
    high_count  = sum(1 for f in (header_findings + cors_findings) if f.get("severity") == "HIGH")

    head("Assessment Complete")
    print(f"  Accessible paths  : {c(GRN, len([r for r in interesting if r['status'] and r['status']<300]))}")
    print(f"  Auth-gated paths  : {c(YEL, len([r for r in interesting if r['status'] in (401,403)]))}")
    print(f"  HIGH findings     : {c(RED, high_count)}")
    print(f"\n  {c(GRN,'HTML report')} : {report_path}")
    print(f"  {c(GRN,'Evidence JSON')}: {out_dir / 'evidence.json'}")
    print(f"{'─'*60}\n")

def get_urls() -> list:
    """Collect one or more URLs: from args, stdin pipe, or interactive prompt."""
    urls = []

    # 1. CLI arguments
    cli = [a for a in sys.argv[1:] if not a.startswith("-")]
    if cli:
        return cli

    # 2. Piped stdin
    if not sys.stdin.isatty():
        for line in sys.stdin:
            line = line.strip()
            if line:
                urls.append(line)
        return urls

    # 3. Interactive session
    print(f"\n{BLD}{CYN}termux-assess.py — URL Security Assessment{R}")
    print("Paste a URL from your browser (or multiple URLs, one per line).")
    print("Type 'done' or press Ctrl-C to finish.\n")
    while True:
        try:
            raw = input(f"{BLD}URL>{R} ").strip()
        except (EOFError, KeyboardInterrupt):
            print()
            break
        if raw.lower() in ("done", "exit", "quit", ""):
            break
        if raw:
            urls.append(raw)
    return urls

if __name__ == "__main__":
    print(f"\n{BLD}{CYN}╔{'═'*56}╗")
    print(f"║  termux-assess.py — URL Security Assessment Tool     ║")
    print(f"║  AUTHORISED USE ONLY — CONFIDENTIAL                  ║")
    print(f"╚{'═'*56}╝{R}")

    urls = get_urls()
    if not urls:
        print("No URLs provided. Exiting.")
        sys.exit(0)

    for url in urls:
        try:
            run_assessment(url)
        except KeyboardInterrupt:
            print("\nInterrupted.")
            break
        except Exception as e:
            err(f"Assessment failed for {url}: {e}")
