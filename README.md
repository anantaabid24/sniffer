# Ananta Stream Catcher

Android app starter (Kotlin) for detecting HLS playlist URLs in its built-in browser and importing URLs explicitly shared from other apps.

## What this build does

- Watches requests made by the app's own WebView and records URLs whose paths/queries indicate `.m3u8` or `.m3u` playlists.
- Accepts `ACTION_SEND` text links and `ACTION_VIEW` links from other apps. Use Share > Ananta Stream Catcher in the source app when it exposes a URL.
- Accepts a URL pasted from the clipboard.
- Keeps a local capture history in SharedPreferences.
- Deduplicates captures; copy, remove, clear, and export as an M3U text playlist.
- Tests a captured HLS URL with AndroidX Media3 ExoPlayer.
- Uses a premium dark glass-style interface with cyan/purple gradient accents and capture cards.

## Important Android limitation

This is **not** a universal cross-app HTTPS sniffer. Android app sandboxing and TLS encryption prevent an ordinary app from reading other apps' private HTTPS requests. Share-in works only when the source app exposes a URL through Android's share/view interface or clipboard. The browser detector only sees requests from this app's WebView. This project intentionally does not install a TLS interception certificate, decrypt HTTPS, bypass certificate pinning, defeat DRM, or intercept protected streams.

A `VpnService` without a complete packet-forwarding tunnel would break device connectivity. This build therefore does not start a dummy VPN or claim that it can extract encrypted URLs through VPN monitoring.

## Build APK with GitHub Actions (no Android Studio)

1. Create a GitHub repository, for example `Ananta-Stream-Catcher`.
2. Upload/push the **contents** of this project folder to the repository root (the `.github` folder must be at the root).
3. Open **Actions**. The workflow runs on pushes to `main`, or choose **Build Ananta Stream Catcher APK** > **Run workflow**.
4. Open the completed workflow run and download the `Ananta-Stream-Catcher-v1.2.0-debug` artifact.
5. Extract the artifact ZIP and install `app-debug.apk` on your Android device. You may need to allow installation from that source.

The workflow uses JDK 17, Gradle 8.9, Android Gradle Plugin 8.7.3, and Kotlin 2.0.21. No Gradle wrapper is included; GitHub Actions installs Gradle in the runner.

## Local build (optional)

With JDK 17, Android SDK platform 35/build-tools installed, and Gradle 8.9 available:

```bash
gradle assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Use

### Browser capture
1. Open Ananta Stream Catcher.
2. Enter a website address and tap **OPEN**.
3. When the page requests a recognizable M3U8/M3U URL, it appears in the capture list.
4. Tap **TEST** to attempt playback, or **COPY** / export.

### Cross-app share-in capture
1. In another app, use its **Share** action on a URL or link.
2. Choose **Ananta Stream Catcher**.
3. If the shared text contains an M3U8/M3U URL, it is saved. If it contains a normal web page URL, the page opens in the built-in browser for browser-side detection.
4. If the app offers **Copy link** instead, copy it and use **PASTE** in Ananta Stream Catcher.

## Project structure

```text
app/
  build.gradle.kts
  src/main/
    AndroidManifest.xml
    java/com/ananta/streamcatcher/MainActivity.kt
    res/values/styles.xml
build.gradle.kts
settings.gradle.kts
gradle.properties
.github/workflows/build-apk.yml
```

## Notes

- Use only stream URLs you own or are authorized to access and test.
- A detected URL may be short-lived, tokenized, geo-restricted, or require headers/cookies from the original app/session. Copying it does not guarantee playback elsewhere.
- The app does not upload captured URLs to a server; the local history stays on the device unless you explicitly share/export it.
- Debug APKs are not release-signed for Play Store distribution.


## Version 1.2.0 – glass UI and improved HLS candidate detection

- Premium dark glass-style cards, cyan/purple gradient actions and clearer capture sections.
- WebView request inspection plus page resource, Fetch and XHR URL observation.
- URL candidate extraction handles encoded URLs and playlist URLs with query tokens.
- Share-in capture from other apps, local history, copy, Media3 test playback and M3U export.

### Capture limitations

Android WebView request callbacks expose request URLs, not arbitrary response bodies or response Content-Type headers. Some sites construct playlist URLs dynamically or use opaque endpoints, so detection is not guaranteed for every player. The JavaScript resource observer improves coverage but is subject to page/CSP/frame restrictions. Other apps' HTTPS requests are not visible to this app. Use Share/Copy Link for URLs made available by those apps. No DRM, certificate pinning, or access-control bypass is implemented.


## Build fix (JVM target)
Java compilation and Kotlin compilation are both configured for JVM 17 in `app/build.gradle.kts` to avoid the GitHub Actions target mismatch. The workflow uses current checkout/setup-java action majors.
