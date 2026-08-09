# Software design notes

## Receiver lifecycle

`ReceiverService` owns one `PhoneReceiver` and keeps it alive independently of Activity recreation. The Activity binds for observable state and video UI. Back or task removal may leave the Activity without stopping the foreground service; force-stop or normal service destruction performs final shutdown.

The TCP listener binds to the active local IPv4 interface and tries 9010–9029 sequentially. A network callback closes the old listener, unregisters stale NSD state, and lets the bounded-backoff listener loop resolve and bind the new active interface.

TCP and NSD states are separate. NSD registration starts only after a socket is bound, advertises that exact port, and is generation guarded so a retry cannot publish a stale port. A Wi-Fi multicast lock is acquired before `registerService` and released during unregister.

## Display pipeline

The selected resolution is constrained by current physical display mode, window bounds, and reported H.264 decoder capability. Android TV Auto is capped at 1080p; handheld Auto preserves window-native behavior. Resolution and scale are carried in `hello` and resent after display-affecting changes.

H.264 stays in `MediaCodec` and renders directly to `SurfaceView`. The shared viewport layout sizes and positions the surface and cursor identically for Fit, Fill, Stretch, and 1:1. Pure `VideoTransform` math provides matching coordinate mapping tests.

Frame and cursor flows are bounded and drop oldest values under load. Codec, socket, surface, NSD registration, multicast lock, and callbacks have explicit release paths.

## Settings and awake policy

Pure settings models are persisted through a small storage interface, allowing JVM persistence tests without Android framework mocks. TV and handheld awake defaults differ. Active streaming always keeps the display awake; idle behavior follows the selected policy. Only the Activity window and live decoder surface request screen-on state—no brightness or indefinite CPU wake locks are used.

## Security boundary

Wire compatibility requires an unauthenticated local TCP service, stable install ID TXT record, and existing framing. Defensive limits include maximum wire frame size, bounded queues, cursor dimension checks, update URL allow-listing, active-interface binding, and visible peer replacement. See [SECURITY.md](SECURITY.md).
