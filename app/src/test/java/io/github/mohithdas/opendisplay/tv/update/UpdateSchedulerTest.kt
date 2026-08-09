package io.github.mohithdas.opendisplay.tv.update

import androidx.work.NetworkType
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateSchedulerTest {
    @Test
    fun periodicWorkRequiresAConnectedNetwork() {
        val request = UpdateScheduler.request()
        assertEquals(NetworkType.CONNECTED, request.workSpec.constraints.requiredNetworkType)
        assertEquals(UpdatePolicy.CHECK_INTERVAL_MS, request.workSpec.intervalDuration)
    }
}
