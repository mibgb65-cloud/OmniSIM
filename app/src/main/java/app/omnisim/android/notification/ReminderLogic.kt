package app.omnisim.android.notification

import java.time.LocalDate

data class ReminderKey(
    val simId: String,
    val renewalDate: LocalDate,
    val reminderOffset: Int,
)

fun matchedReminderOffset(daysRemaining: Long, enabledOffsets: Set<Int>): Int? = when {
    daysRemaining < 0 && -1 in enabledOffsets -> -1
    daysRemaining in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() &&
        daysRemaining.toInt() in enabledOffsets -> daysRemaining.toInt()
    else -> null
}

fun shouldSendReminder(key: ReminderKey, sent: Set<ReminderKey>): Boolean = key !in sent

