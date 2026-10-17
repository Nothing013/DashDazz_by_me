# DashDazz Changelog

## [2.1.0-HyperGlow] - 2026-10-17

### 🎨 Task 1: Modernized "Hyper-Glow" UI & Theming Customization
- **Dashboard Redesign**: Futuristic glassy card overlays, animated gradient outlines, and soft ambient light shadows.
- **Interactive Preview Header**: Real-time animated Jetpack Compose Canvas camera ring simulating active color, effect, speed, and brightness directly in the dashboard header.
- **UI Accent Color Picker**: Added support for Neon Cyan, Cyber Purple, Electric Amber, Hyper Green, and AMOLED White.
- **Pure AMOLED Dark Mode**: True-black background toggle to save battery on OLED displays.
- **Card Layout Density**: Quick toggle between Compact View and Expanded Glass Cards.
- **Glow Intensity Control**: Configurable UI shader glow strength.
- **Preset Palette Manager**: Create, save, export, and load custom lighting color palettes and dual-tone gradients with JSON serialization.
- **Extended Lighting Effects Library**:
  - `Spectrum Wave`: Smooth rotational rainbow wave.
  - `Pulse Strobe`: High-energy flashing burst for urgent notifications.
  - `Candle Flicker`: Organic warm ambient flicker mode.
  - `Neon Cyberpunk`: Dual-tone alternating cyan/magenta cycles.
  - `Comet Chaser`: Sweeping orbital lit segment around camera ring.
  - `Breathe & Fade`: Gentle expanding pulse with smooth alpha transitions.

### 🛡️ Task 2: Root Cause Fix for Stuck-On LED Bug
- **Concurrency & Cancellation Hardening**: Eliminated coroutine race conditions and unhandled cancellations in LED loops. Added guaranteed non-cancellable `finally` blocks to ensure hardware turn-off is never dropped.
- **Root Shell Synchronization**: Synchronized fast-shell and root-process command pipelines with shared locks.
- **Hardware Power-Down Sequence**: Implemented full AW21024 shutdown register sequence (`0x6e 0x00`, `0xa1 0x00`, `0xa0 0x00`, `0xab 0x00`, `0xac 0x00`) to guarantee zero residual LED drive.
- **LedStateWatchdog**: Background coroutine state checker running every 2.5 seconds. If hardware remains lit while no trigger is active, the watchdog automatically forces a hardware power-down.
- **LedEventLogger**: In-memory circular debug log recording all hardware state transitions.

### ⚡ Task 3: Multi-Trigger Priority Manager & "Notif Only" Rework
- **"Notif Only" Decoupled**: "Notif Only" now defines strictly the ambient baseline (ring off by default), allowing Screen Lock/Unlock Flash, React to Audio, Charging, and Notifications to co-exist cleanly.
- **Non-Blocking Priority Architecture**:
  - **Priority 1 (Highest)**: Notification Alerts (Temporary Overrides)
  - **Priority 2**: Lock / Unlock / Screen Flash Effects
  - **Priority 3**: Charging Animations & Battery Full Alerts
  - **Priority 4**: Special Event Alerts (Pocket Mode, DND, Low Battery)
  - **Priority 5**: React to Audio (Per-App Allowlisted)
  - **Priority 6 (Lowest)**: Ambient / Always-On Ring Baseline
- When higher-priority events end, the next active trigger is seamlessly restored.

### 🔋 Task 4: Advanced Charging & Battery Indicator Engine
- **Battery Engine**: Listens to system power connection and battery broadcasts.
- **Multi-Effect Selection**:
  - `Adaptive Pulse`: Breathing rate scales with charge speed (Fast/Warp charge = faster pulse).
  - `Battery Level Fill Gauge`: Multi-segmented angular fill reflecting battery percentage (0% to 100%).
  - `Dynamic Color Gradient`: Smooth transition from Red (<25%) -> Amber -> Cyan -> Neon Green (full).
- **Low Battery Warning Flash**: Alert pulse when battery drops below 15%.
- **Battery Full Action**: Configurable solid color and auto-timeout upon reaching 100%.

### 🎵 Task 5: Per-App Media Allowlist & Visualizer Enhancements
- **Media Allowlist**: Select between "All System Audio" and "Selected Apps Only" (e.g. Spotify, YouTube Music, ViMusic).
- **Gain & Smoothing Controls**: Audio sensitivity sliders and dynamic color shift calculations.
- **Graceful Fallbacks**: Seamless handling of media pauses and background playback switches.

### 🌙 Task 6: System Automations & Hardware Guards
- **Quiet Hours Schedule**: Configurable start/end night hours (e.g. 11 PM to 7 AM) with overnight wrap-around.
- **DND / Silent Mode Sync**: Automatically suppresses or dims ring lighting when Do Not Disturb or Silent mode is active.
- **Pocket & Proximity Guard**: Utilizes the proximity sensor to temporarily mute ring lighting when the device is face-down or inside a pocket.

### 🧪 Unit Testing & CI/CD
- Added unit tests:
  - `TriggerPriorityManagerTest`
  - `ChargingStateMachineTest`
  - `AudioAllowlistTest`
  - `QuietHoursTest`
- Hardened GitHub Actions `.github/workflows/android.yml` to automatically build and upload debug APK artifacts on every push even without signing secrets.
