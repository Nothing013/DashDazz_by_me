# DashDazz (Hyper-Glow Edition)

Hardware camera-ring light controller for Xiaomi HyperOS devices (Redmi Turbo 4 Pro, Poco F7, POCO X8 Pro Max). Drives the AW21024 RGB LED ring around the camera cutout with root (primary) or Shizuku.

Forked and modernized on [DashDazz_by_me](https://github.com/Nothing013/DashDazz_by_me) based on [DashDazz upstream](https://github.com/olivia-hye/DashDazz).

---

## 🌟 What's New in Hyper-Glow Edition

### 1. Modernized "Hyper-Glow" UI & Theming
- **Interactive Ring Preview Header**: Live Jetpack Compose Canvas simulating camera ring lighting in real time with the active speed, brightness, and colors.
- **Custom UI Accent Colors**: Neon Cyan, Cyber Purple, Electric Amber, Hyper Green, and AMOLED White.
- **Pure AMOLED Mode & Density**: True-black OLED background and Compact/Expanded card layouts.
- **Extended Lighting Effects**:
  - `Spectrum Wave`: Smooth rotational multi-color rainbow.
  - `Pulse Strobe`: High-energy flashing burst.
  - `Candle Flicker`: Organic, relaxing warm ambient flicker.
  - `Neon Cyberpunk`: Cyan and magenta dual-tone alternating cycles.
  - `Comet Chaser`: Sweeping orbital segment around the lens.
  - `Breathe & Fade`: Smooth alpha expanding pulse.
- **Preset Palette Manager**: Create, save, export, and load custom lighting color palettes with JSON export.

### 2. Multi-Trigger Priority Matrix
Concurrent lighting triggers now operate under a non-blocking Priority Manager:

| Priority | Level | Behavior |
| :--- | :--- | :--- |
| **Level 1 (Highest)** | **Notification Alerts** | Temporary high-priority override (pulses/flashes). |
| **Level 2** | **Screen Lock / Unlock Flash** | Rapid transition effect when locking or unlocking. |
| **Level 3** | **Charging & Battery Full** | Adaptive charging animations (breathing/gauge/gradient). |
| **Level 4** | **Special Events** | Low battery warning flash, pocket/DND suppression. |
| **Level 5** | **React to Audio** | Allowlisted music playback visualizer. |
| **Level 6 (Lowest)** | **Ambient Baseline** | Always-on or screen-on continuous ring effect. |

*Note: "Notif Only" mode now cleanly defines the ambient baseline (ring off by default), allowing Lock/Unlock flash, Charging, and Audio reactive triggers to run without getting blocked!*

### 3. Root-Cause Fix for Stuck-On LED Bug
- **Guaranteed Cleanup**: Coroutine loops now include non-cancellable `finally` blocks that ensure hardware turn-off commands execute reliably even during rapid state switches.
- **Synchronized Root Shell**: Shared execution locks eliminate concurrency collisions between `fastShell` and separate `su` processes.
- **Full AW21024 Power-Down**: Writes complete register sequence (`0x6e 0x00`, `0xa1 0x00`, `0xa0 0x00`, `0xab 0x00`, `0xac 0x00`) to guarantee hardware sleep.
- **LedStateWatchdog**: Background coroutine checks every 2.5 seconds to ensure the physical LED matches the expected trigger state, forcing an OFF signal if any external event dropped a command.
- **Internal Logger**: In-memory circular debug log accessible in developer settings.

### 4. Advanced Charging & Battery Engine
- **Adaptive Pulse**: Breathing rate automatically accelerates when Fast/Warp/Turbo charge is active.
- **Battery Fill Gauge**: Angular fill corresponding to current percentage (0% to 100%).
- **Dynamic Gradient**: Color changes smoothly from Red (<25%) to Amber to Cyan to Neon Green.
- **Low Battery Alert**: Optional warning pulse below 15%.
- **Zero CPU Waste**: Updates strictly on battery state broadcasts.

### 5. Media Allowlist & Audio Tuning
- Allowlist specific music apps (e.g. Spotify, YouTube Music, ViMusic) so React to Audio only triggers for your selected media players.
- Adjustable sensitivity sliders and dynamic color shift response.

### 6. System Automations
- **Quiet Hours Schedule**: Configurable sleep hours (e.g. 11 PM to 7 AM) with overnight wrap-around.
- **DND / Silent Mode Sync**: Suppresses ring lighting when Do Not Disturb or Silent is active.
- **Pocket & Proximity Guard**: Mutes lighting when the phone is face-down or inside a pocket.

---

## 🛠️ How to Compile & Install the App

### Option A: Automated GitHub Actions Build (Recommended)
1. Push your changes to `main` on your GitHub repository (`github.com/Nothing013/DashDazz_by_me`).
2. Go to the **Actions** tab on your GitHub repository.
3. Click on the latest workflow run: **Android CI**.
4. Under **Artifacts** at the bottom of the page, download **`DashDazz-debug`**.
5. Extract the ZIP file and install `app-debug.apk` on your phone!

### Option B: Local Compilation via Android Studio / CLI
Make sure you have JDK 17+ and the Android SDK installed on your computer:
```bash
# Clone your repository
git clone https://github.com/Nothing013/DashDazz_by_me.git
cd DashDazz_by_me

# Run unit tests
./gradlew :app:testDebugUnitTest

# Compile the Debug APK
./gradlew :app:assembleDebug

# Install directly to your connected device via ADB
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📱 Hardware & Compatibility
- **Primary Tested Device**: Redmi Turbo 4 Pro / Poco F7 hardware (Xiaomi HyperOS 3, Root Control Path)
- **Supported Hardware**: AW21024 LED Driver Chip via `/sys/class/leds/aw21024_led`
- **Fallback**: Shizuku via hidden `ILightsManager`

## 📄 License
GPL-3.0. Preserves all upstream notices and research citations intact.
