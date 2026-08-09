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

fun calculateNextMonthlyRenewalDate(
    renewalDate: LocalDate,
    dayOfMonth: Int,
): LocalDate {
    require(dayOfMonth in 1..31) { "Monthly renewal day must be between 1 and 31" }

    fun dateInMonth(date: LocalDate): LocalDate =
        date.withDayOfMonth(dayOfMonth.coerceAtMost(date.lengthOfMonth()))

    val thisMonth = dateInMonth(renewalDate)
    return if (thisMonth.isAfter(renewalDate)) {
        thisMonth
    } else {
        dateInMonth(renewalDate.plusMonths(1))
    }
}

fun calculateScheduledNextRenewalDate(
    renewalDate: LocalDate,
    cycleDays: Int?,
    dayOfMonth: Int?,
): LocalDate? {
    require(cycleDays == null || dayOfMonth == null) {
        "A renewal schedule cannot use both a day cycle and a monthly day"
    }
    return when {
        cycleDays != null -> calculateNextRenewalDate(renewalDate, cycleDays)
        dayOfMonth != null -> calculateNextMonthlyRenewalDate(renewalDate, dayOfMonth)
        else -> null
    }
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
