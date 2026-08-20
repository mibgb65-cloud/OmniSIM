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

const val CURRENT_LEGAL_CONSENT_VERSION = 2

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = false,
    val warningPeriodDays: Int = 14,
    val maskPhoneNumbers: Boolean = true,
    val reminderOffsets: Set<Int> = setOf(30, 14, 7, 3, 1, 0, -1),
    val defaultCurrency: String = "USD",
    val lastReminderCheckAt: Instant? = null,
    val lastBackupAt: Instant? = null,
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
        val lastBackupAt = longPreferencesKey("last_backup_at")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map(::toSettings)
    val legalConsentVersion: Flow<Int> = context.settingsDataStore.data.map { preferences ->
        preferences[Keys.legalConsentVersion] ?: 0
    }

    suspend fun setThemeMode(value: ThemeMode) = update(Keys.theme, value.name)
    suspend fun setDynamicColor(value: Boolean) = update(Keys.dynamicColor, value)
    suspend fun setWarningPeriod(value: Int) = update(Keys.warningPeriod, value.coerceAtLeast(0))
    suspend fun setMaskPhoneNumbers(value: Boolean) = update(Keys.maskPhoneNumbers, value)
    suspend fun setDefaultCurrency(value: String) = update(Keys.defaultCurrency, value.uppercase())
    suspend fun acceptCurrentLegalConsent() =
        update(Keys.legalConsentVersion, CURRENT_LEGAL_CONSENT_VERSION)

    suspend fun recordReminderCheck(value: Instant) =
        update(Keys.lastReminderCheckAt, value.toEpochMilli())

    suspend fun recordBackup(value: Instant) =
        update(Keys.lastBackupAt, value.toEpochMilli())

    suspend fun setReminderOffsets(value: Set<Int>) =
        update(Keys.reminderOffsets, value.sortedDescending().joinToString(","))

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
            lastBackupAt = preferences[Keys.lastBackupAt]?.let(Instant::ofEpochMilli),
        )
    }
}
