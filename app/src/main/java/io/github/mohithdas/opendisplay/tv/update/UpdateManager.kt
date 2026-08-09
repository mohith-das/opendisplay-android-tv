package io.github.mohithdas.opendisplay.tv.update

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.mohithdas.opendisplay.tv.BuildConfig
import io.github.mohithdas.opendisplay.tv.MainActivity
import io.github.mohithdas.opendisplay.tv.R
import io.github.mohithdas.opendisplay.tv.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.security.MessageDigest

class UpdateManager private constructor(
    context: Context,
    private val transport: GithubTransport = UrlConnectionGithubTransport(),
    private val releaseSource: ReleaseSource = GithubReleaseClient(transport),
    private val packageInspector: PackageInspector = AndroidPackageInspector(context),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val appContext = context.applicationContext
    private val preferences = UpdatePreferences(appContext)
    private val updateDirectory = File(appContext.filesDir, "updates")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val operationMutex = Mutex()
    private val installer = UpdateInstaller()
    private var currentRelease: StableRelease? = null
    private var pendingDownload = false

    private val _state = MutableStateFlow(
        UpdateUiState(
            installedVersion = BuildConfig.VERSION_NAME,
            lastSuccessfulCheckMillis = preferences.lastSuccessfulCheckMillis,
            automaticallyCheck = preferences.automaticallyCheck,
            autoDownloadUnmetered = preferences.autoDownloadUnmetered,
        ),
    )
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    init {
        cleanIncompleteAndRestoreVerified()
        restoreAvailableRelease()
    }

    fun initialize() {
        try {
            UpdateScheduler.schedule(appContext)
        } catch (failure: Exception) {
            Log.warn("daily update scheduling unavailable; foreground checks remain available", failure)
        }
        checkForUpdates(manual = false)
    }

    fun checkForUpdates(manual: Boolean = true) {
        scope.launch { performCheck(manual = manual, background = false) }
    }

    suspend fun backgroundCheck(): UpdateError? = performCheck(manual = false, background = true)

    fun requestDownload() {
        val release = currentRelease ?: return
        preferences.allowVersion(release.versionName)
        if (_state.value.streaming) {
            pendingDownload = true
            _state.value = _state.value.copy(phase = UpdatePhase.DEFERRED_FOR_STREAM)
            return
        }
        scope.launch { operationMutex.withLock { downloadAndValidate(release, background = false) } }
    }

    fun setStreaming(streaming: Boolean) {
        _state.value = _state.value.copy(streaming = streaming)
        if (!streaming && pendingDownload) {
            pendingDownload = false
            requestDownload()
        }
    }

    fun setAutomaticallyCheck(enabled: Boolean) {
        preferences.automaticallyCheck = enabled
        _state.value = _state.value.copy(automaticallyCheck = enabled)
    }

    fun setAutoDownloadUnmetered(enabled: Boolean) {
        preferences.autoDownloadUnmetered = enabled
        _state.value = _state.value.copy(autoDownloadUnmetered = enabled)
        val release = currentRelease
        if (enabled && release != null && UpdatePolicy.shouldDownload(
                streaming = _state.value.streaming,
                autoDownload = true,
                unmetered = isUnmetered(),
            )
        ) {
            requestDownload()
        }
    }

    fun remindLater() {
        _state.value.latestVersion?.let { preferences.remindedVersion = it }
        abandonDownloadedApk()
        _state.value = _state.value.copy(
            phase = UpdatePhase.IDLE,
            latestVersion = null,
            releaseNotes = "",
            verifiedApk = null,
            error = null,
        )
    }

    fun skipVersion() {
        _state.value.latestVersion?.let { preferences.skippedVersion = it }
        abandonDownloadedApk()
        _state.value = _state.value.copy(
            phase = UpdatePhase.IDLE,
            latestVersion = null,
            releaseNotes = "",
            verifiedApk = null,
            error = null,
        )
    }

    fun launchInstaller(activity: Activity): InstallLaunchResult {
        val snapshot = _state.value
        val apk = snapshot.verifiedApk ?: return InstallLaunchResult.FILE_MISSING
        return installer.launch(activity, apk, snapshot.streaming)
    }

    private suspend fun performCheck(manual: Boolean, background: Boolean): UpdateError? =
        operationMutex.withLock {
            val snapshot = _state.value
            if (!UpdatePolicy.shouldCheck(
                    manual = manual,
                    automaticallyCheck = snapshot.automaticallyCheck,
                    lastSuccessMillis = preferences.lastSuccessfulCheckMillis,
                    nowMillis = clock(),
                )
            ) return@withLock null

            _state.value = snapshot.copy(phase = UpdatePhase.CHECKING, error = null)
            try {
                val release = releaseSource.latestStable()
                currentRelease = release
                val now = clock()
                preferences.lastSuccessfulCheckMillis = now
                if (!VersionPolicy.isNewer(release.versionName, BuildConfig.VERSION_NAME)) {
                    preferences.clearAvailable()
                    _state.value = _state.value.copy(
                        phase = UpdatePhase.UP_TO_DATE,
                        latestVersion = release.versionName,
                        lastSuccessfulCheckMillis = now,
                        releaseNotes = release.notes,
                    )
                    return@withLock null
                }
                if (!manual && !UpdatePolicy.shouldOfferVersion(
                        release.versionName,
                        preferences.skippedVersion,
                        preferences.remindedVersion,
                    )
                ) {
                    _state.value = _state.value.copy(
                        phase = UpdatePhase.IDLE,
                        latestVersion = null,
                        lastSuccessfulCheckMillis = now,
                        releaseNotes = "",
                    )
                    return@withLock null
                }
                _state.value = _state.value.copy(
                    phase = UpdatePhase.AVAILABLE,
                    latestVersion = release.versionName,
                    lastSuccessfulCheckMillis = now,
                    releaseNotes = release.notes,
                    error = null,
                )
                preferences.saveAvailable(release)
                if (UpdatePolicy.shouldDownload(
                        streaming = _state.value.streaming,
                        autoDownload = _state.value.autoDownloadUnmetered,
                        unmetered = isUnmetered(),
                    )
                ) {
                    return@withLock downloadAndValidate(release, background)
                }
                null
            } catch (failure: UpdateException) {
                Log.warn("update check failed: ${failure.reason}", failure)
                _state.value = _state.value.copy(phase = UpdatePhase.ERROR, error = failure.reason)
                failure.reason
            } catch (failure: Exception) {
                Log.warn("update check failed", failure)
                _state.value = _state.value.copy(phase = UpdatePhase.ERROR, error = UpdateError.UNKNOWN)
                UpdateError.UNKNOWN
            }
        }

    private suspend fun downloadAndValidate(release: StableRelease, background: Boolean): UpdateError? {
        if (_state.value.streaming) {
            pendingDownload = true
            _state.value = _state.value.copy(phase = UpdatePhase.DEFERRED_FOR_STREAM)
            return null
        }
        val partial = File(updateDirectory, "$APK_ASSET_NAME.partial")
        val verified = File(updateDirectory, APK_ASSET_NAME)
        try {
            updateDirectory.mkdirs()
            partial.delete()
            _state.value = _state.value.copy(
                phase = UpdatePhase.DOWNLOADING,
                progressPercent = 0,
                error = null,
            )
            val checksumResponse = transport.getText(release.checksum.url, MAX_CHECKSUM_BYTES)
            if (checksumResponse.status !in 200..299) throw UpdateException(UpdateError.SERVER)
            val expectedChecksum = ChecksumPolicy.parseChecksumFile(checksumResponse.body)
            ChecksumPolicy.parseGithubDigest(release.checksum.digest)?.let { githubDigest ->
                val actual = MessageDigest.getInstance("SHA-256")
                    .digest(checksumResponse.body.toByteArray(Charsets.UTF_8)).toHex()
                if (actual != githubDigest) throw UpdateException(UpdateError.GITHUB_DIGEST_MISMATCH)
            }
            val download = transport.download(
                release.apk.url,
                partial,
                MAX_APK_BYTES,
            ) { progress -> _state.value = _state.value.copy(progressPercent = progress) }
            if (!UpdatePolicy.shouldOfferVersion(
                    release.versionName,
                    preferences.skippedVersion,
                    preferences.remindedVersion,
                )
            ) {
                UpdateFilePolicy.reject(partial)
                return null
            }
            ChecksumPolicy.validateDownloaded(
                expected = expectedChecksum,
                actual = download.sha256,
                githubDigest = release.apk.digest,
            )
            val validation = PackageSecurityPolicy.validate(
                installed = packageInspector.installed(),
                candidate = packageInspector.archive(partial),
                release = release,
            )
            if (validation is PackageValidation.Invalid) throw UpdateException(validation.error)
            verified.delete()
            if (!partial.renameTo(verified)) throw UpdateException(UpdateError.STORAGE)
            preferences.verifiedVersion = release.versionName
            preferences.verifiedPath = verified.absolutePath
            _state.value = _state.value.copy(
                phase = UpdatePhase.READY,
                progressPercent = 100,
                verifiedApk = verified,
                error = null,
            )
            if (background) showReadyNotification(release.versionName)
            return null
        } catch (failure: UpdateException) {
            UpdateFilePolicy.reject(partial, verified)
            preferences.clearVerified()
            Log.warn("update download rejected: ${failure.reason}", failure)
            _state.value = _state.value.copy(
                phase = UpdatePhase.ERROR,
                progressPercent = null,
                verifiedApk = null,
                error = failure.reason,
            )
            return failure.reason
        } catch (failure: Exception) {
            UpdateFilePolicy.reject(partial, verified)
            preferences.clearVerified()
            Log.warn("update download failed", failure)
            _state.value = _state.value.copy(
                phase = UpdatePhase.ERROR,
                progressPercent = null,
                verifiedApk = null,
                error = UpdateError.UNKNOWN,
            )
            return UpdateError.UNKNOWN
        }
    }

    private fun cleanIncompleteAndRestoreVerified() {
        try {
            updateDirectory.mkdirs()
            updateDirectory.listFiles { file -> file.name.endsWith(".partial") }
                .orEmpty().forEach(File::delete)
            val path = preferences.verifiedPath
            val version = preferences.verifiedVersion
            val file = path?.let(::File)
            val safe = file?.name == APK_ASSET_NAME &&
                file.parentFile?.canonicalFile == updateDirectory.canonicalFile &&
                file.isFile && version != null && VersionPolicy.isNewer(version, BuildConfig.VERSION_NAME)
            if (safe) {
                _state.value = _state.value.copy(
                    phase = UpdatePhase.READY,
                    latestVersion = version,
                    verifiedApk = file,
                    progressPercent = 100,
                )
            } else {
                file?.takeIf { it.parentFile?.canonicalFile == updateDirectory.canonicalFile }?.delete()
                preferences.clearVerified()
            }
        } catch (failure: Exception) {
            Log.warn("could not restore a pending update; discarding it safely", failure)
            updateDirectory.listFiles { file ->
                file.name == APK_ASSET_NAME || file.name.endsWith(".partial")
            }.orEmpty().forEach(File::delete)
            preferences.clearVerified()
        }
    }

    private fun abandonDownloadedApk() {
        _state.value.verifiedApk?.delete()
        File(updateDirectory, APK_ASSET_NAME).delete()
        File(updateDirectory, "$APK_ASSET_NAME.partial").delete()
        preferences.clearVerified()
        preferences.clearAvailable()
        pendingDownload = false
    }

    private fun restoreAvailableRelease() {
        if (_state.value.phase == UpdatePhase.READY) return
        val release = preferences.loadAvailable() ?: return
        val canOffer = VersionPolicy.isNewer(release.versionName, BuildConfig.VERSION_NAME) &&
            UpdatePolicy.shouldOfferVersion(
                release.versionName,
                preferences.skippedVersion,
                preferences.remindedVersion,
            )
        if (!canOffer) {
            preferences.clearAvailable()
            return
        }
        currentRelease = release
        _state.value = _state.value.copy(
            phase = UpdatePhase.AVAILABLE,
            latestVersion = release.versionName,
            releaseNotes = release.notes,
        )
    }

    private fun isUnmetered(): Boolean = try {
        val manager = appContext.getSystemService(ConnectivityManager::class.java)
        manager?.activeNetwork != null && !manager.isActiveNetworkMetered
    } catch (_: Exception) {
        false
    }

    private fun showReadyNotification(version: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return
        if (!NotificationManagerCompat.from(appContext).areNotificationsEnabled()) return
        val manager = appContext.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                UPDATE_CHANNEL_ID,
                appContext.getString(R.string.update_notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            2,
            Intent(appContext, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        manager.notify(
            UPDATE_NOTIFICATION_ID,
            NotificationCompat.Builder(appContext, UPDATE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(appContext.getString(R.string.update_notification_title))
                .setContentText(appContext.getString(R.string.update_notification_body, version))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build(),
        )
    }

    private fun ByteArray.toHex(): String = joinToString("") { byte -> "%02x".format(byte) }

    companion object {
        private const val MAX_CHECKSUM_BYTES = 1_024L
        private const val MAX_APK_BYTES = 512L * 1024L * 1024L
        private const val UPDATE_CHANNEL_ID = "opendisplay_tv_updates"
        private const val UPDATE_NOTIFICATION_ID = 2

        @Volatile private var instance: UpdateManager? = null

        fun get(context: Context): UpdateManager = instance ?: synchronized(this) {
            instance ?: UpdateManager(context).also { instance = it }
        }
    }
}
