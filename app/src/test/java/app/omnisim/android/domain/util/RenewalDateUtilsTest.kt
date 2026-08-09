package app.omnisim.android.domain.util

import app.omnisim.android.domain.model.RenewalStatus
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RenewalDateUtilsTest {
    private val augustFirst = LocalDate.of(2026, 8, 1)

    @Test
    fun `renewal calculations use the actual renewal date`() {
        assertEquals(LocalDate.of(2026, 8, 31), calculateNextRenewalDate(augustFirst, 30))
        assertEquals(LocalDate.of(2026, 10, 30), calculateNextRenewalDate(augustFirst, 90))
        assertEquals(LocalDate.of(2027, 1, 28), calculateNextRenewalDate(augustFirst, 180))
        assertEquals(LocalDate.of(2027, 8, 1), calculateNextRenewalDate(augustFirst, 365))
    }

    @Test
    fun `renewal cycle must be positive`() {
        assertThrows(IllegalArgumentException::class.java) {
            calculateNextRenewalDate(augustFirst, 0)
        }
    }

    @Test
    fun `monthly renewal uses the next selected calendar day`() {
        assertEquals(
            LocalDate.of(2026, 9, 1),
            calculateNextMonthlyRenewalDate(LocalDate.of(2026, 8, 1), 1),
        )
        assertEquals(
            LocalDate.of(2026, 9, 1),
            calculateNextMonthlyRenewalDate(LocalDate.of(2026, 8, 20), 1),
        )
        assertEquals(
            LocalDate.of(2026, 8, 25),
            calculateNextMonthlyRenewalDate(LocalDate.of(2026, 8, 20), 25),
        )
    }

    @Test
    fun `monthly renewal clamps to the last day of short months`() {
        assertEquals(
            LocalDate.of(2027, 2, 28),
            calculateNextMonthlyRenewalDate(LocalDate.of(2027, 1, 31), 31),
        )
        assertEquals(
            LocalDate.of(2028, 2, 29),
            calculateNextMonthlyRenewalDate(LocalDate.of(2028, 1, 31), 31),
        )
    }

    @Test
    fun `monthly renewal day must be valid`() {
        assertThrows(IllegalArgumentException::class.java) {
            calculateNextMonthlyRenewalDate(augustFirst, 32)
        }
    }

    @Test
    fun `days until renewal preserves calendar dates`() {
        assertEquals(9L, daysUntilRenewal(augustFirst, LocalDate.of(2026, 8, 10)))
        assertEquals(-1L, daysUntilRenewal(augustFirst, LocalDate.of(2026, 7, 31)))
    }

    @Test
    fun `status precedence covers every state`() {
        val today = LocalDate.of(2026, 8, 7)
        assertEquals(
            RenewalStatus.Active,
            calculateRenewalStatus(today, today.plusDays(15), 14, archived = false),
        )
        assertEquals(
            RenewalStatus.DueSoon,
            calculateRenewalStatus(today, today.plusDays(14), 14, archived = false),
        )
        assertEquals(
            RenewalStatus.DueToday,
            calculateRenewalStatus(today, today, 14, archived = false),
        )
        assertEquals(
            RenewalStatus.Overdue,
            calculateRenewalStatus(today, today.minusDays(1), 14, archived = false),
        )
        assertEquals(
            RenewalStatus.Archived,
            calculateRenewalStatus(today, today.minusDays(1), 14, archived = true),
        )
    }
}
