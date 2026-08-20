package app.omnisim.android.data.exchange

import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class OfficialExchangeRateSourceTest {
    @Test
    fun `keeps ECB rates and fills missing currencies from InforEuro`() = runTest {
        val source = OfficialExchangeRateSource(
            ecbSource = ExchangeRateSource {
                snapshot(
                    date = LocalDate.of(2026, 8, 10),
                    rates = mapOf("EUR" to 1.0, "USD" to 1.1555),
                )
            },
            inforEuroSource = ExchangeRateSource {
                snapshot(
                    date = LocalDate.of(2026, 8, 1),
                    rates = mapOf("EUR" to 1.0, "USD" to 1.1476, "TWD" to 37.1088),
                    coverage = ExchangeRateCoverage.InforEuroMonthly,
                )
            },
        )

        val result = source.fetch()

        assertEquals(ExchangeRateCoverage.EcbDailyWithInforEuroMonthly, result.coverage)
        assertEquals(LocalDate.of(2026, 8, 10), result.rateDate)
        assertEquals(1.1555, result.ratesPerEuro.getValue("USD"), 0.0)
        assertEquals(37.1088, result.ratesPerEuro.getValue("TWD"), 0.0)
        assertEquals(LocalDate.of(2026, 8, 10), result.ecbRateDate)
        assertEquals(YearMonth.of(2026, 8), result.inforEuroRateMonth)
        assertEquals(CurrencyRateSupport.Daily, result.currencyRateSupport("USD"))
        assertEquals(CurrencyRateSupport.Monthly, result.currencyRateSupport("TWD"))
        assertEquals(CurrencyRateSupport.Unavailable, result.currencyRateSupport("AED"))
    }

    @Test
    fun `uses InforEuro when ECB is unavailable`() = runTest {
        val inforEuro = snapshot(
            date = LocalDate.of(2026, 8, 1),
            rates = mapOf("EUR" to 1.0, "AED" to 4.212),
            coverage = ExchangeRateCoverage.InforEuroMonthly,
        )
        val source = OfficialExchangeRateSource(
            ecbSource = ExchangeRateSource { throw IOException("offline") },
            inforEuroSource = ExchangeRateSource { inforEuro },
        )

        assertEquals(inforEuro, source.fetch())
    }

    @Test
    fun `keeps both failures when official sources are unavailable`() = runTest {
        val source = OfficialExchangeRateSource(
            ecbSource = ExchangeRateSource { throw IOException("offline") },
            inforEuroSource = ExchangeRateSource { error("invalid response") },
        )

        try {
            source.fetch()
            fail("Expected official source failure")
        } catch (error: ExchangeRateSourcesUnavailableException) {
            assertEquals(2, error.failures.size)
        }
    }

    @Test
    fun `refreshes a combined legacy cache that lacks source coverage details`() {
        val now = Instant.parse("2026-08-11T01:00:00Z")
        val legacy = snapshot(
            date = LocalDate.of(2026, 8, 10),
            rates = mapOf("EUR" to 1.0, "USD" to 1.15, "TWD" to 37.1),
            coverage = ExchangeRateCoverage.EcbDailyWithInforEuroMonthly,
        )

        assertEquals(false, legacy.isFresh(now))
        assertEquals(true, legacy.copy(ecbCurrencies = setOf("EUR", "USD")).isFresh(now))
    }

    private fun snapshot(
        date: LocalDate,
        rates: Map<String, Double>,
        coverage: ExchangeRateCoverage = ExchangeRateCoverage.EcbDaily,
    ) = ExchangeRateSnapshot(
        rateDate = date,
        ratesPerEuro = rates,
        fetchedAt = Instant.parse("2026-08-11T00:00:00Z"),
        coverage = coverage,
    )
}
