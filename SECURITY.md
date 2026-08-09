# Security policy

## Supported versions

Security fixes are applied to the latest release. Older APKs may not receive fixes.

## Report a vulnerability

Please use a [private GitHub security advisory](https://github.com/mohith-das/opendisplay-android-tv/security/advisories/new). Do not open a public issue for an unpatched vulnerability. Include affected versions, device/Android version, reproduction steps, impact, and a suggested mitigation when possible.

This is a volunteer open-source project with no bug bounty. Reports will be acknowledged as time permits; coordinated disclosure is appreciated.

## Trust boundary

OpenDisplay's compatible wire protocol uses an unauthenticated, unencrypted TCP connection. OpenDisplay TV listens on one active local IPv4 interface and ports 9010–9029. Bonjour publishes the install ID and protocol version. These behaviors are required for compatibility with the current Mac sender.

Use the receiver only on a trusted LAN. Guest networks, public Wi-Fi, routed untrusted subnets, and exposed port forwarding are unsafe. A reachable peer can attempt to connect, replace an active session, send control messages, and supply compressed media/cursor input to platform decoders.

The app validates frame lengths, bounds cursor images, allow-lists update URLs, uses bounded queues, and surfaces peer replacement. Those defenses do not add authentication or encryption.
