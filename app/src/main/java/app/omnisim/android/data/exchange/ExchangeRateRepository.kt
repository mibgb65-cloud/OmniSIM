package app.omnisim.android.data.exchange

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val ECB_DAILY_RATES_URL =
    "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml"
private val CACHE_MAX_AGE: Duration = Duration.ofHours(12)
private val Context.exchangeRatesDataStore by preferencesDataStore(name = "exchange_rates")

data class ExchangeRateSnapshot(
    val rateDate: LocalDate,
    val ratesPerEuro: Map<String, Double>,
    val fetchedAt: Instant,
    val coverage: ExchangeRateCoverage = ExchangeRateCoverage.EcbDaily,
    val ecbRateDate: LocalDate? = null,
    val inforEuroRateMonth: YearMonth? = null,
    val ecbCurrencies: Set<String> = emptySet(),
    val inforEuroCurrencies: Set<String> = emptySet(),
)

enum class ExchangeRateCoverage {
    EcbDaily,
    EcbDailyWithInforEuroMonthly,
    InforEuroMonthly,
}

enum class CurrencyRateSupport {
    Daily,
    Monthly,
    Unavailable,
}

internal data class ParsedExchangeRates(
    val rateDate: LocalDate,
    val ratesPerEuro: Map<String, Double>,
)

fun ExchangeRateSnapshot.isFresh(now: Instant = Instant.now()): Boolean =
    fetchedAt.plus(CACHE_MAX_AGE).isAfter(now) &&
        (coverage != ExchangeRateCoverage.EcbDailyWithInforEuroMonthly ||
            ecbCurrencies.isNotEmpty())

fun ExchangeRateSnapshot.currencyRateSupport(currencyCode: String): CurrencyRateSupport {
    val code = currencyCode.trim().uppercase(Locale.ROOT)
    return when {
        code in ecbCurrencies -> CurrencyRateSupport.Daily
        code in inforEuroCurrencies -> CurrencyRateSupport.Monthly
        code !in ratesPerEuro -> CurrencyRateSupport.Unavailable
        coverage == ExchangeRateCoverage.EcbDaily -> CurrencyRateSupport.Daily
        else -> CurrencyRateSupport.Monthly
    }
}

fun interface ExchangeRateSource {
    suspend fun fetch(): ExchangeRateSnapshot
}

class EcbExchangeRateSource(
    private val endpoint: String = ECB_DAILY_RATES_URL,
) : ExchangeRateSource {
    override suspend fun fetch(): ExchangeRateSnapshot {
        val xml = fetchExchangeRateDocument(endpoint, "application/xml,text/xml")
        val parsed = parseEcbExchangeRates(xml)
        return ExchangeRateSnapshot(
            rateDate = parsed.rateDate,
            ratesPerEuro = parsed.ratesPerEuro,
            fetchedAt = Instant.now(),
            coverage = ExchangeRateCoverage.EcbDaily,
            ecbRateDate = parsed.rateDate,
            ecbCurrencies = parsed.ratesPerEuro.keys,
        )
    }
}

class ExchangeRateRepository(
    private val context: Context,
    private val source: ExchangeRateSource = OfficialExchangeRateSource(),
) {
    private object Keys {
        val snapshot = stringPreferencesKey("snapshot")
    }

    private val json = Json { ignoreUnknownKeys = true }

    val snapshots: Flow<ExchangeRateSnapshot?> = context.exchangeRatesDataStore.data.map { preferences ->
        preferences[Keys.snapshot]?.let(::decodeSnapshot)
    }

    suspend fun getCachedSnapshot(): ExchangeRateSnapshot? = snapshots.first()

    suspend fun refreshIfStale(now: Instant = Instant.now()): ExchangeRateSnapshot {
        val cached = getCachedSnapshot()
        if (cached != null && cached.isFresh(now)) return cached

        return refresh()
    }

    suspend fun refresh(): ExchangeRateSnapshot {
        val fresh = source.fetch()
        context.exchangeRatesDataStore.edit { preferences ->
            preferences[Keys.snapshot] = json.encodeToString(fresh.toCached())
        }
        return fresh
    }

    private fun decodeSnapshot(value: String): ExchangeRateSnapshot? = runCatching {
        json.decodeFromString<CachedExchangeRateSnapshot>(value).toSnapshot()
    }.getOrNull()
}

