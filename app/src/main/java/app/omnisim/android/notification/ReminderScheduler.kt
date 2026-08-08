package app.omnisim.android.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
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

    companion object {
        private const val UNIQUE_WORK_NAME = "omnisim-daily-renewal-check"
    }
}
