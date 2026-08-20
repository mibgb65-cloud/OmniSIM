package app.omnisim.android.domain.util

import app.omnisim.android.data.local.entity.RenewalHistoryEntity
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RenewalCostHistoryUtilsTest {
    private val today = LocalDate.of(2026, 8, 11)

    @Test
    fun `actual spend groups active SIM history by original currency and period`() {
        val history = listOf(
            renewal("today", "sim-a", today, 10.0, "usd", 7),
            renewal("boundary", "sim-a", today.minusDays(30), 20.0, "USD", 6),
            renewal("older", "sim-a", today.minusDays(31), 30.0, "USD", 5),
            renewal("euro", "sim-a", today.minusDays(100), 5.0, "EUR", 4),
            renewal("archived", "sim-b", today, 100.0, "USD", 3),
            renewal("future", "sim-a", today.plusDays(1), 200.0, "USD", 2),
            renewal("too-old", "sim-a", today.minusDays(366), 300.0, "USD", 1),
        )

        val result = calculateActualSpend(history, setOf("sim-a"), today)

        assertEquals(
            listOf(
                ActualSpendByCurrency("EUR", 0.0, 5.0),
                ActualSpendByCurrency("USD", 30.0, 60.0),
            ),
            result.byCurrency,
        )
        assertEquals(0, result.incompleteRecordCount)
    }

    @Test
    fun `actual spend reports incomplete recent records`() {
        val history = listOf(
            renewal("no-amount", "sim-a", today, null, "USD", 4),
            renewal("no-currency", "sim-a", today, 10.0, null, 3),
            renewal("invalid", "sim-a", today, Double.NaN, "USD", 2),
            renewal("other", "sim-b", today, null, null, 1),
        )

        val result = calculateActualSpend(history, setOf("sim-a"), today)

        assertEquals(emptyList<ActualSpendByCurrency>(), result.byCurrency)
        assertEquals(3, result.incompleteRecordCount)
    }

    @Test
    fun `price change compares latest two recorded amounts in the same currency`() {
        val history = listOf(
            renewal("old-usd", "sim-a", today.minusDays(90), 10.0, "USD", 1),
            renewal("eur", "sim-a", today.minusDays(60), 100.0, "EUR", 2),
            renewal("latest", "sim-a", today.minusDays(30), 15.0, "usd", 3),
        )

        assertEquals(
            RenewalPriceChange("USD", 15.0, 10.0, 50.0),
            calculateLatestRenewalPriceChange(history, "sim-a"),
        )
    }

    @Test
    fun `price change omits a percentage when the previous amount is zero`() {
        val history = listOf(
            renewal("zero", "sim-a", today.minusDays(60), 0.0, "USD", 1),
            renewal("latest", "sim-a", today.minusDays(30), 5.0, "USD", 2),
        )

        val result = calculateLatestRenewalPriceChange(history, "sim-a")!!

        assertEquals(5.0, result.latestAmount, 0.0)
        assertNull(result.percentageChange)
    }

    @Test
    fun `price change requires two usable same-currency records`() {
        val history = listOf(
            renewal("usd", "sim-a", today.minusDays(30), 5.0, "USD", 2),
            renewal("eur", "sim-a", today.minusDays(60), 4.0, "EUR", 1),
        )

        assertNull(calculateLatestRenewalPriceChange(history, "sim-a"))
    }

    private fun renewal(
        id: String,
        simId: String,
        date: LocalDate,
        amount: Double?,
        currency: String?,
        order: Long,
    ) = RenewalHistoryEntity(
        id = id,
        simId = simId,
        renewalDate = date,
        previousRenewalDate = null,
        nextRenewalDate = date.plusDays(30),
        amount = amount,
        currency = currency,
        notes = null,
        createdAt = Instant.ofEpochSecond(order),
    )
}
