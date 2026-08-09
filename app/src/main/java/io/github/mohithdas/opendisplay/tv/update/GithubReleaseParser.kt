package io.github.mohithdas.opendisplay.tv.update

import org.json.JSONException
import org.json.JSONObject

object GithubReleaseParser {
    private const val MAX_NOTES_LENGTH = 16_384

    fun parse(json: String): StableRelease {
        try {
            val root = JSONObject(json)
            if (root.optBoolean("draft", false) || root.optBoolean("prerelease", false)) {
                throw UpdateException(UpdateError.MALFORMED_RESPONSE)
            }
            val tag = root.getString("tag_name").trim()
            val version = tag.removePrefix("v")
            if (!VersionPolicy.isReleaseVersion(version)) {
                throw UpdateException(UpdateError.MALFORMED_RESPONSE)
            }
            val assets = root.getJSONArray("assets")
            var apk: ReleaseAsset? = null
            var checksum: ReleaseAsset? = null
            for (index in 0 until assets.length()) {
                val item = assets.getJSONObject(index)
                val asset = ReleaseAsset(
                    name = item.getString("name"),
                    url = item.getString("browser_download_url"),
                    digest = item.optString("digest").takeIf { it.isNotBlank() },
                )
                when (asset.name) {
                    APK_ASSET_NAME -> apk = asset
                    CHECKSUM_ASSET_NAME -> checksum = asset
                }
            }
            return StableRelease(
                tagName = tag,
                versionName = version,
                notes = root.optString("body").take(MAX_NOTES_LENGTH),
                apk = apk ?: throw UpdateException(UpdateError.MISSING_APK),
                checksum = checksum ?: throw UpdateException(UpdateError.MISSING_CHECKSUM),
            )
        } catch (failure: UpdateException) {
            throw failure
        } catch (failure: JSONException) {
            throw UpdateException(UpdateError.MALFORMED_RESPONSE, failure)
        }
    }
}
