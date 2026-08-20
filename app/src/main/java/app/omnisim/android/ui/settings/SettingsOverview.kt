package app.omnisim.android.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.omnisim.android.BuildConfig
import app.omnisim.android.R
import app.omnisim.android.data.preferences.AppSettings
import app.omnisim.android.notification.NotificationAvailability

@Composable
internal fun SettingsOverview(
    settings: AppSettings,
    appLanguage: AppLanguage,
    notificationAvailability: NotificationAvailability,
    onOpenSection: (SettingsSection) -> Unit,
) {
    val appearanceSummary = stringResource(
        R.string.settings_overview_summary,
        themeModeLabel(settings.themeMode),
        appLanguageLabel(appLanguage),
    )
    val renewalSummary = if (settings.reminderOffsets.isEmpty()) {
        stringResource(R.string.global_reminders_disabled_short)
    } else {
        stringResource(
            R.string.settings_overview_summary,
            stringResource(
                if (notificationAvailability.canPost) R.string.notification_status_ready
                else R.string.notification_status_needs_attention,
            ),
            pluralStringResource(
                R.plurals.settings_reminder_count,
                settings.reminderOffsets.size,
                settings.reminderOffsets.size,
            ),
        )
    }
    val backupSummary = settings.lastBackupAt?.let { backupAt ->
        stringResource(R.string.last_backup_time, reminderCheckTimeLabel(backupAt))
    } ?: stringResource(R.string.no_backup_created_short)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingsActionCard(
            title = stringResource(R.string.settings_appearance_and_language),
            description = appearanceSummary,
            leadingIcon = Icons.Default.Settings,
            onClick = { onOpenSection(SettingsSection.Appearance) },
        )
        SettingsActionCard(
            title = stringResource(R.string.settings_renewal_and_notifications),
            description = renewalSummary,
            leadingIcon = Icons.Default.Notifications,
            onClick = { onOpenSection(SettingsSection.Renewal) },
        )
        SettingsActionCard(
            title = stringResource(R.string.settings_data_and_privacy),
            description = backupSummary,
            leadingIcon = Icons.Default.Lock,
            onClick = { onOpenSection(SettingsSection.DataPrivacy) },
        )
        SettingsActionCard(
            title = stringResource(R.string.settings_help_about),
            description = stringResource(R.string.app_version_label, BuildConfig.VERSION_NAME),
            leadingIcon = Icons.Default.Info,
            onClick = { onOpenSection(SettingsSection.HelpAbout) },
        )
    }
}
