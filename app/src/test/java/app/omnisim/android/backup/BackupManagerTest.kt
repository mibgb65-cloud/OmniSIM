package app.omnisim.android.backup

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupManagerTest {
    @Test
    fun `backup input at size limit is accepted`() {
        val content = "1234"

        assertEquals(
            content,
            ByteArrayInputStream(content.toByteArray()).readBackupText(maxBytes = 4),
        )
    }

    @Test
    fun `backup input over size limit is rejected`() {
        assertThrows(BackupValidationException::class.java) {
            ByteArrayInputStream("12345".toByteArray()).readBackupText(maxBytes = 4)
        }
    }
}
