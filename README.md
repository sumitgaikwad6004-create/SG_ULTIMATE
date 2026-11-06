# SG_ULTIMATE — Android browser source

**Version:** 5.0  
**Package:** `com.sg.browser`

A phone-buildable Kotlin Android project combining a WebView browser, direct-stream video player and device audio effects.

## Included features

### Browser
- URL/search bar with search fallback
- Back, forward, reload and home
- Basic bookmarks and local browsing history
- Private-mode toggle (new visits are not saved to history)
- Desktop-site toggle
- Find text on page
- Share page and copy URL
- DownloadManager-based file downloads
- Clear cookies, cache, form data and saved browser history
- Basic privacy setting placeholder

### Video
- Direct MP4 and HLS (`.m3u8`) playback through AndroidX Media3
- Playback speed presets from 0.25× to 4×
- Maximum-resolution selection (only if the stream exposes selectable tracks)
- 10-second seek controls
- Landscape toggle
- Brightness, contrast, saturation, hue and lightness controls

### Audio
- Android Equalizer, BassBoost and Virtualizer where supported by the device

## Important limitations
- This is **source code**, not a compiled APK. It has not been built or device-tested in this package.
- Website video playback normally remains inside the website's own player. The separate player supports direct MP4/HLS URLs; DRM-protected streams cannot be bypassed.
- The quality selector limits available tracks; it cannot create resolutions that the stream does not provide.
- The privacy/ad-block menu is a placeholder setting, not a full filter-list ad blocker. Do not treat it as protection against all tracking or ads.
- Private mode here avoids adding new page visits to this app's history. It does not hide activity from websites, your network, your ISP, or Android.
- Android audio effects depend on device/vendor support and may not affect every output route.

## Build requirements
- Android Studio or a compatible Android IDE with Gradle support
- JDK 17
- Android SDK 35
- Internet access for Gradle dependency downloads

Open this folder as an Android Gradle project and build the `app` debug variant. The project uses Kotlin DSL and AndroidX Media3 1.5.1.

## No GitHub required
You can copy this ZIP to an Android IDE that supports Gradle projects. No GitHub repository is required.
