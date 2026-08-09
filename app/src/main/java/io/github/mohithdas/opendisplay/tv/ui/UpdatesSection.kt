package io.github.mohithdas.opendisplay.tv.ui

import android.app.Activity
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.mohithdas.opendisplay.tv.R
import io.github.mohithdas.opendisplay.tv.update.InstallLaunchResult
import io.github.mohithdas.opendisplay.tv.update.UpdateError
import io.github.mohithdas.opendisplay.tv.update.UpdateManager
import io.github.mohithdas.opendisplay.tv.update.UpdatePhase
import java.text.DateFormat
import java.util.Date

@Composable
fun UpdatesSection(manager: UpdateManager) {
    val state by manager.state.collectAsState()
    val context = LocalContext.current
    var installerMessage by remember { mutableStateOf<Int?>(null) }

    Column(
        Modifier.fillMaxWidth().padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.updates), style = MaterialTheme.typography.titleLarge)
        UpdateValue(stringResource(R.string.installed_version), state.installedVersion)
        UpdateValue(
            stringResource(R.string.latest_version),
            state.latestVersion ?: stringResource(R.string.not_checked),
        )
        UpdateValue(
            stringResource(R.string.last_successful_update_check),
            state.lastSuccessfulCheckMillis?.let {
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it))
            } ?: stringResource(R.string.never),
        )

        UpdateToggle(
            label = stringResource(R.string.automatically_check_updates),
            enabled = state.automaticallyCheck,
            onClick = { manager.setAutomaticallyCheck(!state.automaticallyCheck) },
        )
        UpdateToggle(
            label = stringResource(R.string.auto_download_unmetered),
            enabled = state.autoDownloadUnmetered,
            onClick = { manager.setAutoDownloadUnmetered(!state.autoDownloadUnmetered) },
        )
        TvActionButton(
            text = stringResource(
                if (state.phase == UpdatePhase.CHECKING) R.string.checking_for_updates
                else R.string.check_for_updates,
            ),
            onClick = { manager.checkForUpdates(manual = true) },
        )

        when (state.phase) {
            UpdatePhase.UP_TO_DATE -> Text(stringResource(R.string.up_to_date), color = Color(0xFF8BE0B4))
            UpdatePhase.AVAILABLE -> Text(stringResource(R.string.update_available), color = Color(0xFF8BE0B4))
            UpdatePhase.DOWNLOADING -> Text(
                stringResource(R.string.download_progress, state.progressPercent ?: 0),
                color = Color(0xFF8BE0B4),
            )
            UpdatePhase.READY -> Text(stringResource(R.string.update_ready), color = Color(0xFF8BE0B4))
            UpdatePhase.DEFERRED_FOR_STREAM -> Text(
                stringResource(R.string.update_deferred_streaming),
                color = Color(0xFFFFC857),
            )
            UpdatePhase.ERROR -> Text(
                updateErrorText(state.error),
                color = Color(0xFFFF8A80),
            )
            else -> Unit
        }

        if (state.releaseNotes.isNotBlank()) {
            Text(stringResource(R.string.release_notes), style = MaterialTheme.typography.titleMedium)
            Text(state.releaseNotes, color = Color.LightGray)
        }

        if (state.phase in setOf(UpdatePhase.AVAILABLE, UpdatePhase.DEFERRED_FOR_STREAM)) {
            TvActionButton(stringResource(R.string.download_update), manager::requestDownload)
        }
        if (state.phase == UpdatePhase.READY) {
            if (state.streaming) {
                Text(stringResource(R.string.install_after_disconnect), color = Color(0xFFFFC857))
            } else {
                TvActionButton(stringResource(R.string.install_update), onClick = {
                    installerMessage = when (manager.launchInstaller(context as Activity)) {
                        InstallLaunchResult.PERMISSION_REQUIRED -> R.string.install_permission_instructions
                        InstallLaunchResult.DEFERRED_STREAMING -> R.string.install_after_disconnect
                        InstallLaunchResult.FILE_MISSING -> R.string.update_file_missing
                        InstallLaunchResult.LAUNCHED -> R.string.install_confirmation_required
                    }
                })
            }
        }
        installerMessage?.let { Text(stringResource(it), color = Color(0xFFFFC857)) }

        if (state.phase in setOf(
                UpdatePhase.AVAILABLE,
                UpdatePhase.DOWNLOADING,
                UpdatePhase.READY,
                UpdatePhase.DEFERRED_FOR_STREAM,
            )
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TvActionButton(stringResource(R.string.remind_me_later), manager::remindLater)
                TvActionButton(stringResource(R.string.skip_this_version), manager::skipVersion)
            }
        }
        if (state.phase == UpdatePhase.ERROR) {
            TvActionButton(stringResource(R.string.retry), onClick = { manager.checkForUpdates(manual = true) })
        }
        Text(stringResource(R.string.update_install_security_note), color = Color.LightGray)
    }
}

@Composable
private fun UpdateToggle(label: String, enabled: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }.border(
            if (focused) 3.dp else 1.dp,
            if (focused) Color.White else Color(0xFF365248),
            RoundedCornerShape(12.dp),
        ),
        color = if (focused) Color(0xFF235B46) else Color(0xFF13251F),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(if (enabled) R.string.enabled else R.string.disabled),
                color = Color(0xFF8BE0B4),
            )
        }
    }
}

@Composable
private fun UpdateValue(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.LightGray)
        Text(value, modifier = Modifier.padding(start = 24.dp))
    }
}

@Composable
private fun updateErrorText(error: UpdateError?): String = stringResource(
    when (error) {
        UpdateError.OFFLINE -> R.string.update_error_offline
        UpdateError.TIMEOUT -> R.string.update_error_timeout
        UpdateError.RATE_LIMITED -> R.string.update_error_rate_limited
        UpdateError.MISSING_APK, UpdateError.MISSING_CHECKSUM -> R.string.update_error_missing_asset
        UpdateError.INVALID_CHECKSUM, UpdateError.CHECKSUM_MISMATCH,
        UpdateError.GITHUB_DIGEST_MISMATCH -> R.string.update_error_checksum
        UpdateError.INVALID_REDIRECT -> R.string.update_error_redirect
        UpdateError.WRONG_PACKAGE -> R.string.update_error_package
        UpdateError.VERSION_NOT_NEWER, UpdateError.VERSION_MISMATCH -> R.string.update_error_version
        UpdateError.WRONG_SIGNATURE -> R.string.update_error_signature
        UpdateError.STORAGE -> R.string.update_error_storage
        UpdateError.SERVER -> R.string.update_error_server
        else -> R.string.update_error_unknown
    },
)
