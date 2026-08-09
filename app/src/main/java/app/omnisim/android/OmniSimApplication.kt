package app.omnisim.android

import android.app.Application
import app.omnisim.android.backup.BackupManager
import app.omnisim.android.data.exchange.ExchangeRateRepository
import app.omnisim.android.data.local.OmniSimDatabase
import app.omnisim.android.data.preferences.SettingsRepository
import app.omnisim.android.data.repository.SimRepository
import app.omnisim.android.data.update.AppUpdateRepository
import app.omnisim.android.notification.NotificationHelper
import app.omnisim.android.notification.ReminderScheduler

class OmniSimApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper(this).createChannel()
        container.reminderScheduler.schedule()
    }
}

class AppContainer(application: Application) {
    val database = OmniSimDatabase.create(application)
    val simRepository = SimRepository(database)
    val settingsRepository = SettingsRepository(application)
    val exchangeRateRepository = ExchangeRateRepository(application)
    val appUpdateRepository = AppUpdateRepository()
    val backupManager = BackupManager(application.contentResolver, database, settingsRepository)
    val reminderScheduler = ReminderScheduler(application)
}
