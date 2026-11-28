# Ashika TV webOS v9.2.0 — Android feature parity work

This release adds webOS-native equivalents for features present in the Android app. It does not copy Android framework code into webOS.

## Feature mapping

| Android feature | webOS implementation in v9.2.0 | Status / caveat |
|---|---|---|
| ExoPlayer live playback | LG native HLS first; HLS.js fallback | Codec, DRM, provider and CORS restrictions remain |
| Per-stream HTTP headers | M3U #EXTHTTP, #EXTVLCOPT, Kodi stream headers, URL pipe headers; HLS.js XHR headers | Native LG video cannot attach arbitrary headers; header-required HLS needs HLS.js/MSE |
| M3U / M3U8 URL + local file | URL and local M3U/M3U8 import | Browser CORS may block remote playlist fetch |
| Toffee JSON playlists | JSON channel array import via playlist loader | Provider-specific JSON schemas may differ |
| Toffee site extractor | Toffee page/JSON extraction from pasted public HTML, plus direct public-page fetch attempt | Direct fetch may be blocked by webOS CORS; DRM/login-protected streams are not extracted; expiring URLs can stop working |
| XMLTV EPG URL + local import | XMLTV URL, local XML import, .gz decompression when browser supports it, current/upcoming programmes and channel matching | URL access depends on CORS; .gz needs DecompressionStream support |
| In-app floating PiP | Mini-player overlay with expand/back controls | webOS browser does not expose Android system PiP parity |
| Audio/subtitle selection | Native tracks and HLS.js audio/subtitle renditions | Only tracks exposed by the stream/player can be selected |
| Sports quick access | Cricket and football scoreboard links; sports-channel finder | Scores open provider webpages rather than a native live-score API |
| Smart Channel Finder | Search loaded channels by name/category/favorites/recent | Local playlist data only |
| Stream health | Browser network/ready state, resolution and playback error code | No ExoPlayer codec/bitrate/dropped-frame telemetry |
| Watch statistics | Local per-channel watch time | Stored in TV browser local storage |
| Live Match Center | Add, view and delete manually entered scorecards | Manual scores, not a live sports data feed |
| Family Profiles | Local profile switching and profile-specific favorites/recent | Stored locally on the TV |
| QR pairing + LAN companion | Pairing display and companion JSON endpoint import | Pairing code is not a cloud account or automatic two-way sync |
| Cloud sync / Firebase | Portable JSON export/import + phone-to-TV LAN sync | Firebase/cloud account sync intentionally not reintroduced after the earlier Firebase-removal request |
| Admin channel management | Existing PIN-protected admin panel and channel/playlist management | PIN and data are local to this TV browser |

## Test checklist

1. Load an M3U URL and a local M3U file.
2. Test a playlist containing #EXTHTTP, #EXTVLCOPT, #KODIPROP, tvg-id, tvg-logo, and url-tvg.
3. Import a Toffee-compatible JSON channel array.
4. Test a public Toffee page HTML paste; confirm that protected/DRM content is not reported as extracted.
5. Load/import an XMLTV file, including a .gz URL only if the target webOS runtime supports decompression.
6. Test a normal HLS stream and an HLS stream requiring Referer/Cookie headers.
7. Test Mini/PiP, fullscreen, channel switching, subtitle/audio tracks, D-pad and Back.
8. Export a backup, then import it and verify profiles, favorites, playlists and channels.
9. Test the companion endpoint from the same LAN.

A successful package build validates metadata, source parity, inline JavaScript syntax and IPK contents. It does not prove every stream works. Final playback validation must happen on the target LG TV (32LM636BPTB / webOS 4.10.0).
