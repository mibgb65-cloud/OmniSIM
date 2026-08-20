package app.omnisim.android.data.update

import java.io.IOException
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
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
    val checksumDownloadUrl: String,
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
    private val cacheDirectory: File = File(
        System.getProperty("java.io.tmpdir") ?: ".",
        "omnisim-updates",
    ),
    private val apkVerifier: (File) -> Boolean = { true },
) {
    suspend fun getLatestRelease(): AppReleaseInfo = source.fetchLatestRelease()

    suspend fun downloadAndVerify(
        release: AppReleaseInfo,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        require(isTrustedUpdateDownloadUrl(release.apkDownloadUrl)) {
            "Release APK URL is invalid"
        }
        require(isTrustedChecksumDownloadUrl(release.checksumDownloadUrl)) {
            "Release checksum URL is invalid"
        }
        val expectedApkName = "OmniSIM-" + release.version + "-release.apk"
        require(URI(release.apkDownloadUrl).path.substringAfterLast('/') == expectedApkName) {
            "Release version does not match the APK name"
        }
        require(
            URI(release.checksumDownloadUrl).path.substringAfterLast('/') ==
                expectedApkName + ".sha256",
        ) {
            "Release version does not match the checksum name"
        }
        require(cacheDirectory.isDirectory || cacheDirectory.mkdirs()) {
            "Update cache directory is unavailable"
        }
        val target = File(cacheDirectory, expectedApkName)
        val partial = File(cacheDirectory, target.name + ".part")
        try {
            val checksum = parseSha256Checksum(
                fetchDownloadText(release.checksumDownloadUrl),
                target.name,
            )
            downloadApk(release.apkDownloadUrl, partial, onProgress)
            require(sha256Hex(partial) == checksum) {
                "Downloaded APK checksum does not match the official checksum"
            }
            require(apkVerifier(partial)) {
                "Downloaded APK package or signature is invalid"
            }
            if (target.exists()) target.delete()
            require(partial.renameTo(target)) { "Downloaded APK could not be finalized" }
            target
        } catch (error: Throwable) {
            partial.delete()
            throw error
        }
    }
}

private const val MAX_UPDATE_APK_BYTES = 100L * 1024L * 1024L

private fun fetchDownloadText(url: String): String {
    val connection = openDownloadConnection(url)
    try {
        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            throw IOException("Update download failed: HTTP " + responseCode)
        }
        return connection.inputStream.use { readLimitedText(it, 4 * 1024) }
    } finally {
        connection.disconnect()
    }
}

private fun readLimitedText(input: InputStream, maxBytes: Int): String {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(512)
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        require(output.size() + count <= maxBytes) { "Checksum response is too large" }
        output.write(buffer, 0, count)
    }
    return output.toString(Charsets.UTF_8.name())
}

private fun downloadApk(
    url: String,
    destination: File,
    onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
) {
    val connection = openDownloadConnection(url)
    try {
        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            throw IOException("Update APK download failed: HTTP " + responseCode)
        }
        val contentLength = connection.contentLength.toLong().takeIf { it >= 0L }
        require(contentLength == null || contentLength <= MAX_UPDATE_APK_BYTES) {
            "Update APK is unexpectedly large"
        }
        var downloaded = 0L
        var lastReported = -1L
        onProgress(0L, contentLength)
        connection.inputStream.use { input ->
            FileOutputStream(destination).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    downloaded += count
                    require(downloaded <= MAX_UPDATE_APK_BYTES) {
                        "Update APK is unexpectedly large"
                    }
                    output.write(buffer, 0, count)
                    if (downloaded - lastReported >= 64L * 1024L) {
                        onProgress(downloaded, contentLength)
                        lastReported = downloaded
                    }
                }
            }
        }
        onProgress(downloaded, contentLength ?: downloaded)
    } finally {
        connection.disconnect()
    }
}

private fun openDownloadConnection(url: String): HttpURLConnection =
    (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 15_000
        readTimeout = 30_000
        setRequestProperty("Accept", "application/octet-stream,text/plain")
        setRequestProperty("User-Agent", "OmniSIM-Android")
    }

internal fun parseSha256Checksum(response: String, expectedFileName: String): String {
    val tokens = response.lineSequence()
        .map { it.trim() }
        .filter(String::isNotEmpty)
        .firstOrNull()
        ?.split(Regex("""\s+"""))
        ?: throw IllegalArgumentException("Checksum response is empty")
    val checksum = tokens.firstOrNull()?.lowercase(Locale.ROOT)
        ?: throw IllegalArgumentException("Checksum response is invalid")
    require(checksum.length == 64 && checksum.all { it in "0123456789abcdef" }) {
        "Checksum response is invalid"
    }
    val fileName = tokens.getOrNull(1)?.removePrefix("*")
    require(fileName == null || fileName == expectedFileName) {
        "Checksum response names a different APK"
    }
    return checksum
}

private fun sha256Hex(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    return digest.digest().joinToString("") { byte ->
        "%02x".format(Locale.ROOT, byte.toInt() and 0xff)
    }
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
    val expectedApkName = "OmniSIM-$version-release.apk"
    val apk = release.assets.singleOrNull { it.name == expectedApkName }
        ?: throw IllegalArgumentException("Official release APK is missing or ambiguous")
    val checksum = release.assets.singleOrNull { it.name == "$expectedApkName.sha256" }
        ?: throw IllegalArgumentException("Release checksum is missing or ambiguous")
    require(isTrustedUpdateDownloadUrl(apk.downloadUrl)) { "Release APK URL is invalid" }
    require(isTrustedChecksumDownloadUrl(checksum.downloadUrl)) {
        "Release checksum URL is invalid"
    }
    return AppReleaseInfo(
        version = version,
        title = release.name?.trim().takeUnless { it.isNullOrEmpty() } ?: "OmniSIM $version",
        notes = release.body?.trim()?.takeIf(String::isNotEmpty)?.take(12_000),
        apkDownloadUrl = apk.downloadUrl,
        checksumDownloadUrl = checksum.downloadUrl,
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

internal fun isTrustedUpdateDownloadUrl(value: String): Boolean =
    isTrustedReleaseAssetUrl(value, suffix = "-release.apk")

internal fun isTrustedChecksumDownloadUrl(value: String): Boolean =
    isTrustedReleaseAssetUrl(value, suffix = "-release.apk.sha256")

private fun isTrustedReleaseAssetUrl(value: String, suffix: String): Boolean = runCatching {
    val uri = URI(value)
    val pathParts = uri.path.split('/').filter(String::isNotEmpty)
    val tagVersion = pathParts.getOrNull(4)?.removePrefix("v")?.removePrefix("V")
    val fileName = pathParts.getOrNull(5)
    uri.scheme == "https" &&
        uri.host.equals("github.com", ignoreCase = true) &&
        uri.port == -1 && uri.userInfo == null && uri.query == null && uri.fragment == null &&
        pathParts.take(4) == listOf("mibgb65-cloud", "OmniSIM", "releases", "download") &&
        tagVersion != null && fileName == "OmniSIM-$tagVersion$suffix" &&
        runCatching { parseVersion(tagVersion) }.isSuccess
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
