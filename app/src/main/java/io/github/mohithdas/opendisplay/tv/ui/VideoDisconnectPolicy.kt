package io.github.mohithdas.opendisplay.tv.ui

internal object VideoDisconnectPolicy {
    fun shouldClearRetainedFrame(previouslyConnected: Boolean, connected: Boolean): Boolean =
        previouslyConnected && !connected

    fun shouldSubmitFrame(connected: Boolean): Boolean = connected
}
