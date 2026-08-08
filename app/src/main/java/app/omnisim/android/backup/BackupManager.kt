package app.omnisim.android.backup

import android.content.ContentResolver
import android.net.Uri
import app.omnisim.android.data.local.OmniSimDatabase
import app.omnisim.android.data.preferences.SettingsRepository
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class BackupManager(
    private val contentResolver: ContentResolver,
    private val database: OmniSimDatabase,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend fun export(uri: Uri) = withContext(Dispatchers.IO) {
        val dao = database.dao()
        val content = BackupCodec.encode(
            sims = dao.getAllSims(),
            history = dao.getAllHistory(),
            settings = settingsRepository.settings.first(),
            exportedAt = Instant.now(clock),
        )
        contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(content) }
            ?: error("Could not open backup destination")
    }

    suspend fun readAndValidate(uri: Uri): BackupPayload = withContext(Dispatchers.IO) {
        val content = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("Could not open backup")
        BackupCodec.decode(content)
    }

    suspend fun restore(payload: BackupPayload) = withContext(Dispatchers.IO) {
        val dao = database.dao()
        val previousSims = dao.getAllSims()
        val previousHistory = dao.getAllHistory()
        val previousReminderStates = dao.getAllReminderStates()
        val previousSettings = settingsRepository.settings.first()
        try {
            dao.replaceAll(payload.sims, payload.history)
            settingsRepository.replace(payload.settings)
        } catch (exception: Exception) {
            runCatching {
                dao.replaceAll(previousSims, previousHistory, previousReminderStates)
            }
            runCatching { settingsRepository.replace(previousSettings) }
            throw exception
        }
    }
}
