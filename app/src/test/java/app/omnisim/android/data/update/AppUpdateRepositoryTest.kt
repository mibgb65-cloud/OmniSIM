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
    fun parseGitHubRelease_readsNotesAndPrefersReleaseApk() {
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
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals("1.2.0", release.version)
        assertEquals("OmniSIM 1.2.0", release.title)
        assertEquals("Fixes and improvements.", release.notes)
        assertTrue(release.apkDownloadUrl.endsWith("OmniSIM-1.2.0-release.apk"))
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
    fun trustedDownloadUrl_requiresGitHubHttpsApk() {
        assertTrue(
            isTrustedUpdateDownloadUrl(
                "https://github.com/mibgb65-cloud/OmniSIM/releases/download/v1.2.0/OmniSIM.apk",
            ),
        )
        assertEquals(false, isTrustedUpdateDownloadUrl("http://github.com/example.apk"))
        assertEquals(false, isTrustedUpdateDownloadUrl("https://example.com/OmniSIM.apk"))
    }
}
