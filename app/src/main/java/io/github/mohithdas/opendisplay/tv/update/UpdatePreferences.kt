package io.github.mohithdas.opendisplay.tv.update

import android.content.Context

class UpdatePreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "opendisplay_tv_updates",
        Context.MODE_PRIVATE,
    )

    var automaticallyCheck: Boolean
        get() = preferences.getBoolean(KEY_AUTOMATIC_CHECK, true)
        set(value) = preferences.edit().putBoolean(KEY_AUTOMATIC_CHECK, value).apply()

    var autoDownloadUnmetered: Boolean
        get() = preferences.getBoolean(KEY_AUTO_DOWNLOAD, false)
        set(value) = preferences.edit().putBoolean(KEY_AUTO_DOWNLOAD, value).apply()

    var lastSuccessfulCheckMillis: Long?
        get() = preferences.getLong(KEY_LAST_SUCCESS, -1L).takeIf { it >= 0L }
        set(value) = preferences.edit().apply {
            if (value == null) remove(KEY_LAST_SUCCESS) else putLong(KEY_LAST_SUCCESS, value)
        }.apply()

    var skippedVersion: String?
        get() = preferences.getString(KEY_SKIPPED_VERSION, null)
        set(value) = preferences.edit().putString(KEY_SKIPPED_VERSION, value).apply()

    var remindedVersion: String?
        get() = preferences.getString(KEY_REMINDED_VERSION, null)
        set(value) = preferences.edit().putString(KEY_REMINDED_VERSION, value).apply()

    var verifiedVersion: String?
        get() = preferences.getString(KEY_VERIFIED_VERSION, null)
        set(value) = preferences.edit().putString(KEY_VERIFIED_VERSION, value).apply()

    var verifiedPath: String?
        get() = preferences.getString(KEY_VERIFIED_PATH, null)
        set(value) = preferences.edit().putString(KEY_VERIFIED_PATH, value).apply()

    fun clearVerified() {
        preferences.edit().remove(KEY_VERIFIED_VERSION).remove(KEY_VERIFIED_PATH).apply()
    }

    fun saveAvailable(release: StableRelease) {
        preferences.edit()
            .putString(KEY_AVAILABLE_TAG, release.tagName)
            .putString(KEY_AVAILABLE_NOTES, release.notes)
            .putString(KEY_AVAILABLE_APK_URL, release.apk.url)
            .putString(KEY_AVAILABLE_APK_DIGEST, release.apk.digest)
            .putString(KEY_AVAILABLE_CHECKSUM_URL, release.checksum.url)
            .putString(KEY_AVAILABLE_CHECKSUM_DIGEST, release.checksum.digest)
            .apply()
    }

    fun loadAvailable(): StableRelease? {
        val tag = preferences.getString(KEY_AVAILABLE_TAG, null) ?: return null
        val apkUrl = preferences.getString(KEY_AVAILABLE_APK_URL, null) ?: return null
        val checksumUrl = preferences.getString(KEY_AVAILABLE_CHECKSUM_URL, null) ?: return null
        val version = tag.removePrefix("v")
        if (!VersionPolicy.isReleaseVersion(version)) return null
        return StableRelease(
            tagName = tag,
            versionName = version,
            notes = preferences.getString(KEY_AVAILABLE_NOTES, "").orEmpty().take(16_384),
            apk = ReleaseAsset(
                APK_ASSET_NAME,
                apkUrl,
                preferences.getString(KEY_AVAILABLE_APK_DIGEST, null),
            ),
            checksum = ReleaseAsset(
                CHECKSUM_ASSET_NAME,
                checksumUrl,
                preferences.getString(KEY_AVAILABLE_CHECKSUM_DIGEST, null),
            ),
        )
    }

    fun clearAvailable() {
        preferences.edit()
            .remove(KEY_AVAILABLE_TAG)
            .remove(KEY_AVAILABLE_NOTES)
            .remove(KEY_AVAILABLE_APK_URL)
            .remove(KEY_AVAILABLE_APK_DIGEST)
            .remove(KEY_AVAILABLE_CHECKSUM_URL)
            .remove(KEY_AVAILABLE_CHECKSUM_DIGEST)
            .apply()
    }

    fun allowVersion(version: String) {
        preferences.edit().apply {
            if (this@UpdatePreferences.skippedVersion == version) remove(KEY_SKIPPED_VERSION)
            if (this@UpdatePreferences.remindedVersion == version) remove(KEY_REMINDED_VERSION)
        }.apply()
    }

    private companion object {
        const val KEY_AUTOMATIC_CHECK = "automatic_check"
        const val KEY_AUTO_DOWNLOAD = "auto_download_unmetered"
        const val KEY_LAST_SUCCESS = "last_success"
        const val KEY_SKIPPED_VERSION = "skipped_version"
        const val KEY_REMINDED_VERSION = "reminded_version"
        const val KEY_VERIFIED_VERSION = "verified_version"
        const val KEY_VERIFIED_PATH = "verified_path"
        const val KEY_AVAILABLE_TAG = "available_tag"
        const val KEY_AVAILABLE_NOTES = "available_notes"
        const val KEY_AVAILABLE_APK_URL = "available_apk_url"
        const val KEY_AVAILABLE_APK_DIGEST = "available_apk_digest"
        const val KEY_AVAILABLE_CHECKSUM_URL = "available_checksum_url"
        const val KEY_AVAILABLE_CHECKSUM_DIGEST = "available_checksum_digest"
    }
}
