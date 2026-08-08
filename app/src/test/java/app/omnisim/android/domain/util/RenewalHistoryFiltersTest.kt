package app.omnisim.android.domain.util

import app.omnisim.android.data.local.entity.RenewalHistoryEntity
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class RenewalHistoryFiltersTest {
    private val today = LocalDate.of(2026, 8, 8)

    @Test
    fun `history is filtered by SIM and sorted newest first`() {
        val history = listOf(
            renewal("older", "sim-a", today.minusDays(20), "2026-08-01T00:00:00Z"),
            renewal("other", "sim-b", today.minusDays(1), "2026-08-07T00:00:00Z"),
            renewal("newer", "sim-a", today.minusDays(2), "2026-08-08T00:00:00Z"),
        )

        val result = filterRenewalHistory(
            history = history,
            simId = "sim-a",
            range = RenewalHistoryRange.All,
            today = today,
        )

        assertEquals(listOf("newer", "older"), result.map(RenewalHistoryEntity::id))
    }

    @Test
    fun `time range includes its boundary and excludes older records`() {
        val history = listOf(
            renewal("inside", "sim-a", today.minusDays(30), "2026-07-09T00:00:00Z"),
            renewal("outside", "sim-a", today.minusDays(31), "2026-07-08T00:00:00Z"),
        )

        val result = filterRenewalHistory(
            history = history,
            simId = null,
            range = RenewalHistoryRange.Last30Days,
            today = today,
        )

        assertEquals(listOf("inside"), result.map(RenewalHistoryEntity::id))
    }

    @Test
    fun `records on the same date use creation time as tie breaker`() {
        val history = listOf(
            renewal("first", "sim-a", today, "2026-08-08T08:00:00Z"),
            renewal("second", "sim-a", today, "2026-08-08T09:00:00Z"),
        )

        val result = filterRenewalHistory(
            history = history,
            simId = null,
            range = RenewalHistoryRange.All,
            today = today,
        )

        assertEquals(listOf("second", "first"), result.map(RenewalHistoryEntity::id))
    }

    private fun renewal(
        id: String,
        simId: String,
        date: LocalDate,
        createdAt: String,
    ) = RenewalHistoryEntity(
        id = id,
        simId = simId,
        renewalDate = date,
        previousRenewalDate = null,
        nextRenewalDate = date.plusDays(30),
        amount = null,
        currency = null,
        notes = null,
        createdAt = Instant.parse(createdAt),
    )
}
