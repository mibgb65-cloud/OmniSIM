package app.omnisim.android.domain.util

import java.util.Locale

data class CurrencyAmount(
    val currency: String,
    val amount: Double,
)

data class ConvertedCostTotal(
    val daily: Double,
    val includedCount: Int,
    val excludedCurrencies: Set<String>,
)

fun calculateDailyHoldingCost(
    renewalPrice: Double?,
    renewalCycleDays: Int?,
    renewalDayOfMonth: Int? = null,
): Double? {
    if (renewalPrice == null || !renewalPrice.isFinite() || renewalPrice < 0) return null
    if (renewalCycleDays != null && renewalDayOfMonth != null) return null
    val periodDays = when {
        renewalCycleDays != null && renewalCycleDays > 0 -> renewalCycleDays.toDouble()
        renewalDayOfMonth != null && renewalDayOfMonth in 1..31 -> 365.2425 / 12.0
        else -> return null
    }
    return renewalPrice / periodDays
}

fun convertCurrencyAmount(
    amount: Double,
    fromCurrency: String,
    toCurrency: String,
    ratesPerEuro: Map<String, Double>,
): Double? {
    if (!amount.isFinite() || amount < 0.0) return null
    val from = fromCurrency.trim().uppercase(Locale.ROOT)
    val to = toCurrency.trim().uppercase(Locale.ROOT)
    if (from.isBlank() || to.isBlank()) return null
    if (from == to) return amount

    val fromRate = if (from == "EUR") 1.0 else ratesPerEuro[from]
    val toRate = if (to == "EUR") 1.0 else ratesPerEuro[to]
    if (fromRate == null || toRate == null) return null
    if (!fromRate.isFinite() || fromRate <= 0.0 || !toRate.isFinite() || toRate <= 0.0) {
        return null
    }
    return amount / fromRate * toRate
}

fun calculateConvertedCostTotal(
    costs: List<CurrencyAmount>,
    targetCurrency: String,
    ratesPerEuro: Map<String, Double>,
): ConvertedCostTotal {
    val target = targetCurrency.trim().uppercase(Locale.ROOT)
    var daily = 0.0
    var includedCount = 0
    val excludedCurrencies = mutableSetOf<String>()

    costs.forEach { cost ->
        val currency = cost.currency.trim().uppercase(Locale.ROOT)
        val converted = convertCurrencyAmount(cost.amount, currency, target, ratesPerEuro)
        if (converted == null) {
            excludedCurrencies += currency
        } else {
            daily += converted
            includedCount += 1
        }
    }

    return ConvertedCostTotal(
        daily = daily,
        includedCount = includedCount,
        excludedCurrencies = excludedCurrencies,
    )
}
