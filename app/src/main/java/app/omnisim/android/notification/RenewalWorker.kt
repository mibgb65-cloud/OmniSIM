package app.omnisim.android.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.omnisim.android.OmniSimApplication
import app.omnisim.android.data.local.entity.ReminderStateEntity
import app.omnisim.android.data.local.entity.SimEntity
import app.omnisim.android.data.preferences.ReminderCheckResult
import app.omnisim.android.domain.util.daysUntilRenewal
import app.omnisim.android.domain.util.effectiveReminderOffsets
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.first

class RenewalWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val application = applicationContext as OmniSimApplication
        val settingsRepository = application.container.settingsRepository
        return try {
            val helper = NotificationHelper(applicationContext)
            if (!helper.notificationsEnabled()) {
                settingsRepository.recordReminderCheck(
                    Instant.now(),
                    ReminderCheckResult.NotificationsBlocked,
                    scannedCount = 0,
                    sentCount = 0,
                )
                return Result.success()
            }

            val dao = application.container.database.dao()
            val settings = settingsRepository.settings.first()
            val today = LocalDate.now()
            val sent = dao.getAllReminderStates().map {
                ReminderKey(it.simId, it.renewalDate, it.reminderOffset)
            }.toSet()
            val sims = dao.getActiveSims().filter(SimEntity::remindersEnabled)
            var sentCount = 0

            sims.forEach { sim ->
                val remaining = daysUntilRenewal(today, sim.nextRenewalDate)
                val offsets = effectiveReminderOffsets(sim.reminderOffsets, settings.reminderOffsets)
                val offset = matchedReminderOffset(remaining, offsets) ?: return@forEach
                val key = ReminderKey(sim.id, sim.nextRenewalDate, offset)
                if (shouldSendReminder(key, sent) && helper.show(sim, remaining, settings.maskPhoneNumbers)) {
                    dao.insertReminderState(
                        ReminderStateEntity(sim.id, sim.nextRenewalDate, offset, Instant.now()),
                    )
                    sentCount += 1
                }
            }
            settingsRepository.recordReminderCheck(
                Instant.now(),
                ReminderCheckResult.Success,
                scannedCount = sims.size,
                sentCount = sentCount,
            )
            Result.success()
        } catch (_: Exception) {
            runCatching {
                settingsRepository.recordReminderCheck(
                    Instant.now(),
                    ReminderCheckResult.Failed,
                    scannedCount = 0,
                    sentCount = 0,
                )
            }
            Result.retry()
        }
    }
}
