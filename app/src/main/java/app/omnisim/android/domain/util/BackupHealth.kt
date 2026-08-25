package app.omnisim.android.domain.util

import java.time.Duration
import java.time.Instant

private val MAX_BACKUP_AGE: Duration = Duration.ofDays(30)

fun isBackupRecommended(lastBackupAt: Instant?, backupDirty: Boolean, now: Instant): Boolean =
    backupDirty || lastBackupAt == null || lastBackupAt.plus(MAX_BACKUP_AGE).isBefore(now)
