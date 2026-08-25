package app.omnisim.android.notification

import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderHealthTest {
    private val now = Instant.parse("2026-08-25T12:00:00Z")

    @Test
    fun `check is fresh for no more than forty eight hours`() {
        assertTrue(isReminderCheckFresh(now.minusSeconds(48 * 60 * 60), now))
        assertFalse(isReminderCheckFresh(now.minusSeconds(48 * 60 * 60 + 1), now))
    }

    @Test
    fun `missing check is not fresh`() {
        assertFalse(isReminderCheckFresh(null, now))
    }
}
