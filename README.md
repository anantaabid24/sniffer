# Ananta Stream Catcher 2.0.0

Android Kotlin project with a dark network-inspector dashboard, installed-app selector, Android Sharesheet import, playlist URL extraction, local capture history, copy/open actions, and M3U export.

## Build
Upload the contents of this directory to the root of a GitHub repository. Open Actions and run **Build Ananta Stream Catcher APK**. Download the `Ananta-Stream-Catcher-debug` artifact.

## Capture from another app
Open an IPTV app and use its Share feature to share a text or URI to Ananta Stream Catcher. If the app exposes a playlist URL, the app extracts `.m3u8`, `.m3u`, `.mpd`, `format=m3u8`, or `type=hls` URLs. Apps that do not share URLs cannot be read by this app.

## Important limitation
The Target Apps selector only records a preferred app; it does not grant access to another app's private traffic. Ordinary Android apps cannot universally read other apps' HTTPS URLs. TLS encryption hides URL paths and query parameters, and DRM/certificate pinning may further restrict inspection. This project deliberately does not install a TLS interception certificate, decrypt HTTPS, bypass pinning, or defeat DRM. It does not claim universal HttpCanary-equivalent capture.

Minimum Android 8.0 / API 26. Java and Kotlin JVM target 17.
