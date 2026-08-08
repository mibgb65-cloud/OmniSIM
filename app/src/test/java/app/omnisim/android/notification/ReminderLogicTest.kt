package app.omnisim.android.notification

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderLogicTest {
    private val offsets = setOf(30, 14, 7, 3, 1, 0, -1)

    @Test
    fun `matches configured exact offsets`() {
        assertEquals(30, matchedReminderOffset(30, offsets))
        assertEquals(7, matchedReminderOffset(7, offsets))
        assertEquals(0, matchedReminderOffset(0, offsets))
        assertNull(matchedReminderOffset(6, offsets))
    }

    @Test
    fun `all overdue days share one overdue offset`() {
        assertEquals(-1, matchedReminderOffset(-1, offsets))
        assertEquals(-1, matchedReminderOffset(-40, offsets))
        assertNull(matchedReminderOffset(-1, offsets - -1))
    }

    @Test
    fun `same cycle reminder key is deduplicated`() {
        val key = ReminderKey("sim-1", LocalDate.of(2026, 8, 14), 7)
        assertFalse(shouldSendReminder(key, setOf(key)))
        assertTrue(shouldSendReminder(key.copy(renewalDate = LocalDate.of(2026, 11, 12)), setOf(key)))
        assertTrue(shouldSendReminder(key.copy(reminderOffset = 3), setOf(key)))
    }
}

