package io.github.mohithdas.opendisplay.tv.update

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.net.SocketTimeoutException

class GithubClientTest {
    @Test
    fun apiTimeoutIsRecoverable() = runTest {
        assertSuspendReason(UpdateError.TIMEOUT) {
            GithubReleaseClient(FakeTransport(failure = SocketTimeoutException())).latestStable()
        }
    }

    @Test
    fun rateLimitIsReported() = runTest {
        assertSuspendReason(UpdateError.RATE_LIMITED) {
            GithubReleaseClient(
                FakeTransport(response = HttpTextResponse(403, "", mapOf("x-ratelimit-remaining" to "0"))),
            ).latestStable()
        }
    }

    @Test
    fun malformedResponseFailsClosed() = runTest {
        assertSuspendReason(UpdateError.MALFORMED_RESPONSE) {
            GithubReleaseClient(FakeTransport(response = HttpTextResponse(200, "{}", emptyMap())))
                .latestStable()
        }
    }

    @Test
    fun redirectHostsMustRemainOnGithubInfrastructure() {
        assertTrue(GithubHostPolicy.isAllowed("https://github.com/owner/repo/releases/download/v/a.apk"))
        assertTrue(GithubHostPolicy.isAllowed("https://release-assets.githubusercontent.com/a"))
        assertFalse(GithubHostPolicy.isAllowed("http://github.com/a"))
        assertFalse(GithubHostPolicy.isAllowed("https://github.com.evil.example/a"))
        assertFalse(GithubHostPolicy.isAllowed("https://evil.example/a"))
    }
}

private class FakeTransport(
    private val response: HttpTextResponse? = null,
    private val failure: Exception? = null,
) : GithubTransport {
    override suspend fun getText(url: String, maxBytes: Long): HttpTextResponse {
        failure?.let { throw it }
        return response ?: HttpTextResponse(200, GithubReleaseParserTest.releaseJson(), emptyMap())
    }

    override suspend fun download(
        url: String,
        target: File,
        maxBytes: Long,
        onProgress: (Int) -> Unit,
    ): DownloadEvidence = throw UnsupportedOperationException()
}

private suspend fun assertSuspendReason(expected: UpdateError, block: suspend () -> Unit) {
    try {
        block()
        fail("Expected $expected")
    } catch (failure: UpdateException) {
        assertEquals(expected, failure.reason)
    }
}
