package io.github.mohithdas.opendisplay.tv.update

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class GithubReleaseParserTest {
    @Test
    fun acceptsLatestStableReleaseWithExactAssets() {
        val release = GithubReleaseParser.parse(releaseJson())

        assertEquals("v0.1.1", release.tagName)
        assertEquals(APK_ASSET_NAME, release.apk.name)
        assertEquals(CHECKSUM_ASSET_NAME, release.checksum.name)
    }

    @Test fun rejectsDraft() = assertReason(UpdateError.MALFORMED_RESPONSE) {
        GithubReleaseParser.parse(releaseJson(draft = true))
    }

    @Test fun rejectsPrerelease() = assertReason(UpdateError.MALFORMED_RESPONSE) {
        GithubReleaseParser.parse(releaseJson(prerelease = true))
    }

    @Test fun rejectsMissingApk() = assertReason(UpdateError.MISSING_APK) {
        GithubReleaseParser.parse(releaseJson(includeApk = false))
    }

    @Test fun rejectsMissingChecksum() = assertReason(UpdateError.MISSING_CHECKSUM) {
        GithubReleaseParser.parse(releaseJson(includeChecksum = false))
    }

    @Test fun rejectsMalformedJson() = assertReason(UpdateError.MALFORMED_RESPONSE) {
        GithubReleaseParser.parse("not json")
    }

    companion object {
        fun releaseJson(
            draft: Boolean = false,
            prerelease: Boolean = false,
            includeApk: Boolean = true,
            includeChecksum: Boolean = true,
        ): String {
            val assets = buildList {
                if (includeApk) add(
                    """{"name":"$APK_ASSET_NAME","browser_download_url":"https://github.com/a.apk","digest":"sha256:${"a".repeat(64)}"}""",
                )
                if (includeChecksum) add(
                    """{"name":"$CHECKSUM_ASSET_NAME","browser_download_url":"https://github.com/a.sha256"}""",
                )
            }.joinToString(",")
            return """{"draft":$draft,"prerelease":$prerelease,"tag_name":"v0.1.1","body":"Fixes","assets":[$assets]}"""
        }
    }
}

private fun assertReason(expected: UpdateError, block: () -> Unit) {
    try {
        block()
        fail("Expected $expected")
    } catch (failure: UpdateException) {
        assertEquals(expected, failure.reason)
    }
}
