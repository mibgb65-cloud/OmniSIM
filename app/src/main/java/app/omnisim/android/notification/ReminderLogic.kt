package app.omnisim.android.notification

import java.time.LocalDate

data class ReminderKey(
    val simId: String,
    val renewalDate: LocalDate,
    val reminderOffset: Int,
)

fun matchedReminderOffset(daysRemaining: Long, enabledOffsets: Set<Int>): Int? = when {
    daysRemaining < 0 && -1 in enabledOffsets -> -1
    daysRemaining < 0 -> null
    daysRemaining in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() -> enabledOffsets
        .asSequence()
        .filter { it >= 0 && daysRemaining <= it.toLong() }
        .minOrNull()
    else -> null
}

fun shouldSendReminder(key: ReminderKey, sent: Set<ReminderKey>): Boolean = key !in sent
