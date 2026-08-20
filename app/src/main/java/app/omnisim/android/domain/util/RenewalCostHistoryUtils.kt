package app.omnisim.android.domain.util

import app.omnisim.android.data.local.entity.RenewalHistoryEntity
import java.time.LocalDate
import java.util.Locale

data class ActualSpendByCurrency(
    val currency: String,
    val last30Days: Double,
    val last365Days: Double,
)

data class ActualSpendSummary(
    val byCurrency: List<ActualSpendByCurrency>,
    val incompleteRecordCount: Int,
)

data class RenewalPriceChange(
    val currency: String,
    val latestAmount: Double,
    val previousAmount: Double,
    val percentageChange: Double?,
)

fun calculateActualSpend(
    history: List<RenewalHistoryEntity>,
    activeSimIds: Set<String>,
    today: LocalDate,
): ActualSpendSummary {
    val earliest30Days = today.minusDays(30)
    val earliest365Days = today.minusDays(365)
    val totals = mutableMapOf<String, SpendAccumulator>()
    var incompleteRecordCount = 0

    history.asSequence()
        .filter { it.simId in activeSimIds }
        .filter { !it.renewalDate.isAfter(today) && !it.renewalDate.isBefore(earliest365Days) }
        .forEach { renewal ->
            val amount = renewal.amount?.takeIf { it.isFinite() && it >= 0.0 }
            val currency = renewal.currency
                ?.trim()
                ?.uppercase(Locale.ROOT)
                ?.takeIf(String::isNotEmpty)
            if (amount == null || currency == null) {
                incompleteRecordCount += 1
                return@forEach
            }
            val total = totals.getOrPut(currency, ::SpendAccumulator)
            total.last365Days += amount
            if (!renewal.renewalDate.isBefore(earliest30Days)) total.last30Days += amount
        }

    return ActualSpendSummary(
        byCurrency = totals.map { (currency, total) ->
            ActualSpendByCurrency(currency, total.last30Days, total.last365Days)
        }.sortedBy(ActualSpendByCurrency::currency),
        incompleteRecordCount = incompleteRecordCount,
    )
}

fun calculateLatestRenewalPriceChange(
    history: List<RenewalHistoryEntity>,
    simId: String,
): RenewalPriceChange? {
    val recordedAmounts = history.asSequence()
        .filter { it.simId == simId }
        .mapNotNull { renewal ->
            val amount = renewal.amount?.takeIf { it.isFinite() && it >= 0.0 }
                ?: return@mapNotNull null
            val currency = renewal.currency
                ?.trim()
                ?.uppercase(Locale.ROOT)
                ?.takeIf(String::isNotEmpty)
                ?: return@mapNotNull null
            RecordedRenewalAmount(renewal, amount, currency)
        }
        .sortedWith(
            compareByDescending<RecordedRenewalAmount> { it.renewal.createdAt }
                .thenByDescending { it.renewal.renewalDate },
        )
        .toList()
    val latest = recordedAmounts.firstOrNull() ?: return null
    val previous = recordedAmounts.drop(1).firstOrNull { it.currency == latest.currency }
        ?: return null
    return RenewalPriceChange(
        currency = latest.currency,
        latestAmount = latest.amount,
        previousAmount = previous.amount,
        percentageChange = previous.amount.takeIf { it > 0.0 }
            ?.let { (latest.amount - it) / it * 100.0 },
    )
}

private data class SpendAccumulator(
    var last30Days: Double = 0.0,
    var last365Days: Double = 0.0,
)

private data class RecordedRenewalAmount(
    val renewal: RenewalHistoryEntity,
    val amount: Double,
    val currency: String,
)
