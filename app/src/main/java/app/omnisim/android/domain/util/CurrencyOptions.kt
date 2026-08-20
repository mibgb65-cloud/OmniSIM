package app.omnisim.android.domain.util

import java.util.Currency
import java.util.Locale

data class CurrencyOption(
    val code: String,
    val symbol: String,
    val displayName: String,
    val englishName: String,
)

private val commonCurrencyCodes = listOf(
    "USD", "EUR", "GBP", "CNY", "JPY", "SGD", "HKD", "AUD", "CAD", "MYR",
    "THB", "TWD", "KRW", "INR", "IDR", "PHP", "VND", "AED", "SAR", "TRY",
)
private val knownOfficialCurrencyCodes = setOf(
    "AED", "AFN", "ALL", "AMD", "AOA", "ARS", "AUD", "AWG", "AZN", "BAM",
    "BBD", "BDT", "BHD", "BIF", "BMD", "BND", "BOB", "BRL", "BSD", "BTN",
    "BWP", "BYN", "BZD", "CAD", "CDF", "CHF", "CLP", "CNY", "COP", "CRC",
    "CUP", "CVE", "CZK", "DJF", "DKK", "DOP", "DZD", "EGP", "ERN", "ETB",
    "EUR", "FJD", "FKP", "GBP", "GEL", "GHS", "GIP", "GMD", "GNF", "GTQ",
    "GYD", "HKD", "HNL", "HTG", "HUF", "IDR", "ILS", "INR", "IQD", "IRR",
    "ISK", "JMD", "JOD", "JPY", "KES", "KGS", "KHR", "KMF", "KRW", "KWD",
    "KYD", "KZT", "LAK", "LBP", "LKR", "LRD", "LSL", "LYD", "MAD", "MDL",
    "MGA", "MKD", "MMK", "MNT", "MOP", "MRU", "MUR", "MVR", "MWK", "MXN",
    "MYR", "MZN", "NAD", "NGN", "NIO", "NOK", "NPR", "NZD", "OMR", "PAB",
    "PEN", "PGK", "PHP", "PKR", "PLN", "PYG", "QAR", "RON", "RSD", "RUB",
    "RWF", "SAR", "SBD", "SCR", "SDG", "SEK", "SGD", "SHP", "SLE", "SOS",
    "SRD", "SSP", "STN", "SYP", "SZL", "THB", "TJS", "TMT", "TND", "TOP",
    "TRY", "TTD", "TWD", "TZS", "UAH", "UGX", "USD", "UYU", "UZS", "VES",
    "VND", "VUV", "WST", "XAF", "XCD", "XCG", "XOF", "XPF", "YER", "ZAR",
    "ZIG", "ZMW",
)
private val currencyCodePattern = Regex("[A-Z]{3}")

fun currencyOptions(
    locale: Locale,
    officialCurrencyCodes: Set<String> = emptySet(),
): List<CurrencyOption> {
    val localCurrency = runCatching { Currency.getInstance(locale).currencyCode }.getOrNull()
    val preferredCodes = listOfNotNull(localCurrency) + commonCurrencyCodes.filterNot { it == localCurrency }
    val preferredIndexes = preferredCodes.withIndex().associate { it.value to it.index }
    val runtimeOptions = Currency.getAvailableCurrencies()
        .asSequence()
        .filterNot { it.currencyCode == "XXX" }
        .map { currency ->
            CurrencyOption(
                code = currency.currencyCode,
                symbol = currency.getSymbol(locale),
                displayName = currency.getDisplayName(locale),
                englishName = currency.getDisplayName(Locale.ENGLISH),
            )
        }
        .toList()
    val runtimeCodes = runtimeOptions.mapTo(mutableSetOf(), CurrencyOption::code)
    val supplementalOptions = (knownOfficialCurrencyCodes + officialCurrencyCodes)
        .asSequence()
        .mapNotNull(::normalizeCurrencyCode)
        .filterNot(runtimeCodes::contains)
        .distinct()
        .map { code ->
            CurrencyOption(
                code = code,
                symbol = code,
                displayName = code,
                englishName = code,
            )
        }
        .toList()

    return (runtimeOptions + supplementalOptions)
        .sortedWith(
            compareBy<CurrencyOption> { preferredIndexes[it.code] ?: Int.MAX_VALUE }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.displayName }
                .thenBy(CurrencyOption::code),
        )
        .toList()
}

fun filterCurrencyOptions(
    currencies: List<CurrencyOption>,
    query: String,
): List<CurrencyOption> {
    val normalized = query.trim().lowercase(Locale.ROOT)
    if (normalized.isBlank()) return currencies

    return currencies.filter { currency ->
        currency.code.lowercase(Locale.ROOT).contains(normalized) ||
            currency.symbol.lowercase(Locale.ROOT).contains(normalized) ||
            currency.displayName.lowercase(Locale.ROOT).contains(normalized) ||
            currency.englishName.lowercase(Locale.ROOT).contains(normalized)
    }
}

fun isSupportedCurrencyCode(
    code: String,
    officialCurrencyCodes: Set<String> = emptySet(),
): Boolean {
    val normalized = normalizeCurrencyCode(code) ?: return false
    val trustedOfficialCodes = (knownOfficialCurrencyCodes + officialCurrencyCodes)
        .mapNotNull(::normalizeCurrencyCode)
    return normalized in trustedOfficialCodes ||
        runCatching { Currency.getInstance(normalized) }.isSuccess
}

private fun normalizeCurrencyCode(code: String): String? =
    code.trim()
        .uppercase(Locale.ROOT)
        .takeIf { it != "XXX" && currencyCodePattern.matches(it) }
