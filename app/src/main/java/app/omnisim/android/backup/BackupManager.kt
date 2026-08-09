package app.omnisim.android.backup

import android.content.ContentResolver
import android.net.Uri
import android.util.AtomicFile
import app.omnisim.android.data.local.OmniSimDatabase
import app.omnisim.android.data.preferences.SettingsRepository
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class BackupManager(
    private val contentResolver: ContentResolver,
    private val database: OmniSimDatabase,
    private val settingsRepository: SettingsRepository,
    private val recoveryFile: File,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend fun export(uri: Uri) = withContext(Dispatchers.IO) {
        val dao = database.dao()
        val exportedAt = Instant.now(clock)
        val content = BackupCodec.encode(
            sims = dao.getAllSims(),
            history = dao.getAllHistory(),
            settings = settingsRepository.settings.first(),
            exportedAt = exportedAt,
        )
        contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(content) }
            ?: error("Could not open backup destination")
        settingsRepository.recordBackup(exportedAt)
    }

    suspend fun exportHistoryCsv(uri: Uri) = withContext(Dispatchers.IO) {
        val dao = database.dao()
        val content = RenewalHistoryCsv.encode(dao.getAllSims(), dao.getAllHistory())
        contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(content) }
            ?: error("Could not open CSV destination")
    }

    suspend fun readAndValidate(uri: Uri): BackupPayload = withContext(Dispatchers.IO) {
        val content = contentResolver.openInputStream(uri)?.use { it.readBackupText() }
            ?: error("Could not open backup")
        BackupCodec.decode(content)
    }

    suspend fun restore(payload: BackupPayload) = withContext(Dispatchers.IO) {
        val dao = database.dao()
        val previousSims = dao.getAllSims()
        val previousHistory = dao.getAllHistory()
        val previousReminderStates = dao.getAllReminderStates()
        val previousSettings = settingsRepository.settings.first()
        writeRecoverySnapshot(previousSims, previousHistory, previousSettings)
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

    fun hasRecoverySnapshot(): Boolean = recoveryFile.isFile

    suspend fun readRecoverySnapshot(): BackupPayload? = withContext(Dispatchers.IO) {
        if (!recoveryFile.isFile) return@withContext null
        BackupCodec.decode(recoveryFile.inputStream().use { it.readBackupText() })
    }

    private fun writeRecoverySnapshot(
        sims: List<app.omnisim.android.data.local.entity.SimEntity>,
        history: List<app.omnisim.android.data.local.entity.RenewalHistoryEntity>,
        settings: app.omnisim.android.data.preferences.AppSettings,
    ) {
        val directory = recoveryFile.parentFile ?: error("Recovery directory is unavailable")
        check(directory.isDirectory || directory.mkdirs()) { "Could not create recovery directory" }
        val content = BackupCodec.encode(
            sims = sims,
            history = history,
            settings = settings,
            exportedAt = Instant.now(clock),
        ).toByteArray(Charsets.UTF_8)
        val atomicFile = AtomicFile(recoveryFile)
        val output = atomicFile.startWrite()
        try {
            output.write(content)
            atomicFile.finishWrite(output)
        } catch (exception: Exception) {
            atomicFile.failWrite(output)
            throw exception
        }
    }
}

internal const val MAX_BACKUP_SIZE_BYTES = 5 * 1024 * 1024

internal fun InputStream.readBackupText(
    maxBytes: Int = MAX_BACKUP_SIZE_BYTES,
): String {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var totalBytes = 0
    while (true) {
        val bytesRead = read(buffer)
        if (bytesRead < 0) break
        totalBytes += bytesRead
        if (totalBytes > maxBytes) {
            throw BackupValidationException("Backup file is too large")
        }
        output.write(buffer, 0, bytesRead)
    }
    return output.toString(Charsets.UTF_8.name())
}
