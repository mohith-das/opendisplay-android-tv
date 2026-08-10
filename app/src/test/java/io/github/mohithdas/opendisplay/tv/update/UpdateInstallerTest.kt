package io.github.mohithdas.opendisplay.tv.update

import android.content.Intent
import android.provider.Settings
import androidx.activity.ComponentActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class UpdateInstallerTest {
    @Test
    fun missingUnknownSourcesPermissionOpensAppSpecificSettingsFromActivity() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        shadowOf(activity.packageManager).setCanRequestPackageInstalls(false)
        val apk = File(activity.filesDir, "candidate.apk").apply { writeText("test") }

        val result = UpdateInstaller().launch(activity, apk, streaming = false)

        assertEquals(InstallLaunchResult.PERMISSION_REQUIRED, result)
        val intent = shadowOf(activity).nextStartedActivity
        assertEquals(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, intent.action)
        assertEquals("package:${activity.packageName}", intent.data.toString())
    }

    @Test
    fun grantedUnknownSourcesPermissionOpensAndroidPackageInstaller() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        shadowOf(activity.packageManager).setCanRequestPackageInstalls(true)
        val apk = File(activity.filesDir, "updates/OpenDisplay-TV.apk").apply {
            parentFile?.mkdirs()
            writeText("test")
        }

        val result = UpdateInstaller().launch(activity, apk, streaming = false)

        assertEquals(InstallLaunchResult.LAUNCHED, result)
        val intent = shadowOf(activity).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("application/vnd.android.package-archive", intent.type)
        assertEquals("content", intent.data?.scheme)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }
}
