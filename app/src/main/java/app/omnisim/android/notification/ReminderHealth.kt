package app.omnisim.android.notification

import java.time.Duration
import java.time.Instant

private val HEALTHY_CHECK_AGE: Duration = Duration.ofHours(48)

fun isReminderCheckFresh(lastCheckAt: Instant?, now: Instant): Boolean =
    lastCheckAt != null && !lastCheckAt.plus(HEALTHY_CHECK_AGE).isBefore(now)
