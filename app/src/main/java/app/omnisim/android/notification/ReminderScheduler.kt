package app.omnisim.android.notification

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {
    fun schedule() {
        val request = PeriodicWorkRequestBuilder<RenewalWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(4, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun checkNow() {
        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<RenewalWorker>().build(),
        )
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "omnisim-daily-renewal-check"
        private const val IMMEDIATE_WORK_NAME = "omnisim-immediate-renewal-check"
    }
}
