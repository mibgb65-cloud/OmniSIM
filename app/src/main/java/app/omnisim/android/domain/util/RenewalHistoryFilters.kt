package app.omnisim.android.domain.util

import app.omnisim.android.data.local.entity.RenewalHistoryEntity
import java.time.LocalDate

enum class RenewalHistoryRange(val days: Long?) {
    All(null),
    Last30Days(30),
    Last90Days(90),
    LastYear(365),
}

fun filterRenewalHistory(
    history: List<RenewalHistoryEntity>,
    simId: String?,
    range: RenewalHistoryRange,
    today: LocalDate,
): List<RenewalHistoryEntity> {
    val earliestDate = range.days?.let(today::minusDays)
    return history
        .asSequence()
        .filter { simId == null || it.simId == simId }
        .filter { earliestDate == null || !it.renewalDate.isBefore(earliestDate) }
        .sortedWith(
            compareByDescending<RenewalHistoryEntity>(RenewalHistoryEntity::renewalDate)
                .thenByDescending(RenewalHistoryEntity::createdAt),
        )
        .toList()
}
