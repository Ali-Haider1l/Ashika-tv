const { lookup } = require("node:dns/promises");
const { isIP } = require("node:net");

const ALLOWED_HEADERS = new Set(["user-agent", "referer", "origin", "cookie", "accept", "accept-language"]);
const MAX_REDIRECTS = 4;
const MAX_MANIFEST_BYTES = 2 * 1024 * 1024;

function json(res, status, body) {
  res.statusCode = status;
  res.setHeader("Content-Type", "application/json; charset=utf-8");
  res.setHeader("Cache-Control", "no-store");
  res.end(JSON.stringify(body));
}
function isPublicAddress(address) {
  const a = String(address).toLowerCase().split("%")[0];
  if (a === "::1" || a === "::" || a.startsWith("fc") || a.startsWith("fd") || a.startsWith("fe80:") || a.startsWith("ff")) return false;
  if (a.startsWith("::ffff:")) return isPublicAddress(a.slice(7));
  if (a.includes(":")) return true;
  const p = a.split(".").map(Number);
  if (p.length !== 4 || p.some(n => !Number.isInteger(n) || n < 0 || n > 255)) return false;
  const [x,y] = p;
  return !(x === 0 || x === 10 || x === 127 || x >= 224 ||
    (x === 100 && y >= 64 && y <= 127) || (x === 169 && y === 254) ||
    (x === 172 && y >= 16 && y <= 31) || (x === 192 && y === 168) ||
    (x === 192 && y === 0) || (x === 198 && (y === 18 || y === 19)));
}
async function validateTarget(raw) {
  let u;
  try { u = new URL(raw); } catch { throw new Error("Invalid stream URL"); }
  if (u.protocol !== "https:" && u.protocol !== "http:") throw new Error("Only HTTP/HTTPS stream URLs are supported");
  if (u.username || u.password || !u.hostname) throw new Error("URL credentials are not allowed");
  const allowed = (process.env.RELAY_ALLOWED_HOSTS || "").split(",").map(x => x.trim().toLowerCase()).filter(Boolean);
  const host = u.hostname.toLowerCase();
  if (!allowed.length || !allowed.some(d => host === d || host.endsWith("." + d))) throw new Error("Stream host is not on RELAY_ALLOWED_HOSTS");
  const ip = isIP(host);
  const addresses = ip ? [{ address: host }] : await lookup(host, { all: true, verbatim: true });
  if (!addresses.length || addresses.some(x => !isPublicAddress(x.address))) throw new Error("Private or reserved network targets are blocked");
  return u;
}
function decodeHeaders(raw) {
  if (!raw) return {};
  let parsed;
  try { parsed = JSON.parse(Buffer.from(raw, "base64url").toString("utf8")); } catch { throw new Error("Invalid header payload"); }
  if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) throw new Error("Invalid header payload");
  const out = {};
  for (const [k,v] of Object.entries(parsed)) {
    if (ALLOWED_HEADERS.has(k.toLowerCase()) && typeof v === "string" && v.length <= 4096 && !/[\r\n]/.test(v)) out[k] = v;
  }
  return out;
}
function relayUrl(target, key, headers) {
  const q = new URLSearchParams({ url: target.href });
  if (key) q.set("key", key);
  if (Object.keys(headers).length) q.set("h", Buffer.from(JSON.stringify(headers)).toString("base64url"));
  return "/api/relay?" + q.toString();
}
function rewriteManifest(text, base, key, headers) {
  return text.split(/\r?\n/).map(line => {
    const trimmed = line.trim();
    if (!trimmed) return line;
    if (trimmed.startsWith("#")) {
      return line.replace(/URI="([^"]+)"/g, (_m, value) => {
        try { return 'URI="' + relayUrl(new URL(value, base), key, headers) + '"'; } catch { return 'URI="' + value + '"'; }
      });
    }
    try { return relayUrl(new URL(trimmed, base), key, headers); } catch { return line; }
  }).join("\n");
}
module.exports = async function handler(req, res) {
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Range, Content-Type");
  res.setHeader("Vary", "Origin");
  if (req.method === "OPTIONS") { res.statusCode = 204; return res.end(); }
  if (req.method !== "GET" && req.method !== "HEAD") return json(res, 405, { error: "Method not allowed" });
  const expected = process.env.RELAY_API_KEY || "";
  const supplied = String(req.query.key || "");
  if (!expected || supplied.length !== expected.length || supplied !== expected) return json(res, 401, { error: "Relay key missing or invalid" });
  if (req.query.health === "1") return json(res, 200, { ok: true, service: "Ashika TV Relay", version: "1.0.0" });
  try {
    let target = await validateTarget(String(req.query.url || ""));
    const headers = decodeHeaders(String(req.query.h || ""));
    if (req.headers.range) headers.Range = String(req.headers.range);
    let upstream;
    for (let i = 0; i <= MAX_REDIRECTS; i++) {
      upstream = await fetch(target, { method: req.method, headers, redirect: "manual", signal: AbortSignal.timeout(25000) });
      if (![301,302,303,307,308].includes(upstream.status)) break;
      const location = upstream.headers.get("location");
      if (!location || i === MAX_REDIRECTS) throw new Error("Too many or invalid upstream redirects");
      target = await validateTarget(new URL(location, target).href);
    }
    if (!upstream) throw new Error("Upstream request failed");
    const type = (upstream.headers.get("content-type") || "").toLowerCase();
    const looksManifest = /\.m3u8(?:$|\?)/i.test(target.pathname) || type.includes("mpegurl");
    res.statusCode = upstream.status;
    for (const name of ["content-type","content-length","content-range","accept-ranges","last-modified","etag"]) {
      const value = upstream.headers.get(name);
      if (value && name !== "content-length" || value && !looksManifest) res.setHeader(name, value);
    }
    res.setHeader("Cache-Control", "no-store");
    if (looksManifest && upstream.ok && req.method !== "HEAD") {
      const bytes = await upstream.arrayBuffer();
      if (bytes.byteLength > MAX_MANIFEST_BYTES) return json(res, 413, { error: "Manifest too large" });
      const body = Buffer.from(bytes).toString("utf8");
      if (/KEYFORMAT="(?:com\.apple\.streamingkeydelivery|urn:uuid:)/i.test(body) || /SAMPLE-AES/i.test(body)) {
        return json(res, 403, { error: "DRM-protected HLS is not supported by this relay" });
      }
      const rewritten = rewriteManifest(body, target, expected, headers);
      res.setHeader("Content-Type", "application/vnd.apple.mpegurl; charset=utf-8");
      res.setHeader("Content-Length", Buffer.byteLength(rewritten));
      return res.end(rewritten);
    }
    if (req.method === "HEAD" || !upstream.body) return res.end();
    const { Readable } = require("node:stream");
    Readable.fromWeb(upstream.body).pipe(res);
  } catch (error) {
    return json(res, 400, { error: error.message || "Relay request failed" });
  }
};
