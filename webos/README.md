# Ashika Live TV for LG webOS

This is the LG webOS TV implementation of Ashika TV, built separately from the Android Jetpack Compose app and adapted for TV remote navigation and the webOS media stack.

## Android feature mapping

The webOS app includes equivalents for the Android app's main destinations and workflows:

- Live channels, search, categories, favorites and recently watched
- Channel playback, previous/next channel, channel drawer, fullscreen, mini-player, subtitle/audio track selection and stream diagnostics
- Channel preview with EPG details, add/edit/delete channel
- M3U URL and local-file import, saved playlist refresh/removal, companion phone-to-TV sync
- XMLTV EPG URL/file import, refresh, clear and programme listing
- Smart channel finder, stream health, watch statistics, match center, family profiles and QR pairing
- PIN-protected admin tools, bulk playlist refresh, secure relay settings and local backup/restore
- Bengali/English display-language selection
- Toffee channel import helpers and live sports score links

The webOS build workflow checks the Android Compose screen source contracts and required webOS UI/function mappings, in addition to HTML/JavaScript syntax and IPK contents.

## Platform differences

Android ExoPlayer and Android System Picture-in-Picture cannot run on webOS. The TV build uses the browser/native media playback capabilities and an in-app mini-player equivalent. Stream playback still depends on codec, DRM, headers, CORS, provider availability and the LG TV's webOS version. DRM-protected Toffee streams are not bypassed. Android Google/Firebase account sign-in and cloud account sync are not claimed as webOS features; local profiles and export/import backup are available.

## Build

GitHub Actions installs the official `@webos-tools/cli`, validates the feature mapping, generates the launcher icon, packages the IPK and publishes the versioned release.

## Install

Download `com.ashika.livetv_9.4.0_all.ipk` from the [webOS v9.4.0 release](https://github.com/Ali-Haider1l/Ashika-tv/releases/tag/webos-v9.4.0). Install using LG webOS Developer Mode and `ares-install`.
