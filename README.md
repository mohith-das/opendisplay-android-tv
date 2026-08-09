# OpenDisplay TV

[![Download latest APK](https://img.shields.io/badge/Download_latest_APK-OpenDisplay--TV.apk-2ea44f?style=for-the-badge&logo=android)](https://github.com/mohith-das/opendisplay-android-tv/releases/latest/download/OpenDisplay-TV.apk)

## Download latest APK

Download **[OpenDisplay-TV.apk](https://github.com/mohith-das/opendisplay-android-tv/releases/latest/download/OpenDisplay-TV.apk)** from the latest stable release. The matching SHA-256 file is available beside it.

> **Unofficial community project.** OpenDisplay TV is not an official OpenDisplay Android application and is not maintained or endorsed by the original OpenDisplay authors. It is a community Android TV fork of [josepacelli/opendisplay-android](https://github.com/josepacelli/opendisplay-android), compatible with the protocol and Mac sender from [peetzweg/opendisplay](https://github.com/peetzweg/opendisplay).

OpenDisplay TV turns an Android TV, Google TV, Onn streaming device, phone, or tablet into a real extended display for a Mac. It receives low-latency H.264 directly into Android's hardware `MediaCodec` decoder and renders to a `SurfaceView`. It does **not** use AirPlay, avoiding AirPlay receiver color-conversion issues and receiver-mode restrictions.

## Requirements

- Android 8.0 / Android TV 8.0 (API 26) or newer.
- A Mac running the original [OpenDisplay Mac app](https://github.com/peetzweg/opendisplay/releases).
- Both devices on the same trusted local network. Ethernet and Wi-Fi can work when the router bridges multicast and client traffic.

The receiver protocol is currently unauthenticated and unencrypted. **Use it only on a trusted local network.** Anyone able to reach the receiver port may connect and send protocol traffic.

## Install on Onn, Android TV, or Google TV

1. Download `OpenDisplay-TV.apk` on a computer or directly on the TV device.
2. Allow the installer you use to install unknown apps:
   - Google TV / newer Onn: **Settings → Apps → Special app access → Install unknown apps**.
   - Android TV variants: **Settings → Device Preferences → Security & Restrictions → Unknown sources**.
3. Transfer the APK with a USB drive, cloud-storage file manager, or ADB.
4. Open the APK and choose **Install**.
5. Launch the **OpenDisplay TV** tile from the TV home screen.

ADB installation is often simplest for development:

```bash
adb connect TV_IP_ADDRESS
adb install -r OpenDisplay-TV.apk
```

On a phone or tablet, enable installation for the browser or file manager that downloaded the APK, open it, and choose **Install**. The same app supports touch and two-finger scrolling on touchscreen devices.

### Upgrading from a prototype

The production package is `io.github.mohithdas.opendisplay.tv` and uses a production signing certificate. Earlier prototypes used debug signing and identities such as `io.github.josepacelli.opendisplay.tv`; Android may show both tiles. Uninstall the prototype before installing this release if installation fails or duplicate tiles appear:

```bash
adb uninstall io.github.josepacelli.opendisplay.tv
adb install OpenDisplay-TV.apk
```

Future OpenDisplay TV releases signed with the same production key install with `adb install -r` and retain settings.

## Connect from a Mac

1. Install and open the original OpenDisplay Mac app.
2. Open OpenDisplay TV on the Android device. The main screen shows the complete listener address, normally `TV_IP_ADDRESS:9010`.
3. In the Mac app, choose the Android receiver from the Wi-Fi device list.

The receiver tries TCP ports **9010 through 9029** in order. It advertises `_opensidecar._tcp` with the selected port plus the install ID and protocol version. TCP listener state and Bonjour/NSD state are displayed independently; a working listener does not automatically mean Bonjour registration succeeded.

The Mac app supports multiple live device sessions. Launch OpenDisplay on each receiver and connect each row; every receiver becomes its own extended display. Available encoder, Wi-Fi, and Mac performance determine the practical device count.

### Manual IP fallback

The current upstream Mac source still reads `host` and `port` from `UserDefaults` and dials that plain TCP endpoint when a host override exists. If Bonjour does not list the TV, copy the address shown in **Settings & Diagnostics**, quit OpenDisplay on the Mac, and run:

```bash
defaults write com.peetzweg.opensidecar.mac host -string TV_IP_ADDRESS
defaults write com.peetzweg.opensidecar.mac port -string TV_PORT
```

Quit and reopen OpenDisplay afterward. To remove the override and restore automatic USB/Wi-Fi discovery:

```bash
defaults delete com.peetzweg.opensidecar.mac host
defaults delete com.peetzweg.opensidecar.mac port
```

Quit and reopen OpenDisplay again after deleting the values.

## TV settings

Every control has an Android TV focus outline and works with a D-pad and OK button. **Settings** remains available while disconnected, listening, or streaming. Back closes settings first; leaving the Activity does not accidentally stop the foreground receiver.

### Resolution and UI scale

- **Auto / Native** chooses the active physical display mode while respecting the application window and AVC decoder capability. TVs reporting 4K are conservatively capped at 1080p in this release; 4K is never the default.
- **1920×1080** and **1280×720** appear only when the display and decoder can support them.
- **Auto UI scale** selects 1× on Android TV and a density-aware value on phones/tablets.
- **1×** requests a full logical-pixel workspace; **2× HiDPI** requests larger interface elements.

The selected pixel dimensions and scale are sent in every protocol `hello`, and a changed resolution triggers an updated hello. A current upstream Mac limitation is important: its virtual-display code still divides received pixel dimensions by two and does not yet apply the `scale` field. Consequently, the Mac may continue to expose a 960×540 logical workspace for a 1920×1080 hello even when 1× is selected. The Android setting and wire value are ready, but fully honoring 1× requires a compatible Mac-side change; this fork does not misrepresent or silently replace the upstream Mac app.

### Fit to screen

- **Fit** (default): preserves aspect ratio and shows the complete image with letterboxing or pillarboxing.
- **Fill**: preserves aspect ratio and crops equally to fill the display.
- **Stretch**: fills the display without preserving aspect ratio and visibly warns about distortion.
- **1:1 native pixels**: maps one decoded pixel to one display pixel and crops when the image is larger.

The video surface, local cursor, touch input, and pointer coordinates share one transform, including fill-mode crop offsets and fit-mode bars.

### Keep screen awake

- **While connected**: keeps the display awake during an active Mac stream.
- **While receiver is open**: also keeps an idle TV receiver available for reconnection; this is the Android TV default.
- **Never**: allows normal sleep while idle. Active streaming still remains awake to prevent an unexpected session interruption.

Phones and tablets default to **While connected**. The app uses `FLAG_KEEP_SCREEN_ON` and `SurfaceView.keepScreenOn`, releases them when no longer needed, and does not use deprecated brightness wake locks or an indefinite CPU wake lock. It can prevent Android's screensaver, Ambient Mode, dimming, and device sleep while active, but cannot override a physical television's separate sleep timer, eco mode, or automatic power-off configuration.

The optional performance overlay reports FPS, approximate network throughput, latency, RTT, and approximate Java heap use. Diagnostics also show receiver name, IP, listener port, TCP status, Bonjour status, connection state, selected output, fit mode, awake policy, and decoder resolution.

## Troubleshooting

### Listener does not start

- Confirm the device has a local IPv4 address and is not in airplane mode.
- Check **Settings & Diagnostics**. The app retries recoverable failures with bounded backoff.
- Ports 9010–9029 may all be occupied; stop conflicting receiver/server apps and reopen OpenDisplay TV.
- After a network change, allow a few seconds for the listener to bind the new interface and Bonjour to re-register.

### Mac cannot find the TV with Bonjour

- Confirm **Bonjour / NSD** says **Discoverable**, not merely that TCP is listening.
- Put both devices on the same normal LAN. Guest Wi-Fi commonly enables client isolation, blocking both mDNS multicast and direct TCP.
- Disable wireless/AP isolation and allow multicast/mDNS between Wi-Fi and Ethernet clients.
- VPNs, managed networks, and some mesh systems can choose or isolate a different active interface.
- Reopen the receiver after changing networks, or use the manual host/port fallback above.

### Performance and memory

Use 1080p on capable TV sticks and 720p on lower-end or congested devices. **Fit** does not copy video frames; decoded frames remain in the hardware codec/surface pipeline. The receiver keeps bounded frame and cursor queues, releases codecs/sockets/surfaces on lifecycle changes, and avoids per-frame bitmap conversion. Memory use varies with vendor codec buffers, resolution, firmware, and reconnect history; the diagnostics value is an approximate Java heap reading, not a promise of total process memory.

## Build from source

Install JDK 21 and Android SDK 36, then run:

```bash
git clone https://github.com/mohith-das/opendisplay-android-tv.git
cd opendisplay-android-tv
./gradlew testDebugUnitTest lintDebug assembleDebug
```

The debug APK is under `app/build/outputs/apk/debug/`. Release signing uses an ignored `keystore.properties` file pointing to a protected keystore outside the repository. CI restores the same PKCS#12 key from encrypted GitHub Actions secrets, signs the APK, verifies it with `apksigner`, and publishes `OpenDisplay-TV.apk` plus `OpenDisplay-TV.apk.sha256`. No keystore or signing password belongs in Git.

See [CONTRIBUTING.md](CONTRIBUTING.md) for development checks and [SECURITY.md](SECURITY.md) for private vulnerability reporting.

## Known limitations

- Physical Onn/Google TV behavior varies by firmware; complete the release checklist on the target device before relying on it unattended.
- Current upstream Mac code does not yet honor the hello `scale` field, as explained above.
- The protocol has no pairing, authentication, encryption, or internet-safe transport.
- H.264 capability reporting is vendor supplied; a device can claim a mode it cannot sustain at low latency.
- Touch gestures are meaningful on phones/tablets; most TV remotes only operate app controls.
- The app cannot control the television panel's independent power-saving settings.

## Attribution and license

OpenDisplay TV preserves the complete history and GitHub fork relationship of the unofficial GPL-3.0 Android implementation [josepacelli/opendisplay-android](https://github.com/josepacelli/opendisplay-android). Its protocol behavior derives from the original GPL-3.0 [peetzweg/opendisplay](https://github.com/peetzweg/opendisplay) project. See [NOTICE](NOTICE) for attribution.

Licensed under the [GNU General Public License v3.0](LICENSE).
