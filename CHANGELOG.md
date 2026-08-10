# Changelog

All notable changes to this project are documented here. Versions follow semantic versioning.

## Unreleased

### Changed

- The Settings button now fades out after five seconds of inactivity while connected. Any remote-key or pointer interaction reveals it and restarts the timer; D-pad/OK focuses it, and Menu/Settings opens the dialog directly.

## [0.1.1] - 2026-08-09

### Fixed

- Fixed the production startup crash on Android 11 and newer. Service startup now uses `DisplayManager.DEFAULT_DISPLAY` and conservative 720p/1080p fallbacks; only an Activity may read visual window metrics.
- Contained vendor display-mode and AVC capability failures so the foreground receiver still reaches a visible listening, waiting-for-network, or recoverable error state.
- Updated the accurate display profile after `MainActivity` binds, including an updated protocol hello when a Mac connected before Activity detection completed.
- Removed the receiver-name text field from normal D-pad focus traversal, preventing it from trapping TV remote navigation.

### Added

- Added a secure, TV-friendly updater backed by the repository's latest stable GitHub release API. It uses anonymous public requests and embeds no GitHub token.
- Added exact asset-name, HTTPS redirect-host, streamed SHA-256, GitHub digest, package ID, monotonically increasing version code, tag/version, and Android signing-lineage validation.
- Added app-private streaming downloads, progress, release notes, retry states, dismiss/skip behavior, an opt-in unmetered auto-download setting, and Android's user-confirmed installer flow.
- Added a throttled foreground check, constrained daily WorkManager check, nonintrusive completion notification, and streaming-aware download/install deferral.
- Added API 30 startup regression coverage, Android instrumentation, and focused tests for update parsing, version policy, throttling, assets, checksums, signatures, redirects, cleanup, persistence, and WorkManager constraints.

### Upgrade note

- Version 0.1.0 crashes before it can update itself on Android 11 and newer. Install v0.1.1 manually over v0.1.0; v0.1.1 and later can use the in-app updater.

## [0.1.0] - 2026-08-09

### Added

- Android TV and Google TV launcher support, TV banner, and D-pad focus states.
- Sequential listener fallback from TCP 9010 through 9029 with the full selected address shown.
- Multicast-lock-backed Bonjour registration, independent NSD diagnostics, retry backoff, and network-change re-registration.
- Persistent TV resolution, UI scale, Fit/Fill/Stretch/1:1, keep-awake, and optional performance overlay settings.
- Shared video/cursor/input transform and focused tests for settings, resolution, listener, NSD, transform, and awake policy.
- Production identity, signing workflow, CI checks, release automation, and English/Portuguese resources.

### Changed

- TV Auto output defaults to a low-memory 1080p ceiling instead of advertising 4K.
- Android TV defaults to keeping the screen awake while the receiver Activity is open; handhelds default to connected-only.
- The frame queue is bounded to favor current low-latency video.

### Security

- The receiver binds to the active local IPv4 interface and documents the unauthenticated trusted-LAN boundary.

[0.1.0]: https://github.com/mohith-das/opendisplay-android-tv/releases/tag/v0.1.0
[0.1.1]: https://github.com/mohith-das/opendisplay-android-tv/releases/tag/v0.1.1
