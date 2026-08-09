package io.github.mohithdas.opendisplay.tv.settings

object AwakePolicy {
    fun shouldKeepScreenAwake(
        policy: KeepAwakePolicy,
        receiverOpen: Boolean,
        connected: Boolean,
    ): Boolean {
        if (connected) return true
        return policy == KeepAwakePolicy.WHILE_OPEN && receiverOpen
    }
}

