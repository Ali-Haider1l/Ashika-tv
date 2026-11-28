# Ashika TV webOS v9.1.0

## Added in this update
- XMLTV EPG URL loading and local XML import, with current/upcoming programme list and channel matching by `tvg-id` or display name.
- Dedicated cricket and football scoreboard shortcuts.
- Audio and subtitle track selectors when the LG webOS media pipeline exposes those tracks.
- Native webOS HLS playback is preferred; hls.js is a fallback with fatal network/media recovery and visible error reporting.
- Third-party default playlist is no longer fetched automatically at startup. It remains saved for manual loading; this avoids a startup failure when a playlist host blocks cross-origin requests.
- Existing channel browsing, categories, search, favorites, recently watched, profiles, admin PIN, manual channel management, M3U file import, LAN companion sync, pairing display, watch statistics and stream diagnostics are retained.

## Platform parity notes
This is a webOS implementation, not an Android APK copied into an IPK. Android-only capabilities that depend on Android framework APIs (system Picture-in-Picture, Android account/Firebase authentication, Room/Datastore internals, and ExoPlayer-only telemetry) require separate webOS equivalents and are not represented as completed by this release. Audio/subtitle track controls appear only when the stream/browser exposes track metadata. XMLTV URL loading depends on the EPG host allowing TV-browser CORS; local XML import is available as an alternative.

## Test on the target LG TV
1. Install the IPK and open Ashika TV.
2. Load a known-working M3U playlist manually from Settings / Playlist.
3. Test the supplied Kolkata TV HLS stream and at least one HLS stream from the user's own playlist.
4. Open Stream Health and confirm playback state / resolution.
5. Load a small XMLTV XML file and verify programme rows.
6. Test D-pad focus, Back key, fullscreen, channel switching, subtitles and audio tracks where supported.

A successful IPK packaging job does not prove every third-party stream will play. Actual stream availability, codec support, CORS and provider restrictions must be tested on the TV.
