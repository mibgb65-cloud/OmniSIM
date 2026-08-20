package app.omnisim.android.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateRepositoryTest {
    @Test
    fun compareVersionNames_handlesPrefixesAndMissingSegments() {
        assertTrue(compareVersionNames("v1.1.0", "1.0.9") > 0)
        assertEquals(0, compareVersionNames("1.0", "1.0.0"))
        assertTrue(compareVersionNames("1.0.0", "1.0.1") < 0)
    }

    @Test
    fun parseGitHubRelease_readsStrictReleaseAssets() {
        val release = parseGitHubRelease(
            """
            {
              "tag_name": "v1.2.0",
              "name": "OmniSIM 1.2.0",
              "body": "  Fixes and improvements.  ",
              "assets": [
                {
                  "name": "OmniSIM-1.2.0-debug.apk",
                  "browser_download_url": "https://github.com/mibgb65-cloud/OmniSIM/releases/download/v1.2.0/debug.apk"
                },
                {
                  "name": "OmniSIM-1.2.0-release.apk",
                  "browser_download_url": "https://github.com/mibgb65-cloud/OmniSIM/releases/download/v1.2.0/OmniSIM-1.2.0-release.apk"
                },
                {
                  "name": "OmniSIM-1.2.0-release.apk.sha256",
                  "browser_download_url": "https://github.com/mibgb65-cloud/OmniSIM/releases/download/v1.2.0/OmniSIM-1.2.0-release.apk.sha256"
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals("1.2.0", release.version)
        assertEquals("OmniSIM 1.2.0", release.title)
        assertEquals("Fixes and improvements.", release.notes)
        assertTrue(release.apkDownloadUrl.endsWith("OmniSIM-1.2.0-release.apk"))
        assertTrue(release.checksumDownloadUrl.endsWith("OmniSIM-1.2.0-release.apk.sha256"))
    }

    @Test
    fun parseGitHubRelease_rejectsMissingApk() {
        val response = """
            {
              "tag_name": "v1.2.0",
              "assets": []
            }
        """.trimIndent()

        assertThrows(IllegalArgumentException::class.java) {
            parseGitHubRelease(response)
        }
    }

    @Test
    fun parseGitHubRelease_rejectsDebugOnlyRelease() {
        val response = """
            {
              "tag_name": "v1.2.0",
              "assets": [
                {
                  "name": "OmniSIM-1.2.0-debug.apk",
                  "browser_download_url": "https://github.com/mibgb65-cloud/OmniSIM/releases/download/v1.2.0/OmniSIM-1.2.0-debug.apk"
                }
              ]
            }
        """.trimIndent()

        assertThrows(IllegalArgumentException::class.java) {
            parseGitHubRelease(response)
        }
    }

    @Test
    fun parseGitHubRelease_rejectsMissingChecksum() {
        val response = """
            {
              "tag_name": "v1.2.0",
              "assets": [
                {
                  "name": "OmniSIM-1.2.0-release.apk",
                  "browser_download_url": "https://github.com/mibgb65-cloud/OmniSIM/releases/download/v1.2.0/OmniSIM-1.2.0-release.apk"
                }
              ]
            }
        """.trimIndent()

        assertThrows(IllegalArgumentException::class.java) {
            parseGitHubRelease(response)
        }
    }

    @Test
    fun trustedDownloadUrl_requiresExactOfficialReleasePath() {
        assertEquals(
            false,
            isTrustedUpdateDownloadUrl(
                "https://github.com/mibgb65-cloud/OmniSIM/releases/download/v1.2.0/OmniSIM.apk",
            ),
        )
        assertTrue(
            isTrustedChecksumDownloadUrl(
                "https://github.com/mibgb65-cloud/OmniSIM/releases/download/v1.2.0/OmniSIM-1.2.0-release.apk.sha256",
            ),
        )
        assertTrue(
            isTrustedUpdateDownloadUrl(
                "https://github.com/mibgb65-cloud/OmniSIM/releases/download/v1.2.0/OmniSIM-1.2.0-release.apk",
            ),
        )
        assertEquals(
            false,
            isTrustedUpdateDownloadUrl(
                "https://github.com/mibgb65-cloud/OmniSIM/releases/download/v1.2.0/OmniSIM-1.2.0-debug.apk",
            ),
        )
        assertEquals(false, isTrustedUpdateDownloadUrl("http://github.com/example.apk"))
        assertEquals(false, isTrustedUpdateDownloadUrl("https://example.com/OmniSIM.apk"))
    }

    @Test
    fun parseSha256Checksum_acceptsGitHubChecksumFile() {
        assertEquals(
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
            parseSha256Checksum(
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef  OmniSIM-1.3.0-release.apk",
                "OmniSIM-1.3.0-release.apk",
            ),
        )
    }

    @Test
    fun parseSha256Checksum_rejectsWrongFileName() {
        assertThrows(IllegalArgumentException::class.java) {
            parseSha256Checksum(
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef  other.apk",
                "OmniSIM-1.3.0-release.apk",
            )
        }
    }

    @Test
    fun parseSha256Checksum_rejectsInvalidHash() {
        assertThrows(IllegalArgumentException::class.java) {
            parseSha256Checksum("not-a-checksum", "OmniSIM-1.3.0-release.apk")
        }
    }
}
