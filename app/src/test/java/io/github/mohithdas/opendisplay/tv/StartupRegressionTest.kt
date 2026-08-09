package io.github.mohithdas.opendisplay.tv

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import io.github.mohithdas.opendisplay.tv.net.ListenerPhase
import io.github.mohithdas.opendisplay.tv.net.PhoneReceiver
import io.github.mohithdas.opendisplay.tv.service.ReceiverService
import io.github.mohithdas.opendisplay.tv.settings.DisplayProfileProvider
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class StartupRegressionTest {
    private var receiver: PhoneReceiver? = null
    private var service: ReceiverService? = null

    @After
    fun cleanUp() {
        receiver?.stop()
        service?.onDestroy()
    }

    @Test
    fun receiverConstructsWithApplicationContextOnApi30() {
        val application = ApplicationProvider.getApplicationContext<Application>()

        receiver = PhoneReceiver(application)

        assertNotNull(receiver)
        assertTrue(receiver!!.displaySelection.value.pixels.width >= 2)
    }

    @Test
    fun serviceStartsReceiverBeforeAnyActivityExists() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val controller = Robolectric.buildService(
            ReceiverService::class.java,
            Intent(application, ReceiverService::class.java),
        ).create().startCommand(0, 1)
        service = controller.get()

        assertNotNull(service!!.receiver)
        assertTrue(
            service!!.receiver.listenerState.value.phase in setOf(
                ListenerPhase.STARTING,
                ListenerPhase.WAITING_FOR_NETWORK,
                ListenerPhase.LISTENING,
                ListenerPhase.RETRYING,
                ListenerPhase.PORTS_UNAVAILABLE,
            ),
        )
    }

    @Test
    fun activityVisualDetectionIsSafeOnApi30() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()

        val profile = DisplayProfileProvider.detectActivity(activity)

        assertTrue(profile.physicalMode.width >= 2)
        assertTrue(profile.windowBounds.height >= 2)
        activity.finish()
    }
}
