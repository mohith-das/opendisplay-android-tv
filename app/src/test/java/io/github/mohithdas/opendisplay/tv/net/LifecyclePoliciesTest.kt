package io.github.mohithdas.opendisplay.tv.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LifecyclePoliciesTest {
    @Test
    fun retryBackoffIsBounded() {
        assertEquals(1_000, RetryBackoff.delayMillis(0))
        assertEquals(2_000, RetryBackoff.delayMillis(1))
        assertEquals(30_000, RetryBackoff.delayMillis(20))
    }

    @Test
    fun nsdNeverRegistersAgainstAStalePort() {
        assertTrue(NsdRegistrationPolicy.canRegister(true, 9014, 9014))
        assertFalse(NsdRegistrationPolicy.canRegister(true, 9015, 9014))
        assertFalse(NsdRegistrationPolicy.canRegister(false, 9014, 9014))
        assertFalse(NsdRegistrationPolicy.canRegister(true, null, 9014))
    }
}

