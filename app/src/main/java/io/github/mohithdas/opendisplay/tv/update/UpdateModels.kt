package io.github.mohithdas.opendisplay.tv.update

import java.io.File

const val RELEASE_API_URL =
    "https://api.github.com/repos/mohith-das/opendisplay-android-tv/releases/latest"
const val APK_ASSET_NAME = "OpenDisplay-TV.apk"
const val CHECKSUM_ASSET_NAME = "OpenDisplay-TV.apk.sha256"
const val EXPECTED_PACKAGE_ID = "io.github.mohithdas.opendisplay.tv"
const val PRODUCTION_CERTIFICATE_SHA256 =
    "4cceafa2b86d9602b4b02429df74fffb0ed165ce9ae423e5105b85dd048a1943"

data class ReleaseAsset(
    val name: String,
    val url: String,
    val digest: String?,
)

data class StableRelease(
    val tagName: String,
    val versionName: String,
    val notes: String,
    val apk: ReleaseAsset,
    val checksum: ReleaseAsset,
)

enum class UpdateError {
    OFFLINE,
    TIMEOUT,
    RATE_LIMITED,
    SERVER,
    MALFORMED_RESPONSE,
    MISSING_APK,
    MISSING_CHECKSUM,
    INVALID_CHECKSUM,
    CHECKSUM_MISMATCH,
    GITHUB_DIGEST_MISMATCH,
    INVALID_REDIRECT,
    WRONG_PACKAGE,
    VERSION_NOT_NEWER,
    VERSION_MISMATCH,
    WRONG_SIGNATURE,
    STORAGE,
    UNKNOWN,
}

enum class UpdatePhase {
    IDLE,
    CHECKING,
    UP_TO_DATE,
    AVAILABLE,
    DOWNLOADING,
    READY,
    DEFERRED_FOR_STREAM,
    ERROR,
}

data class UpdateUiState(
    val phase: UpdatePhase = UpdatePhase.IDLE,
    val installedVersion: String,
    val latestVersion: String? = null,
    val lastSuccessfulCheckMillis: Long? = null,
    val automaticallyCheck: Boolean = true,
    val autoDownloadUnmetered: Boolean = false,
    val progressPercent: Int? = null,
    val releaseNotes: String = "",
    val error: UpdateError? = null,
    val verifiedApk: File? = null,
    val streaming: Boolean = false,
)

data class DownloadEvidence(
    val file: File,
    val sha256: String,
)

data class PackageEvidence(
    val packageName: String,
    val versionCode: Long,
    val versionName: String,
    val currentSignerSha256: Set<String>,
    val signingLineageSha256: Set<String>,
) {
    val allSignerSha256: Set<String> get() = currentSignerSha256 + signingLineageSha256
}

sealed interface PackageValidation {
    data object Valid : PackageValidation
    data class Invalid(val error: UpdateError) : PackageValidation
}

class UpdateException(val reason: UpdateError, cause: Throwable? = null) : Exception(reason.name, cause)
