package app.omnisim.android.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

enum class ThemeMode { System, Light, Dark }

enum class ReminderCheckResult { NotRun, Success, NotificationsBlocked, Failed }

const val CURRENT_LEGAL_CONSENT_VERSION = 3

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = false,
    val warningPeriodDays: Int = 14,
    val maskPhoneNumbers: Boolean = true,
    val reminderOffsets: Set<Int> = setOf(30, 14, 7, 3, 1, 0, -1),
    val defaultCurrency: String = "USD",
    val lastReminderCheckAt: Instant? = null,
    val lastReminderCheckResult: ReminderCheckResult = ReminderCheckResult.NotRun,
    val lastReminderCheckScannedCount: Int = 0,
    val lastReminderCheckSentCount: Int = 0,
    val lastBackupAt: Instant? = null,
    val backupDirty: Boolean = false,
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val theme = stringPreferencesKey("theme")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val warningPeriod = intPreferencesKey("warning_period")
        val maskPhoneNumbers = booleanPreferencesKey("mask_phone_numbers")
        val reminderOffsets = stringPreferencesKey("reminder_offsets")
        val defaultCurrency = stringPreferencesKey("default_currency")
        val legalConsentVersion = intPreferencesKey("legal_consent_version")
        val lastReminderCheckAt = longPreferencesKey("last_reminder_check_at")
        val lastReminderCheckResult = stringPreferencesKey("last_reminder_check_result")
        val lastReminderCheckScannedCount = intPreferencesKey("last_reminder_check_scanned_count")
        val lastReminderCheckSentCount = intPreferencesKey("last_reminder_check_sent_count")
        val lastBackupAt = longPreferencesKey("last_backup_at")
        val backupDirty = booleanPreferencesKey("backup_dirty")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map(::toSettings)
    val legalConsentVersion: Flow<Int> = context.settingsDataStore.data.map { preferences ->
        preferences[Keys.legalConsentVersion] ?: 0
    }

    suspend fun setThemeMode(value: ThemeMode) = updateBackedSetting(Keys.theme, value.name)
    suspend fun setDynamicColor(value: Boolean) = updateBackedSetting(Keys.dynamicColor, value)
    suspend fun setWarningPeriod(value: Int) =
        updateBackedSetting(Keys.warningPeriod, value.coerceAtLeast(0))
    suspend fun setMaskPhoneNumbers(value: Boolean) =
        updateBackedSetting(Keys.maskPhoneNumbers, value)
    suspend fun setDefaultCurrency(value: String) =
        updateBackedSetting(Keys.defaultCurrency, value.uppercase())
    suspend fun acceptCurrentLegalConsent() =
        update(Keys.legalConsentVersion, CURRENT_LEGAL_CONSENT_VERSION)

    suspend fun recordReminderCheck(
        value: Instant,
        result: ReminderCheckResult,
        scannedCount: Int,
        sentCount: Int,
    ) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.lastReminderCheckAt] = value.toEpochMilli()
            preferences[Keys.lastReminderCheckResult] = result.name
            preferences[Keys.lastReminderCheckScannedCount] = scannedCount
            preferences[Keys.lastReminderCheckSentCount] = sentCount
        }
    }

    suspend fun recordBackup(value: Instant) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.lastBackupAt] = value.toEpochMilli()
            preferences[Keys.backupDirty] = false
        }
    }

    suspend fun markBackupDirty() = update(Keys.backupDirty, true)

    suspend fun setReminderOffsets(value: Set<Int>) =
        updateBackedSetting(Keys.reminderOffsets, value.sortedDescending().joinToString(","))

    suspend fun replace(settings: AppSettings) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.theme] = settings.themeMode.name
            preferences[Keys.dynamicColor] = settings.dynamicColor
            preferences[Keys.warningPeriod] = settings.warningPeriodDays
            preferences[Keys.maskPhoneNumbers] = settings.maskPhoneNumbers
            preferences[Keys.reminderOffsets] = settings.reminderOffsets.sortedDescending().joinToString(",")
            preferences[Keys.defaultCurrency] = settings.defaultCurrency
        }
    }

    private suspend fun <T> update(key: Preferences.Key<T>, value: T) {
        context.settingsDataStore.edit { it[key] = value }
    }

    private suspend fun <T> updateBackedSetting(key: Preferences.Key<T>, value: T) {
        context.settingsDataStore.edit { preferences ->
            preferences[key] = value
            preferences[Keys.backupDirty] = true
        }
    }

    private fun toSettings(preferences: Preferences): AppSettings {
        val default = AppSettings()
        return AppSettings(
            themeMode = preferences[Keys.theme]
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: default.themeMode,
            dynamicColor = preferences[Keys.dynamicColor] ?: default.dynamicColor,
            warningPeriodDays = preferences[Keys.warningPeriod] ?: default.warningPeriodDays,
            maskPhoneNumbers = preferences[Keys.maskPhoneNumbers] ?: default.maskPhoneNumbers,
            reminderOffsets = preferences[Keys.reminderOffsets]
                ?.split(',')
                ?.mapNotNull(String::toIntOrNull)
                ?.toSet()
                ?: default.reminderOffsets,
            defaultCurrency = preferences[Keys.defaultCurrency] ?: default.defaultCurrency,
            lastReminderCheckAt = preferences[Keys.lastReminderCheckAt]?.let(Instant::ofEpochMilli),
            lastReminderCheckResult = preferences[Keys.lastReminderCheckResult]
                ?.let { runCatching { ReminderCheckResult.valueOf(it) }.getOrNull() }
                ?: default.lastReminderCheckResult,
            lastReminderCheckScannedCount = preferences[Keys.lastReminderCheckScannedCount] ?: 0,
            lastReminderCheckSentCount = preferences[Keys.lastReminderCheckSentCount] ?: 0,
            lastBackupAt = preferences[Keys.lastBackupAt]?.let(Instant::ofEpochMilli),
            backupDirty = preferences[Keys.backupDirty] ?: false,
        )
    }
}
