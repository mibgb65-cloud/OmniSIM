package app.omnisim.android.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderSettingsTest {
    @Test
    fun nullOverrideUsesGlobalOffsets() {
        assertEquals(setOf(14, 1), effectiveReminderOffsets(null, setOf(14, 1)))
    }

    @Test
    fun emptyOverrideDisablesAllOffsetsWithoutDisablingSim() {
        assertEquals(emptySet<Int>(), effectiveReminderOffsets(emptySet(), setOf(14, 1)))
    }

    @Test
    fun onlySupportedOffsetsAreValid() {
        assertTrue(areReminderOffsetsValid(setOf(30, 0, -1)))
        assertFalse(areReminderOffsetsValid(setOf(2)))
    }
}
