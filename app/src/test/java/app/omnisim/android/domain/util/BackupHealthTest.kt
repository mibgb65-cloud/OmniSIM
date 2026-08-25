package app.omnisim.android.domain.util

import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupHealthTest {
    private val now = Instant.parse("2026-08-25T12:00:00Z")

    @Test
    fun `pending changes always recommend a backup`() {
        assertTrue(isBackupRecommended(now, backupDirty = true, now = now))
    }

    @Test
    fun `clean backup becomes stale after thirty days`() {
        assertFalse(isBackupRecommended(now.minusSeconds(30L * 24 * 60 * 60), false, now))
        assertTrue(isBackupRecommended(now.minusSeconds(30L * 24 * 60 * 60 + 1), false, now))
    }

    @Test
    fun `missing backup recommends export`() {
        assertTrue(isBackupRecommended(null, backupDirty = false, now = now))
    }
}
