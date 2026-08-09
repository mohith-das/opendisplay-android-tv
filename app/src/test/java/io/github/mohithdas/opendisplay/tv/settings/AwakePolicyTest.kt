package io.github.mohithdas.opendisplay.tv.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AwakePolicyTest {
    @Test
    fun liveStreamAlwaysStaysAwake() {
        KeepAwakePolicy.entries.forEach {
            assertTrue(AwakePolicy.shouldKeepScreenAwake(it, receiverOpen = false, connected = true))
        }
    }

    @Test
    fun openPolicyKeepsIdleReceiverAvailable() {
        assertTrue(
            AwakePolicy.shouldKeepScreenAwake(
                KeepAwakePolicy.WHILE_OPEN,
                receiverOpen = true,
                connected = false,
            ),
        )
    }

    @Test
    fun leavingActivityReleasesIdleAwakeState() {
        assertFalse(
            AwakePolicy.shouldKeepScreenAwake(
                KeepAwakePolicy.WHILE_OPEN,
                receiverOpen = false,
                connected = false,
            ),
        )
    }
}

