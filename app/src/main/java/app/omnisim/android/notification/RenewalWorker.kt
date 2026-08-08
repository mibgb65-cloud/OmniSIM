package app.omnisim.android.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.omnisim.android.OmniSimApplication
import app.omnisim.android.data.local.entity.ReminderStateEntity
import app.omnisim.android.domain.util.daysUntilRenewal
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.first

class RenewalWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runCatching {
        val application = applicationContext as OmniSimApplication
        val helper = NotificationHelper(applicationContext)
        if (!helper.notificationsEnabled()) return Result.success()

        val dao = application.container.database.dao()
        val settings = application.container.settingsRepository.settings.first()
        val today = LocalDate.now()
        val sent = dao.getAllReminderStates().map {
            ReminderKey(it.simId, it.renewalDate, it.reminderOffset)
        }.toSet()

        dao.getActiveSims().forEach { sim ->
            val remaining = daysUntilRenewal(today, sim.nextRenewalDate)
            val offset = matchedReminderOffset(remaining, settings.reminderOffsets) ?: return@forEach
            val key = ReminderKey(sim.id, sim.nextRenewalDate, offset)
            if (shouldSendReminder(key, sent)) {
                if (helper.show(sim, remaining, settings.maskPhoneNumbers)) {
                    dao.insertReminderState(
                        ReminderStateEntity(sim.id, sim.nextRenewalDate, offset, Instant.now()),
                    )
                }
            }
        }
        Result.success()
    }.getOrElse { Result.retry() }
}
