package app.omnisim.android.domain.util

import app.omnisim.android.domain.model.RenewalStatus
import java.time.LocalDate
import java.time.temporal.ChronoUnit

fun daysUntilRenewal(today: LocalDate, renewalDate: LocalDate): Long =
    ChronoUnit.DAYS.between(today, renewalDate)

fun calculateNextRenewalDate(renewalDate: LocalDate, cycleDays: Int): LocalDate {
    require(cycleDays > 0) { "Renewal cycle must be positive" }
    return renewalDate.plusDays(cycleDays.toLong())
}

fun calculateRenewalStatus(
    today: LocalDate,
    renewalDate: LocalDate,
    warningPeriodDays: Int,
    archived: Boolean,
): RenewalStatus {
    require(warningPeriodDays >= 0) { "Warning period cannot be negative" }
    if (archived) return RenewalStatus.Archived
    val remaining = daysUntilRenewal(today, renewalDate)
    return when {
        remaining < 0 -> RenewalStatus.Overdue
        remaining == 0L -> RenewalStatus.DueToday
        remaining <= warningPeriodDays -> RenewalStatus.DueSoon
        else -> RenewalStatus.Active
    }
}

