package app.omnisim.android.data.exchange

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.YearMonth
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private const val INFOR_EURO_MONTHLY_RATES_URL =
    "https://ec.europa.eu/budg/inforeuro/api/public/monthly-rates"
private val inforEuroJson = Json { ignoreUnknownKeys = true }
private val currencyCodePattern = Regex("[A-Z]{3}")

class InforEuroExchangeRateSource(
    private val endpoint: String = INFOR_EURO_MONTHLY_RATES_URL,
    private val currentMonth: () -> YearMonth = { YearMonth.now() },
) : ExchangeRateSource {
    override suspend fun fetch(): ExchangeRateSnapshot {
        val month = currentMonth()
        val json = fetchExchangeRateDocument(
            "$endpoint?year=${month.year}&month=${month.monthValue}",
            "application/json",
        )
        val parsed = parseInforEuroExchangeRates(json, month)
        return ExchangeRateSnapshot(
            rateDate = parsed.rateDate,
            ratesPerEuro = parsed.ratesPerEuro,
            fetchedAt = Instant.now(),
            coverage = ExchangeRateCoverage.InforEuroMonthly,
            inforEuroRateMonth = month,
            inforEuroCurrencies = parsed.ratesPerEuro.keys,
        )
    }
}

class OfficialExchangeRateSource(
    private val ecbSource: ExchangeRateSource = EcbExchangeRateSource(),
    private val inforEuroSource: ExchangeRateSource = InforEuroExchangeRateSource(),
) : ExchangeRateSource {
    override suspend fun fetch(): ExchangeRateSnapshot = coroutineScope {
        val ecbRequest = async { fetchResult(ecbSource) }
        val inforEuroRequest = async { fetchResult(inforEuroSource) }
        val ecbResult = ecbRequest.await()
        val inforEuroResult = inforEuroRequest.await()
        val ecb = ecbResult.getOrNull()
        val inforEuro = inforEuroResult.getOrNull()

        when {
            ecb != null && inforEuro != null -> ExchangeRateSnapshot(
                rateDate = maxOf(ecb.rateDate, inforEuro.rateDate),
                ratesPerEuro = inforEuro.ratesPerEuro + ecb.ratesPerEuro,
                fetchedAt = maxOf(ecb.fetchedAt, inforEuro.fetchedAt),
                coverage = ExchangeRateCoverage.EcbDailyWithInforEuroMonthly,
                ecbRateDate = ecb.ecbRateDate ?: ecb.rateDate,
                inforEuroRateMonth = inforEuro.inforEuroRateMonth ?: YearMonth.from(
                    inforEuro.rateDate,
                ),
                ecbCurrencies = ecb.ratesPerEuro.keys,
                inforEuroCurrencies = inforEuro.ratesPerEuro.keys,
            )
            ecb != null -> ecb
            inforEuro != null -> inforEuro
            else -> throw ExchangeRateSourcesUnavailableException(
                listOfNotNull(ecbResult.exceptionOrNull(), inforEuroResult.exceptionOrNull()),
            )
        }
    }
}

internal class ExchangeRateSourcesUnavailableException(
    val failures: List<Throwable>,
) : IOException("Official exchange-rate sources are unavailable", failures.firstOrNull())

private suspend fun fetchResult(source: ExchangeRateSource): Result<ExchangeRateSnapshot> = try {
    Result.success(source.fetch())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    Result.failure(error)
}

internal suspend fun fetchExchangeRateDocument(endpoint: String, accept: String): String =
    withContext(Dispatchers.IO) {
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", accept)
            connection.setRequestProperty("User-Agent", "OmniSIM/1.0")
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IOException("Exchange-rate request failed: HTTP $responseCode")
            }
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

@Serializable
private data class InforEuroRate(
    val isoA3Code: String? = null,
    val value: Double? = null,
)

internal fun parseInforEuroExchangeRates(
    response: String,
    month: YearMonth,
): ParsedExchangeRates {
    val rates = inforEuroJson.decodeFromString<List<InforEuroRate>>(response)
        .mapNotNull { item ->
            val currency = item.isoA3Code
                ?.trim()
                ?.uppercase(Locale.ROOT)
                ?.takeIf { it.matches(currencyCodePattern) }
                ?: return@mapNotNull null
            val rate = item.value?.takeIf { it.isFinite() && it > 0.0 }
                ?: return@mapNotNull null
            currency to rate
        }
        .toMap()
        .toMutableMap()
        .apply { put("EUR", 1.0) }

    require(rates.size > 1) { "InforEuro exchange rates are empty" }
    return ParsedExchangeRates(month.atDay(1), rates)
}
