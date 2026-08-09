// Derived from Shared/Protocol.swift in peetzweg/opendisplay.
// Copyright (c) 2026 Philip Poloczek. Licensed under GPL-3.0.

package io.github.mohithdas.opendisplay.tv.protocol

/**
 * Wire-protocol version contract, ported from the upstream Shared/Protocol.swift.
 * The Android receiver currently speaks protocol 2. The protocol-3 Mac remains backward
 * compatible with protocol 1+, so advertising 2 accurately avoids claiming unsupported Pencil
 * messages while preserving every video, cursor, touch, and lifecycle message used here.
 */
object WireProtocol {
    const val VERSION = 2
    const val MIN_SUPPORTED_PEER = 1
    const val ASSUMED_WHEN_ABSENT = 1
}

/** Control-message `type` string constants (mirrors Swift `WireMessage`). */
object WireMessage {
    const val HELLO = "hello"
    const val PING = "ping"
    const val PONG = "pong"
    const val TOUCH = "touch"
    const val SCROLL = "scroll"
    const val KEYFRAME_REQUEST = "kf"
    const val CURSOR = "cursor"
    const val CURSOR_IMAGE = "cursorImg"
    const val STATS = "stats"
    const val WELCOME = "welcome"
    const val UPDATE_REQUIRED = "updateRequired"
    const val SLEEPING = "sleeping"
    const val CLOSING = "closing"
}
