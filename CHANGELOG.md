# Changelog

All notable changes to this project are documented here. Versions follow semantic versioning.

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

