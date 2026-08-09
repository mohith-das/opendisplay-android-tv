package io.github.mohithdas.opendisplay.tv.update

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UpdatePreferencesTest {
    private lateinit var application: Application

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        application.getSharedPreferences("opendisplay_tv_updates", 0).edit().clear().commit()
    }

    @Test
    fun defaultsAndValuesPersistAcrossInstances() {
        val first = UpdatePreferences(application)
        assertTrue(first.automaticallyCheck)
        assertFalse(first.autoDownloadUnmetered)
        assertNull(first.lastSuccessfulCheckMillis)

        first.automaticallyCheck = false
        first.autoDownloadUnmetered = true
        first.lastSuccessfulCheckMillis = 123L
        first.skippedVersion = "0.1.2"

        val second = UpdatePreferences(application)
        assertFalse(second.automaticallyCheck)
        assertTrue(second.autoDownloadUnmetered)
        assertEquals(123L, second.lastSuccessfulCheckMillis)
        assertEquals("0.1.2", second.skippedVersion)
    }

    @Test
    fun availableReleaseSurvivesBackgroundProcessRestart() {
        val release = StableRelease(
            tagName = "v0.1.2",
            versionName = "0.1.2",
            notes = "Security update",
            apk = ReleaseAsset(APK_ASSET_NAME, "https://github.com/apk", "sha256:${"a".repeat(64)}"),
            checksum = ReleaseAsset(CHECKSUM_ASSET_NAME, "https://github.com/sum", null),
        )
        UpdatePreferences(application).saveAvailable(release)

        assertEquals(release, UpdatePreferences(application).loadAvailable())
    }
}