@Serializable
private data class CachedExchangeRateSnapshot(
    val rateDate: String,
    val ratesPerEuro: Map<String, Double>,
    val fetchedAtEpochMillis: Long,
    val coverage: String = ExchangeRateCoverage.EcbDaily.name,
    val ecbRateDate: String? = null,
    val inforEuroRateMonth: String? = null,
    val ecbCurrencies: Set<String> = emptySet(),
    val inforEuroCurrencies: Set<String> = emptySet(),
)

private fun ExchangeRateSnapshot.toCached() = CachedExchangeRateSnapshot(
    rateDate = rateDate.toString(),
    ratesPerEuro = ratesPerEuro,
    fetchedAtEpochMillis = fetchedAt.toEpochMilli(),
    coverage = coverage.name,
    ecbRateDate = ecbRateDate?.toString(),
    inforEuroRateMonth = inforEuroRateMonth?.toString(),
    ecbCurrencies = ecbCurrencies,
    inforEuroCurrencies = inforEuroCurrencies,
)

private fun CachedExchangeRateSnapshot.toSnapshot(): ExchangeRateSnapshot {
    val validRates = ratesPerEuro
        .mapKeys { (currency, _) -> currency.uppercase(Locale.ROOT) }
        .filterValues { rate -> rate.isFinite() && rate > 0.0 }
        .toMutableMap()
        .apply { put("EUR", 1.0) }
    require(validRates.size > 1) { "Cached exchange rates are empty" }
    val parsedDate = LocalDate.parse(rateDate)
    val parsedCoverage = runCatching { ExchangeRateCoverage.valueOf(coverage) }
        .getOrDefault(ExchangeRateCoverage.EcbDaily)
    val hasSourceMetadata = ecbRateDate != null || inforEuroRateMonth != null ||
        ecbCurrencies.isNotEmpty() || inforEuroCurrencies.isNotEmpty()
    val validCodes = validRates.keys
    val migratedEcbCurrencies = if (hasSourceMetadata) {
        ecbCurrencies.map { it.uppercase(Locale.ROOT) }.toSet() intersect validCodes
    } else if (parsedCoverage == ExchangeRateCoverage.EcbDaily) {
        validCodes
    } else {
        emptySet()
    }
    val migratedInforEuroCurrencies = if (hasSourceMetadata) {
        inforEuroCurrencies.map { it.uppercase(Locale.ROOT) }.toSet() intersect validCodes
    } else if (parsedCoverage != ExchangeRateCoverage.EcbDaily) {
        validCodes
    } else {
        emptySet()
    }
    return ExchangeRateSnapshot(
        rateDate = parsedDate,
        ratesPerEuro = validRates,
        fetchedAt = Instant.ofEpochMilli(fetchedAtEpochMillis),
        coverage = parsedCoverage,
        ecbRateDate = ecbRateDate?.let(LocalDate::parse)
            ?: parsedDate.takeIf { parsedCoverage != ExchangeRateCoverage.InforEuroMonthly },
        inforEuroRateMonth = inforEuroRateMonth?.let(YearMonth::parse)
            ?: YearMonth.from(parsedDate).takeIf {
                parsedCoverage != ExchangeRateCoverage.EcbDaily
            },
        ecbCurrencies = migratedEcbCurrencies,
        inforEuroCurrencies = migratedInforEuroCurrencies,
    )
}

internal fun parseEcbExchangeRates(xml: String): ParsedExchangeRates {
    val time = Regex("""\btime\s*=\s*["'](\d{4}-\d{2}-\d{2})["']""")
        .find(xml)
        ?.groupValues
        ?.get(1)
        ?: throw IllegalArgumentException("ECB rate date is missing")
    val currencyAttribute = Regex("""\bcurrency\s*=\s*["']([A-Za-z]{3})["']""")
    val rateAttribute = Regex("""\brate\s*=\s*["']([0-9]+(?:\.[0-9]+)?)["']""")
    val rates = mutableMapOf("EUR" to 1.0)

    Regex("""<Cube\b[^>]*>""").findAll(xml).forEach { cube ->
        val currency = currencyAttribute.find(cube.value)
            ?.groupValues
            ?.get(1)
            ?.uppercase(Locale.ROOT)
            ?: return@forEach
        val rate = rateAttribute.find(cube.value)
            ?.groupValues
            ?.get(1)
            ?.toDoubleOrNull()
            ?.takeIf { it.isFinite() && it > 0.0 }
            ?: return@forEach
        rates[currency] = rate
    }

    require(rates.size > 1) { "ECB exchange rates are empty" }
    return ParsedExchangeRates(LocalDate.parse(time), rates)
}
