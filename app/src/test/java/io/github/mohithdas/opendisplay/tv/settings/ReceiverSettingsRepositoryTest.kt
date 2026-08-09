package io.github.mohithdas.opendisplay.tv.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ReceiverSettingsRepositoryTest {
    @Test
    fun televisionAndHandheldDefaultsDifferOnlyForAwakePolicy() {
        val television = ReceiverSettingsRepository(MemoryStorage(), true).settings.value
        val handheld = ReceiverSettingsRepository(MemoryStorage(), false).settings.value

        assertEquals(ResolutionChoice.AUTO, television.resolution)
        assertEquals(FitMode.FIT, television.fitMode)
        assertFalse(television.performanceOverlay)
        assertEquals(KeepAwakePolicy.WHILE_OPEN, television.keepAwakePolicy)
        assertEquals(KeepAwakePolicy.WHILE_CONNECTED, handheld.keepAwakePolicy)
    }

    @Test
    fun settingsPersistAcrossRepositoryInstances() {
        val storage = MemoryStorage()
        val first = ReceiverSettingsRepository(storage, true)
        val expected = ReceiverSettings(
            resolution = ResolutionChoice.HD,
            uiScale = UiScaleChoice.TWO,
            fitMode = FitMode.FILL,
            keepAwakePolicy = KeepAwakePolicy.NEVER,
            performanceOverlay = true,
        )
        first.update(expected)

        assertEquals(expected, ReceiverSettingsRepository(storage, false).settings.value)
    }

    @Test
    fun invalidStoredEnumFallsBackSafely() {
        val storage = MemoryStorage().apply { putString("resolution", "NOT_A_MODE") }
        assertEquals(ResolutionChoice.AUTO, ReceiverSettingsRepository(storage, true).settings.value.resolution)
    }

    private class MemoryStorage : SettingsStorage {
        private val strings = mutableMapOf<String, String>()
        private val booleans = mutableMapOf<String, Boolean>()
        override fun getString(key: String): String? = strings[key]
        override fun putString(key: String, value: String) { strings[key] = value }
        override fun getBoolean(key: String, defaultValue: Boolean): Boolean = booleans[key] ?: defaultValue
        override fun putBoolean(key: String, value: Boolean) { booleans[key] = value }
    }
}

