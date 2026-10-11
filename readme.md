# DashDazz

Android system-level utility that controls the RGB LED camera ring on Xiaomi HyperOS devices (POCO X8 Pro Max / Dash). Animated lighting effects (rainbow, breathe, solid, dual-ring) via the `miui.lights.ILightsManager` Binder service (Shizuku, no root) or direct AW21024 sysfs writes (root).

Based on https://github.com/fan-world-me/HyperOS-ColorLightManager-Research

## Requirements

- Xiaomi HyperOS device with camera-ring LED (AW21024)
- Root (full features incl. Dual Ring) and/or Shizuku (basic modes)
- Android 8.0+ (minSdk 26), compileSdk 35
- Microphone permission only for Audio Reactive mode (no audio recorded/uploaded)

## Build

```sh
./gradlew :app:assembleDebug
./gradlew printVersion
```

## Signing

Release signing uses `app/dashdazz.keystore.jks`. Provide secrets via `local.properties`, `-P` flags, or env vars:

```properties
dashdazz.storePassword=...
dashdazz.keyPassword=...
dashdazz.keyAlias=dashdazz
```

## License

GPL-3.0-or-later. See `LICENSE` and `NOTICE`.
