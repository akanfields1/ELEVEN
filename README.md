# ELEVEN // V1

A tiny native Android system-audio booster with a black/yellow cyberpunk UI.

- No ads
- No analytics
- No network permission
- Native Kotlin + Android Views
- System media-volume control
- 100% to 200% boost presets
- Persistent foreground-service mode so the effect can remain active when you switch back to a music/video app
- GitHub Actions workflow that builds a debug APK

## Important compatibility note

Android does not provide a modern guaranteed public API for third-party apps to boost every other app's audio above the device maximum. ELEVEN V1 therefore uses Android's `LoudnessEnhancer` against audio session `0` as a best-effort global-mix effect, then falls back to a flat `Equalizer` boost if needed. Global insert effects on session `0` are deprecated by Android and behavior varies by phone, Android build, audio route, Bluetooth device, and OEM audio stack.

That is why V1 reports the selected engine in the UI instead of silently pretending the boost is working.

## Boost scale

The UI percentage is converted to gain using:

`gain dB = 20 * log10(percent / 100)`

So approximately:

- 100% = +0.0 dB
- 125% = +1.9 dB
- 150% = +3.5 dB
- 175% = +4.9 dB
- 200% = +6.0 dB

## Build locally

Open the folder in a current Android Studio version and build the `app` module.

The project uses Android Gradle Plugin 9.4.1, compile SDK 36, minimum SDK 26, and target SDK 33 for V1 compatibility.

## Build on GitHub

Push to `main`, then open **Actions → Build APK**. The workflow publishes `ELEVEN-debug-apk` as a downloadable build artifact.

## Safety

Extra gain can cause clipping, distortion, hearing damage, or speaker/headphone damage. Start low.
