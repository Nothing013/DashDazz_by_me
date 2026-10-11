# DashDazz

Hardware camera-ring light controller for Xiaomi HyperOS devices (POCO X8 Pro Max / Dash). Drives the RGB LED ring around the rear camera - smooth animations, music reaction, and notification alerts.

Based on https://github.com/fan-world-me/HyperOS-ColorLightManager-Research

## Highlights

- **4 lighting modes** - Rainbow cycle, Breathe pulse, Solid color, and Dual Ring with 4 dual effects (Hardware Split, Rotational Spin, Police Siren, Alternate) plus Beat Rainbow and Split Beat
- **Audio Reactive** - the selected mode dances to your music with automatic beat detection, band tracking (kick / snare / vocal / hat), and auto sensitivity; live oscilloscope and manual tuning screen included; Global Media Mix or Microphone source
- **6 color palettes** - Spectrum, Cyberpunk, Sunset, Ocean, Pastel, Fire - plus 8 presets and unlimited savable custom presets
- **Fine adjustments** - speed, brightness, saturation, hue, per-ring colors, spin steps, degrees-per-beat
- **Smart automation** - pause when screen is off, blink on notification, or fully activate lights on notification with configurable duration; per-behavior notification modes (Off / Blink / Activate)
- **Two control paths** - Root for full features incl. Dual Ring, or Shizuku for basic modes with no root
- **System integration** - Quick Settings tile, notification listener sync, HyperOS "Rhythmic light" conflict detection, dark / light / system theme

## Requirements

- Xiaomi HyperOS device with camera-ring LED (AW21024). Confirmed working: POCO X8 Pro Max, POCO X8 Pro, Redmi Turbo 5, Redmi Turbo 5 Max, Redmi Turbo 4 Pro
- Root (full features) and/or Shizuku (basic modes)
- Android 14+ (minSdk 34), compileSdk 35
- Microphone permission only for Audio Reactive mode - no audio is recorded or uploaded

## Build

```sh
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease   # needs signing secrets, see below
./gradlew :app:testDebugUnitTest
./gradlew printVersion
```

Debug builds use the standard auto-generated debug key and need no setup. Release builds are signed with `app/dashdazz.keystore.jks`; provide secrets via `local.properties`, `-P` flags, or env vars:

```properties
sdk.dir=/path/to/Android/Sdk
dashdazz.storePassword=...
dashdazz.keyPassword=...
dashdazz.keyAlias=...
```

## Get the app

1. Download the latest APK from the [**Releases page**](https://github.com/olivia-hye/DashDazz/releases).
2. Install it on your phone (allow install from unknown sources when asked).
3. Open DashDazz and grant access: **Root** for full features incl. Dual Ring, or start **Shizuku** for basic modes with no root.
4. Pick a lighting mode, tune colors and speed, and turn the ring on. Optionally enable Audio Reactive (needs microphone permission) or notification lighting.

## License

GPL-3.0-or-later. See `LICENSE` and `NOTICE`.
