# ELEVEN // V1

A tiny native Android system-audio booster with a black / neon-yellow UI.

- No ads
- No analytics
- No network permission
- Native Kotlin + Android Views
- System media-volume control
- 100% to 200% boost presets
- Persistent foreground-service mode so the effect can remain active when you switch back to a music/video app

## Stable update signing

GitHub Actions now reuses one cached Android debug signer for ELEVEN instead of creating a different signer on every cloud runner. A scheduled weekly build keeps that cache active.

The very first ELEVEN build used a temporary runner-generated signature, so moving onto this stable update track requires one uninstall/reinstall. After the stable-signed build is installed, later builds should install over it normally.

## Audio compatibility

ELEVEN uses Android's `LoudnessEnhancer` against audio session `0` as a best-effort global-mix effect, then falls back to a flat `Equalizer` boost if needed. Behavior can vary by device and audio route.

## Safety

Extra gain can cause clipping, distortion, hearing damage, or speaker/headphone damage. Start low.
