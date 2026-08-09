package io.github.mohithdas.opendisplay.tv.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.security.MessageDigest

data class HttpTextResponse(
    val status: Int,
    val body: String,
    val headers: Map<String, String>,
)

interface GithubTransport {
    suspend fun getText(url: String, maxBytes: Long): HttpTextResponse
    suspend fun download(
        url: String,
        target: File,
        maxBytes: Long,
        onProgress: (Int) -> Unit,
    ): DownloadEvidence
}

object GithubHostPolicy {
    private val ALLOWED_HOSTS = setOf(
        "api.github.com",
        "github.com",
        "objects.githubusercontent.com",
        "release-assets.githubusercontent.com",
        "github-releases.githubusercontent.com",
    )

    fun isAllowed(raw: String): Boolean = try {
        val url = URL(raw)
        url.protocol.equals("https", ignoreCase = true) &&
            url.userInfo == null &&
            (url.port == -1 || url.port == 443) &&
            url.host.lowercase() in ALLOWED_HOSTS
    } catch (_: Exception) {
        false
    }
}

class UrlConnectionGithubTransport : GithubTransport {
    override suspend fun getText(url: String, maxBytes: Long): HttpTextResponse = withContext(Dispatchers.IO) {
        val connection = openFollowingRedirects(url)
        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8 * 1024)
                var total = 0L
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > maxBytes) throw UpdateException(UpdateError.MALFORMED_RESPONSE)
                    output.write(buffer, 0, count)
                }
                output.toString(Charsets.UTF_8.name())
            }.orEmpty()
            HttpTextResponse(status, body, responseHeaders(connection))
        } finally {
            connection.disconnect()
        }
    }

    override suspend fun download(
        url: String,
        target: File,
        maxBytes: Long,
        onProgress: (Int) -> Unit,
    ): DownloadEvidence = withContext(Dispatchers.IO) {
        target.parentFile?.mkdirs()
        target.delete()
        val connection = openFollowingRedirects(url)
        try {
            if (connection.responseCode !in 200..299) {
                throw UpdateException(UpdateError.SERVER)
            }
            val contentLength = connection.contentLengthLong
            if (contentLength > maxBytes) throw UpdateException(UpdateError.STORAGE)
            val digest = MessageDigest.getInstance("SHA-256")
            var total = 0L
            connection.inputStream.use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > maxBytes) throw UpdateException(UpdateError.STORAGE)
                        digest.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                        if (contentLength > 0) {
                            onProgress(((total * 100L) / contentLength).toInt().coerceIn(0, 100))
                        }
                    }
                    output.fd.sync()
                }
            }
            if (total <= 0) throw UpdateException(UpdateError.STORAGE)
            onProgress(100)
            DownloadEvidence(target, digest.digest().toHex())
        } catch (failure: Exception) {
            target.delete()
            throw failure
        } finally {
            connection.disconnect()
        }
    }

    private fun openFollowingRedirects(raw: String): HttpURLConnection {
        var current = raw
        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            if (!GithubHostPolicy.isAllowed(current)) {
                throw UpdateException(UpdateError.INVALID_REDIRECT)
            }
            val connection = (URL(current).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty(
                    "User-Agent",
                    "OpenDisplay-TV-Android/${io.github.mohithdas.opendisplay.tv.BuildConfig.VERSION_NAME}",
                )
                setRequestProperty("Accept", "application/vnd.github+json, application/octet-stream")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            }
            val status = connection.responseCode
            if (status !in REDIRECT_CODES) return connection
            if (redirectCount == MAX_REDIRECTS) {
                connection.disconnect()
                throw UpdateException(UpdateError.INVALID_REDIRECT)
            }
            val location = connection.getHeaderField("Location")
            val next = location?.let { URL(URL(current), it).toString() }
            connection.disconnect()
            if (next == null || !GithubHostPolicy.isAllowed(next)) {
                throw UpdateException(UpdateError.INVALID_REDIRECT)
            }
            current = next
        }
        throw UpdateException(UpdateError.INVALID_REDIRECT)
    }

    private fun responseHeaders(connection: HttpURLConnection): Map<String, String> =
        connection.headerFields.entries.mapNotNull { (key, values) ->
            key?.lowercase()?.let { it to values.orEmpty().joinToString(",") }
        }.toMap()

    private fun ByteArray.toHex(): String = joinToString("") { byte -> "%02x".format(byte) }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 30_000
        const val MAX_REDIRECTS = 5
        val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
    }
}

interface ReleaseSource {
    suspend fun latestStable(): StableRelease
}

class GithubReleaseClient(private val transport: GithubTransport) : ReleaseSource {
    override suspend fun latestStable(): StableRelease {
        return try {
            val response = transport.getText(RELEASE_API_URL, MAX_RESPONSE_BYTES)
            when {
                response.status in 200..299 -> GithubReleaseParser.parse(response.body)
                response.status == 403 && response.headers["x-ratelimit-remaining"] == "0" ->
                    throw UpdateException(UpdateError.RATE_LIMITED)
                response.status == 429 -> throw UpdateException(UpdateError.RATE_LIMITED)
                else -> throw UpdateException(UpdateError.SERVER)
            }
        } catch (failure: UpdateException) {
            throw failure
        } catch (failure: SocketTimeoutException) {
            throw UpdateException(UpdateError.TIMEOUT, failure)
        } catch (failure: UnknownHostException) {
            throw UpdateException(UpdateError.OFFLINE, failure)
        } catch (failure: Exception) {
            throw UpdateException(UpdateError.UNKNOWN, failure)
        }
    }

    private companion object {
        const val MAX_RESPONSE_BYTES = 2L * 1024L * 1024L
    }
}
