# Ashika TV Stream Relay

A small Vercel Function for authorized, non-DRM HTTP(S) streams. It forwards a limited set of request headers, supports byte-range media requests, and rewrites relative HLS playlist/segment URLs so LG webOS can load them through one origin.

## Required Vercel environment variables

Set these in **Project → Settings → Environment Variables**, for Production and Preview as needed:

- `RELAY_API_KEY`: a long random secret (at least 32 characters).
- `RELAY_ALLOWED_HOSTS`: comma-separated exact hostnames or parent domains you control/are authorized to proxy, for example `cdn.example.com,media.example.net`. Do not use `*`.

The relay rejects hosts not on the allowlist, private/reserved IP targets, unsafe redirects, unsupported methods, and common DRM-marked HLS playlists. Redirect destinations are revalidated. Only User-Agent, Referer, Origin, Cookie, Accept, and Accept-Language are forwarded.

## Deploy

Import this GitHub repository into Vercel and set **Root Directory** to `relay`. Add the two environment variables above, then deploy. The endpoint will be:

`https://YOUR-VERCEL-PROJECT.vercel.app/api/relay`

In Ashika TV → Playlist & Sync Settings, enter that endpoint and the same relay key, then save.

## Important limits

- This is not a public/open proxy: host allowlisting and a relay key are mandatory.
- Use only streams you own or are authorized to access. Never place personal account cookies in a shared/public installation.
- DRM, provider login flows, expiring signed playback sessions, and provider anti-bot controls are not bypassed.
- HLS media playlists are rewritten; DASH/DRM is intentionally not supported.
- Vercel Functions are not intended for unlimited high-bitrate streaming. Check current Vercel bandwidth, function duration, and plan limits before production use. For heavy traffic, use a dedicated streaming CDN/proxy service.
