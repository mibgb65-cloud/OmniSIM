package app.omnisim.android.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsageCostUtilsTest {
    @Test
    fun `daily holding cost divides renewal price by cycle days`() {
        assertEquals(10.0 / 90.0, calculateDailyHoldingCost(10.0, 90)!!, 0.000_001)
    }

    @Test
    fun `zero renewal price is a valid zero cost`() {
        assertEquals(0.0, calculateDailyHoldingCost(0.0, 30)!!, 0.0)
    }

    @Test
    fun `missing or invalid values are excluded`() {
        assertNull(calculateDailyHoldingCost(null, 30))
        assertNull(calculateDailyHoldingCost(10.0, null))
        assertNull(calculateDailyHoldingCost(-1.0, 30))
        assertNull(calculateDailyHoldingCost(Double.NaN, 30))
        assertNull(calculateDailyHoldingCost(10.0, 0))
    }

    @Test
    fun `currency conversion uses euro cross rates`() {
        val rates = mapOf("EUR" to 1.0, "USD" to 1.2, "CNY" to 8.0)

        assertEquals(80.0, convertCurrencyAmount(12.0, "USD", "CNY", rates)!!, 0.000_001)
        assertEquals(10.0, convertCurrencyAmount(12.0, "USD", "EUR", rates)!!, 0.000_001)
        assertEquals(8.0, convertCurrencyAmount(1.0, "EUR", "CNY", rates)!!, 0.000_001)
    }

    @Test
    fun `converted total reports currencies without a usable rate`() {
        val total = calculateConvertedCostTotal(
            costs = listOf(
                CurrencyAmount("USD", 12.0),
                CurrencyAmount("EUR", 10.0),
                CurrencyAmount("XYZ", 5.0),
            ),
            targetCurrency = "CNY",
            ratesPerEuro = mapOf("EUR" to 1.0, "USD" to 1.2, "CNY" to 8.0),
        )

        assertEquals(160.0, total.daily, 0.000_001)
        assertEquals(2, total.includedCount)
        assertEquals(setOf("XYZ"), total.excludedCurrencies)
    }

    @Test
    fun `same currency costs do not require an exchange-rate entry`() {
        assertEquals(
            4.5,
            convertCurrencyAmount(4.5, "USD", "USD", emptyMap())!!,
            0.0,
        )
    }
}
