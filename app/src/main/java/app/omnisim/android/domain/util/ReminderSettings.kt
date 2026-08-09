package app.omnisim.android.domain.util

val SupportedReminderOffsets: Set<Int> = linkedSetOf(30, 14, 7, 3, 1, 0, -1)

fun effectiveReminderOffsets(
    simOffsets: Set<Int>?,
    defaultOffsets: Set<Int>,
): Set<Int> = simOffsets ?: defaultOffsets

fun areReminderOffsetsValid(offsets: Set<Int>?): Boolean =
    offsets == null || offsets.all(SupportedReminderOffsets::contains)
