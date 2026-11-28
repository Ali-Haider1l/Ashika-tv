# Ashika Live TV for LG webOS

This is the webOS TV package source, separate from the Android Compose app.

## Build

The GitHub Actions workflow installs the official `@webos-tools/cli`, generates the launcher icon, and packages this folder as an IPK.

The app uses the standalone HTML player already present in the repository and includes the built-in Kolkata TV HLS channel. Playback of individual streams depends on the stream being online and compatible with the TV's webOS media stack.

## Install

Download `com.ashika.livetv_1.0.0_all.ipk` from the GitHub Actions artifact or the `webos-v1.0.0` GitHub Release. Install using LG webOS Developer Mode and `ares-install`.
