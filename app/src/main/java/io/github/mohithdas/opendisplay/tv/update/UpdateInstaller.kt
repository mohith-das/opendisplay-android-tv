package io.github.mohithdas.opendisplay.tv.update

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

enum class InstallLaunchResult { LAUNCHED, PERMISSION_REQUIRED, DEFERRED_STREAMING, FILE_MISSING }

class UpdateInstaller {
    fun launch(activity: Activity, apk: File, streaming: Boolean): InstallLaunchResult {
        if (streaming) return InstallLaunchResult.DEFERRED_STREAMING
        if (!apk.isFile) return InstallLaunchResult.FILE_MISSING
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !activity.packageManager.canRequestPackageInstalls()
        ) {
            activity.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${activity.packageName}"),
                ),
            )
            return InstallLaunchResult.PERMISSION_REQUIRED
        }
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.updates", apk)
        activity.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
        )
        return InstallLaunchResult.LAUNCHED
    }
}
