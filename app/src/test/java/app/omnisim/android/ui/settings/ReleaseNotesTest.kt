package app.omnisim.android.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseNotesTest {
    @Test
    fun parseReleaseNotes_formatsHeadingsBulletsAndWrappedParagraphs() {
        val blocks = parseReleaseNotes(
            """
            # OmniSIM 1.2.0
            ## Improvements
            - Fix update timing
            This paragraph wraps
            across two lines.
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                ReleaseNoteBlock.Heading("OmniSIM 1.2.0", 1),
                ReleaseNoteBlock.Heading("Improvements", 2),
                ReleaseNoteBlock.Bullet("Fix update timing"),
                ReleaseNoteBlock.Paragraph("This paragraph wraps across two lines."),
            ),
            blocks,
        )
    }

    @Test
    fun parseReleaseNotes_formatsCurrentReleaseSyntax() {
        val blocks = parseReleaseNotes(
            """
            - In-app updates require an exact official APK name and
              its matching **SHA-256** asset.

            1. Download `OmniSIM-release.apk`.
            2. Verify the checksum.

            ---
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                ReleaseNoteBlock.Bullet(
                    "In-app updates require an exact official APK name and its matching SHA-256 asset.",
                ),
                ReleaseNoteBlock.Numbered(1, "Download OmniSIM-release.apk."),
                ReleaseNoteBlock.Numbered(2, "Verify the checksum."),
            ),
            blocks,
        )
    }
}
