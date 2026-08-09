# Contributing

Thanks for improving this unofficial community Android TV fork.

## Development setup

Use JDK 21 and Android SDK 36. Keep changes compatible with Android 8.0 (API 26), preserve the OpenDisplay wire protocol, and keep the video path on hardware `MediaCodec` plus `SurfaceView`.

Before submitting a change, run:

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
git diff --check
```

Add focused JVM tests for pure policy, selection, transform, framing, or lifecycle logic. UI changes should be tested with a D-pad: every interactive element needs a visible focus state, Settings must remain reachable in every receiver state, and Back must not stop the service unexpectedly.

## Pull requests

- Keep commits focused and explain protocol or lifecycle tradeoffs.
- Put user-facing text in `res/values/strings.xml`; add Portuguese in `values-pt` when practical.
- Do not commit APKs, local SDK paths, signing files, passwords, tokens, or generated credentials.
- Do not change the protocol incompatibly without coordinating with the original OpenDisplay Mac project.
- Confirm that new network behavior binds only intended local interfaces and remains safe under reconnects.

Bug reports should include the TV/device model, Android version, selected resolution, TCP and Bonjour states, Mac version, and relevant `adb logcat -s OpenDisplay:*` output with private addresses redacted if desired.

