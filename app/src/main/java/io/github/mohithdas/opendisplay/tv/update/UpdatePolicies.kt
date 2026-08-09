package io.github.mohithdas.opendisplay.tv.update

object VersionPolicy {
    private val VERSION_PATTERN = Regex("^[0-9]+(?:\\.[0-9]+){1,3}$")

    fun isReleaseVersion(value: String): Boolean = VERSION_PATTERN.matches(value)

    fun compare(left: String, right: String): Int {
        val leftParts = left.removePrefix("v").split('.').mapNotNull(String::toIntOrNull)
        val rightParts = right.removePrefix("v").split('.').mapNotNull(String::toIntOrNull)
        val size = maxOf(leftParts.size, rightParts.size)
        for (index in 0 until size) {
            val comparison = (leftParts.getOrNull(index) ?: 0)
                .compareTo(rightParts.getOrNull(index) ?: 0)
            if (comparison != 0) return comparison
        }
        return 0
    }

    fun isNewer(candidate: String, installed: String): Boolean = compare(candidate, installed) > 0
}

object ChecksumPolicy {
    private val LINE = Regex("^([a-fA-F0-9]{64})[ \\t]+\\*?OpenDisplay-TV\\.apk(?:\\r?\\n)?$")
    private val DIGEST = Regex("^sha256:([a-fA-F0-9]{64})$")

    fun parseChecksumFile(value: String): String = LINE.matchEntire(value.trimEnd())
        ?.groupValues?.get(1)?.lowercase()
        ?: throw UpdateException(UpdateError.INVALID_CHECKSUM)

    fun parseGithubDigest(value: String?): String? {
        if (value.isNullOrBlank()) return null
        return DIGEST.matchEntire(value)?.groupValues?.get(1)?.lowercase()
            ?: throw UpdateException(UpdateError.GITHUB_DIGEST_MISMATCH)
    }

    fun validateDownloaded(expected: String, actual: String, githubDigest: String?) {
        if (actual.lowercase() != expected.lowercase()) {
            throw UpdateException(UpdateError.CHECKSUM_MISMATCH)
        }
        parseGithubDigest(githubDigest)?.let { digest ->
            if (actual.lowercase() != digest) {
                throw UpdateException(UpdateError.GITHUB_DIGEST_MISMATCH)
            }
        }
    }
}

object UpdateFilePolicy {
    fun reject(vararg files: java.io.File) {
        files.forEach { file -> if (file.exists() && !file.delete()) file.deleteOnExit() }
    }
}

object PackageSecurityPolicy {
    fun validate(
        installed: PackageEvidence,
        candidate: PackageEvidence,
        release: StableRelease,
    ): PackageValidation {
        if (candidate.packageName != EXPECTED_PACKAGE_ID) {
            return PackageValidation.Invalid(UpdateError.WRONG_PACKAGE)
        }
        if (candidate.versionCode <= installed.versionCode) {
            return PackageValidation.Invalid(UpdateError.VERSION_NOT_NEWER)
        }
        if (candidate.versionName != release.versionName || release.tagName != "v${candidate.versionName}") {
            return PackageValidation.Invalid(UpdateError.VERSION_MISMATCH)
        }
        val expected = PRODUCTION_CERTIFICATE_SHA256.lowercase()
        val installedAll = installed.allSignerSha256.map(String::lowercase).toSet()
        val candidateAll = candidate.allSignerSha256.map(String::lowercase).toSet()
        val installedCurrent = installed.currentSignerSha256.map(String::lowercase).toSet()
        val candidateCurrent = candidate.currentSignerSha256.map(String::lowercase).toSet()
        val validLineage = expected in installedAll && expected in candidateAll &&
            (candidateCurrent.any { it in installedAll } || installedCurrent.any { it in candidateAll })
        return if (validLineage) PackageValidation.Valid
        else PackageValidation.Invalid(UpdateError.WRONG_SIGNATURE)
    }
}

object UpdatePolicy {
    const val CHECK_INTERVAL_MS = 24L * 60L * 60L * 1_000L

    fun shouldCheck(
        manual: Boolean,
        automaticallyCheck: Boolean,
        lastSuccessMillis: Long?,
        nowMillis: Long,
    ): Boolean = manual || (
        automaticallyCheck &&
            (lastSuccessMillis == null || nowMillis - lastSuccessMillis >= CHECK_INTERVAL_MS)
        )

    fun shouldOfferVersion(version: String, skippedVersion: String?, remindedVersion: String?): Boolean =
        version != skippedVersion && version != remindedVersion

    fun shouldDownload(streaming: Boolean, autoDownload: Boolean, unmetered: Boolean): Boolean =
        !streaming && autoDownload && unmetered

    fun canInstall(streaming: Boolean, verified: Boolean): Boolean = !streaming && verified
}
