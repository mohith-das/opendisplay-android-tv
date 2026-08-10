package io.github.mohithdas.opendisplay.tv.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoDisconnectPolicyTest {
    @Test
    fun retainedFrameIsClearedOnlyOnDisconnectTransition() {
        assertFalse(VideoDisconnectPolicy.shouldClearRetainedFrame(false, false))
        assertFalse(VideoDisconnectPolicy.shouldClearRetainedFrame(false, true))
        assertFalse(VideoDisconnectPolicy.shouldClearRetainedFrame(true, true))
        assertTrue(VideoDisconnectPolicy.shouldClearRetainedFrame(true, false))
    }

    @Test
    fun queuedFramesAreIgnoredAfterDisconnect() {
        assertTrue(VideoDisconnectPolicy.shouldSubmitFrame(connected = true))
        assertFalse(VideoDisconnectPolicy.shouldSubmitFrame(connected = false))
    }
}
