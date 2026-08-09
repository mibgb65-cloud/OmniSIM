package app.omnisim.android.data.update

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val LATEST_RELEASE_API_URL =
    "https://api.github.com/repos/mibgb65-cloud/OmniSIM/releases/latest"
private val GITHUB_JSON = Json { ignoreUnknownKeys = true }

data class AppReleaseInfo(
    val version: String,
    val title: String,
    val notes: String?,
    val apkDownloadUrl: String,
)

fun interface AppUpdateSource {
    suspend fun fetchLatestRelease(): AppReleaseInfo
}

class GitHubAppUpdateSource(
    private val endpoint: String = LATEST_RELEASE_API_URL,
) : AppUpdateSource {
    override suspend fun fetchLatestRelease(): AppReleaseInfo = withContext(Dispatchers.IO) {
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "OmniSIM-Android")
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IOException("GitHub release request failed: HTTP $responseCode")
            }
            val response = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            parseGitHubRelease(response)
        } finally {
            connection.disconnect()
        }
    }
}

class AppUpdateRepository(
    private val source: AppUpdateSource = GitHubAppUpdateSource(),
) {
    suspend fun getLatestRelease(): AppReleaseInfo = source.fetchLatestRelease()
}

@Serializable
private data class GitHubReleaseResponse(
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val body: String? = null,
    val assets: List<GitHubReleaseAsset> = emptyList(),
)

@Serializable
private data class GitHubReleaseAsset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
)

internal fun parseGitHubRelease(response: String): AppReleaseInfo {
    val release = GITHUB_JSON.decodeFromString<GitHubReleaseResponse>(response)
    val version = release.tagName.trim().removePrefix("v").removePrefix("V")
    parseVersion(version)
    val apk = release.assets.firstOrNull { it.name.endsWith("-release.apk", ignoreCase = true) }
        ?: release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
        ?: throw IllegalArgumentException("Release APK is missing")
    require(isTrustedUpdateDownloadUrl(apk.downloadUrl)) { "Release APK URL is invalid" }
    return AppReleaseInfo(
        version = version,
        title = release.name?.trim().takeUnless { it.isNullOrEmpty() } ?: "OmniSIM $version",
        notes = release.body?.trim()?.takeIf(String::isNotEmpty)?.take(12_000),
        apkDownloadUrl = apk.downloadUrl,
    )
}

internal fun compareVersionNames(first: String, second: String): Int {
    val firstParts = parseVersion(first)
    val secondParts = parseVersion(second)
    val size = maxOf(firstParts.size, secondParts.size)
    repeat(size) { index ->
        val comparison = firstParts.getOrElse(index) { 0 }
            .compareTo(secondParts.getOrElse(index) { 0 })
        if (comparison != 0) return comparison
    }
    return 0
}

internal fun isTrustedUpdateDownloadUrl(value: String): Boolean = runCatching {
    val uri = URI(value)
    uri.scheme.equals("https", ignoreCase = true) &&
        uri.host.equals("github.com", ignoreCase = true) &&
        uri.path.endsWith(".apk", ignoreCase = true)
}.getOrDefault(false)

private fun parseVersion(value: String): List<Int> {
    val core = value.trim()
        .removePrefix("v")
        .removePrefix("V")
        .substringBefore('+')
        .substringBefore('-')
    val parts = core.split('.')
    require(parts.size in 1..4 && parts.all { part -> part.isNotEmpty() && part.all(Char::isDigit) }) {
        "Invalid version: $value"
    }
    return parts.map { part -> part.toIntOrNull() ?: throw IllegalArgumentException("Invalid version: $value") }
}
