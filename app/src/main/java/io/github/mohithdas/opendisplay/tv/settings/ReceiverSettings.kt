package io.github.mohithdas.opendisplay.tv.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ResolutionChoice { AUTO, FULL_HD, HD }

enum class UiScaleChoice { AUTO, ONE, TWO }

enum class FitMode { FIT, FILL, STRETCH, NATIVE }

enum class KeepAwakePolicy { WHILE_CONNECTED, WHILE_OPEN, NEVER }

data class ReceiverSettings(
    val resolution: ResolutionChoice = ResolutionChoice.AUTO,
    val uiScale: UiScaleChoice = UiScaleChoice.AUTO,
    val fitMode: FitMode = FitMode.FIT,
    val keepAwakePolicy: KeepAwakePolicy,
    val performanceOverlay: Boolean = false,
)

internal interface SettingsStorage {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
}

internal class SharedPreferencesStorage(private val preferences: SharedPreferences) : SettingsStorage {
    override fun getString(key: String): String? = preferences.getString(key, null)

    override fun putString(key: String, value: String) {
        preferences.edit { putString(key, value) }
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        preferences.getBoolean(key, defaultValue)

    override fun putBoolean(key: String, value: Boolean) {
        preferences.edit { putBoolean(key, value) }
    }
}

internal class ReceiverSettingsRepository(
    private val storage: SettingsStorage,
    isTelevision: Boolean,
) {
    private val defaultAwakePolicy = if (isTelevision) {
        KeepAwakePolicy.WHILE_OPEN
    } else {
        KeepAwakePolicy.WHILE_CONNECTED
    }

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<ReceiverSettings> = _settings.asStateFlow()

    fun update(value: ReceiverSettings) {
        storage.putString(KEY_RESOLUTION, value.resolution.name)
        storage.putString(KEY_UI_SCALE, value.uiScale.name)
        storage.putString(KEY_FIT_MODE, value.fitMode.name)
        storage.putString(KEY_KEEP_AWAKE, value.keepAwakePolicy.name)
        storage.putBoolean(KEY_PERFORMANCE_OVERLAY, value.performanceOverlay)
        _settings.value = value
    }

    private fun load(): ReceiverSettings = ReceiverSettings(
        resolution = storage.enumValue(KEY_RESOLUTION, ResolutionChoice.AUTO),
        uiScale = storage.enumValue(KEY_UI_SCALE, UiScaleChoice.AUTO),
        fitMode = storage.enumValue(KEY_FIT_MODE, FitMode.FIT),
        keepAwakePolicy = storage.enumValue(KEY_KEEP_AWAKE, defaultAwakePolicy),
        performanceOverlay = storage.getBoolean(KEY_PERFORMANCE_OVERLAY, false),
    )

    private inline fun <reified T : Enum<T>> SettingsStorage.enumValue(key: String, fallback: T): T =
        getString(key)?.let { stored -> enumValues<T>().firstOrNull { it.name == stored } } ?: fallback

    private companion object {
        const val KEY_RESOLUTION = "resolution"
        const val KEY_UI_SCALE = "uiScale"
        const val KEY_FIT_MODE = "fitMode"
        const val KEY_KEEP_AWAKE = "keepAwake"
        const val KEY_PERFORMANCE_OVERLAY = "performanceOverlay"
    }
}
