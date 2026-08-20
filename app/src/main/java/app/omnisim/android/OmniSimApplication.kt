package app.omnisim.android

import android.app.Application
import app.omnisim.android.backup.BackupManager
import app.omnisim.android.data.exchange.ExchangeRateRepository
import app.omnisim.android.data.local.OmniSimDatabase
import app.omnisim.android.data.preferences.SettingsRepository
import app.omnisim.android.data.repository.SimRepository
import app.omnisim.android.data.update.AppUpdateRepository
import app.omnisim.android.data.update.hasMatchingUpdateSignature
import app.omnisim.android.notification.NotificationHelper
import app.omnisim.android.notification.ReminderScheduler
import java.io.File

class OmniSimApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notificationHelper.createChannel()
        container.reminderScheduler.schedule()
    }
}

class AppContainer(application: Application) {
    val database = OmniSimDatabase.create(application)
    val simRepository = SimRepository(database)
    val settingsRepository = SettingsRepository(application)
    val exchangeRateRepository = ExchangeRateRepository(application)
    val appUpdateRepository = AppUpdateRepository(
        cacheDirectory = File(application.cacheDir, "updates"),
        apkVerifier = { hasMatchingUpdateSignature(application, it) },
    )
    val notificationHelper = NotificationHelper(application)
    val backupManager = BackupManager(
        contentResolver = application.contentResolver,
        database = database,
        settingsRepository = settingsRepository,
        recoveryFile = File(application.filesDir, "recovery/last-restore-snapshot.json"),
    )
    val reminderScheduler = ReminderScheduler(application)
}
