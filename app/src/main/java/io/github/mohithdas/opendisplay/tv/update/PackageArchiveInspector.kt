package io.github.mohithdas.opendisplay.tv.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import java.io.File
import java.security.MessageDigest

interface PackageInspector {
    fun installed(): PackageEvidence
    fun archive(file: File): PackageEvidence
}

class AndroidPackageInspector(context: Context) : PackageInspector {
    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager

    override fun installed(): PackageEvidence = evidence(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(
                appContext.packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(appContext.packageName, signingFlag())
        },
    )

    override fun archive(file: File): PackageEvidence {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageArchiveInfo(
                file.absolutePath,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageArchiveInfo(file.absolutePath, signingFlag())
        } ?: throw UpdateException(UpdateError.WRONG_PACKAGE)
        return evidence(info)
    }

    private fun evidence(info: PackageInfo): PackageEvidence {
        val current: Set<Signature>
        val lineage: Set<Signature>
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = info.signingInfo ?: throw UpdateException(UpdateError.WRONG_SIGNATURE)
            current = signingInfo.apkContentsSigners.orEmpty().toSet()
            lineage = try {
                signingInfo.signingCertificateHistory.orEmpty().toSet()
            } catch (_: Exception) {
                current
            }
        } else {
            @Suppress("DEPRECATION")
            current = info.signatures.orEmpty().toSet()
            lineage = current
        }
        return PackageEvidence(
            packageName = info.packageName,
            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode
            else @Suppress("DEPRECATION") info.versionCode.toLong(),
            versionName = info.versionName.orEmpty(),
            currentSignerSha256 = current.mapTo(mutableSetOf(), ::fingerprint),
            signingLineageSha256 = lineage.mapTo(mutableSetOf(), ::fingerprint),
        )
    }

    private fun signingFlag(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        PackageManager.GET_SIGNING_CERTIFICATES
    } else {
        @Suppress("DEPRECATION")
        PackageManager.GET_SIGNATURES
    }

    private fun fingerprint(signature: Signature): String = MessageDigest.getInstance("SHA-256")
        .digest(signature.toByteArray())
        .joinToString("") { byte -> "%02x".format(byte) }
}
