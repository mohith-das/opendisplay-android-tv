package io.github.mohithdas.opendisplay.tv.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UpdatePoliciesTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test fun newerVersion() = assertTrue(VersionPolicy.isNewer("0.1.1", "0.1.0"))
    @Test fun sameVersion() = assertFalse(VersionPolicy.isNewer("0.1.1", "0.1.1"))
    @Test fun olderVersion() = assertFalse(VersionPolicy.isNewer("0.1.0", "0.1.1"))

    @Test
    fun malformedChecksumIsRejected() = assertUpdateReason(UpdateError.INVALID_CHECKSUM) {
        ChecksumPolicy.parseChecksumFile("not-a-checksum")
    }

    @Test
    fun checksumMismatchIsRejected() = assertUpdateReason(UpdateError.CHECKSUM_MISMATCH) {
        ChecksumPolicy.validateDownloaded("a".repeat(64), "b".repeat(64), null)
    }

    @Test
    fun githubDigestMismatchIsRejected() = assertUpdateReason(UpdateError.GITHUB_DIGEST_MISMATCH) {
        ChecksumPolicy.validateDownloaded(
            "a".repeat(64),
            "a".repeat(64),
            "sha256:${"b".repeat(64)}",
        )
    }

    @Test
    fun checkIsThrottledForTwentyFourHours() {
        val now = 100_000_000L
        assertFalse(UpdatePolicy.shouldCheck(false, true, now - 1_000L, now))
        assertTrue(UpdatePolicy.shouldCheck(false, true, now - UpdatePolicy.CHECK_INTERVAL_MS, now))
        assertTrue(UpdatePolicy.shouldCheck(true, false, now, now))
    }

    @Test
    fun skippedAndDismissedVersionsAreNotOfferedAutomatically() {
        assertFalse(UpdatePolicy.shouldOfferVersion("0.1.1", "0.1.1", null))
        assertFalse(UpdatePolicy.shouldOfferVersion("0.1.1", null, "0.1.1"))
        assertTrue(UpdatePolicy.shouldOfferVersion("0.1.2", "0.1.1", "0.1.1"))
    }

    @Test
    fun downloadAndInstallAreDeferredWhileStreaming() {
        assertFalse(UpdatePolicy.shouldDownload(streaming = true, autoDownload = true, unmetered = true))
        assertFalse(UpdatePolicy.canInstall(streaming = true, verified = true))
        assertTrue(UpdatePolicy.shouldDownload(streaming = false, autoDownload = true, unmetered = true))
        assertTrue(UpdatePolicy.canInstall(streaming = false, verified = true))
    }

    @Test
    fun rejectedDownloadIsDeleted() {
        val partial = temporaryFolder.newFile("OpenDisplay-TV.apk.partial")
        UpdateFilePolicy.reject(partial)
        assertFalse(partial.exists())
    }

    @Test
    fun packageAndVersionCodeMustMatch() {
        assertInvalid(UpdateError.WRONG_PACKAGE, candidate = evidence(packageName = "other.app"))
        assertInvalid(UpdateError.VERSION_NOT_NEWER, candidate = evidence(versionCode = 1))
        assertInvalid(UpdateError.VERSION_MISMATCH, candidate = evidence(versionName = "0.1.2"))
    }

    @Test
    fun wrongSignerIsRejected() {
        assertInvalid(UpdateError.WRONG_SIGNATURE, candidate = evidence(signer = "f".repeat(64)))
    }

    @Test
    fun matchingProductionSignerIsValid() {
        assertEquals(PackageValidation.Valid, validate(evidence()))
    }

    @Test
    fun validSigningLineageIsAccepted() {
        val rotated = "b".repeat(64)
        val candidate = evidence(signer = rotated, lineage = setOf(PRODUCTION_CERTIFICATE_SHA256, rotated))
        assertEquals(PackageValidation.Valid, validate(candidate))
    }

    private fun assertInvalid(error: UpdateError, candidate: PackageEvidence) {
        assertEquals(PackageValidation.Invalid(error), validate(candidate))
    }

    private fun validate(candidate: PackageEvidence): PackageValidation = PackageSecurityPolicy.validate(
        installed = evidence(versionCode = 1, versionName = "0.1.0"),
        candidate = candidate,
        release = StableRelease(
            tagName = "v0.1.1",
            versionName = "0.1.1",
            notes = "",
            apk = ReleaseAsset(APK_ASSET_NAME, "https://github.com/apk", null),
            checksum = ReleaseAsset(CHECKSUM_ASSET_NAME, "https://github.com/sum", null),
        ),
    )

    private fun evidence(
        packageName: String = EXPECTED_PACKAGE_ID,
        versionCode: Long = 2,
        versionName: String = "0.1.1",
        signer: String = PRODUCTION_CERTIFICATE_SHA256,
        lineage: Set<String> = setOf(signer),
    ) = PackageEvidence(packageName, versionCode, versionName, setOf(signer), lineage)
}

private fun assertUpdateReason(expected: UpdateError, block: () -> Unit) {
    try {
        block()
        fail("Expected $expected")
    } catch (failure: UpdateException) {
        assertEquals(expected, failure.reason)
    }
}
