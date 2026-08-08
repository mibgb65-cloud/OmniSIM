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

fun currencyOptions(locale: Locale): List<CurrencyOption> {
    val localCurrency = runCatching { Currency.getInstance(locale).currencyCode }.getOrNull()
    val preferredCodes = listOfNotNull(localCurrency) + commonCurrencyCodes.filterNot { it == localCurrency }
    val preferredIndexes = preferredCodes.withIndex().associate { it.value to it.index }

    return Currency.getAvailableCurrencies()
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

fun isSupportedCurrencyCode(code: String): Boolean {
    val normalized = code.trim().uppercase(Locale.ROOT)
    if (normalized.length != 3 || normalized == "XXX") return false
    return runCatching { Currency.getInstance(normalized) }.isSuccess
}
