package io.github.mohithdas.opendisplay.tv

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityStartupTest {
    @Test
    fun activityRemainsVisibleWhileReceiverStarts() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var resumed = false
            scenario.onActivity { activity ->
                resumed = !activity.isFinishing && !activity.isDestroyed
            }

            assertTrue(resumed)
            assertFalse(scenario.state.name == "DESTROYED")
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            val expectedStates = listOf(
                By.textStartsWith("Listening at"),
                By.textStartsWith("Waiting for an active local network"),
                By.textStartsWith("Listener error"),
                By.textStartsWith("No port available"),
            )
            var reachedRecoverableReceiverState = false
            for (attempt in 0 until 20) {
                if (expectedStates.any(device::hasObject)) {
                    reachedRecoverableReceiverState = true
                    break
                }
                Thread.sleep(500)
            }
            assertTrue(reachedRecoverableReceiverState)
        }
    }
}
