package app.omnisim.android.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.os.ConfigurationCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.omnisim.android.BuildConfig
import app.omnisim.android.R
import app.omnisim.android.backup.BackupPayload
import app.omnisim.android.data.preferences.AppSettings
import app.omnisim.android.data.preferences.ThemeMode
import app.omnisim.android.notification.NotificationAvailability
import app.omnisim.android.notification.NotificationHelper
import app.omnisim.android.ui.components.OmniDialogSystemBars
import app.omnisim.android.ui.components.CurrencyPickerField
import app.omnisim.android.ui.components.omniTextFieldColors
import app.omnisim.android.ui.AppUpdateUiState
import app.omnisim.android.ui.theme.OmniCardPadding
import app.omnisim.android.ui.theme.OmniScreenPadding
import app.omnisim.android.ui.theme.OmniSectionSpacing
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val warningPeriods = listOf(3, 7, 14, 30)
private val advanceReminderOptions = listOf(30, 14, 7, 3, 1)
private val dueReminderOptions = listOf(0, -1)
private val reminderOptions = advanceReminderOptions + dueReminderOptions


@Composable
fun SettingsScreen(
    section: SettingsSection,
    settings: AppSettings,
    appLanguage: AppLanguage,
    pendingRestore: BackupPayload?,
    recoverySnapshotAvailable: Boolean,
    onThemeMode: (ThemeMode) -> Unit,
    onAppLanguage: (AppLanguage) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onWarningPeriod: (Int) -> Unit,
    onMaskPhoneNumbers: (Boolean) -> Unit,
    onReminderOffsets: (Set<Int>) -> Unit,
    onDefaultCurrency: (String) -> Unit,
    onExport: (android.net.Uri) -> Unit,
    onExportHistoryCsv: (android.net.Uri) -> Unit,
    onImport: (android.net.Uri) -> Unit,
    onPrepareRecoveryRestore: () -> Unit,
    onConfirmRestore: () -> Unit,
    onCancelRestore: () -> Unit,
    onNotificationsEnabled: () -> Unit,
    onSendTestNotification: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenLegalDocuments: () -> Unit,
    onOpenPrivacyPermissions: () -> Unit,
    onOpenUsageGuide: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onOpenSection: (SettingsSection) -> Unit,
    bottomContentPadding: Dp,
) {
    val context = LocalContext.current
    var customWarning by remember { mutableStateOf("") }
    var permissionGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var notificationAvailability by remember {
        mutableStateOf(NotificationHelper(context).availability())
    }
    LifecycleResumeEffect(context) {
        notificationAvailability = NotificationHelper(context).availability()
        permissionGranted = notificationAvailability.runtimePermissionGranted
        onPauseOrDispose { }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionGranted = granted
        notificationAvailability = NotificationHelper(context).availability()
        if (granted) onNotificationsEnabled()
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let(onExport) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(onImport) }
    val csvExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri -> uri?.let(onExportHistoryCsv) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = OmniScreenPadding,
            top = 8.dp,
            end = OmniScreenPadding,
            bottom = bottomContentPadding + OmniSectionSpacing,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (section == SettingsSection.Overview) {
            item {
                SettingsActionCard(
                    title = stringResource(R.string.settings_appearance_and_language),
                    description = stringResource(R.string.settings_appearance_and_language_description),
                    leadingIcon = Icons.Default.Settings,
                    onClick = { onOpenSection(SettingsSection.Appearance) },
                )
            }
            item {
                SettingsActionCard(
                    title = stringResource(R.string.settings_renewal_and_notifications),
                    description = stringResource(R.string.settings_renewal_and_notifications_description),
                    leadingIcon = Icons.Default.Notifications,
                    onClick = { onOpenSection(SettingsSection.Renewal) },
                )
            }
            item {
                SettingsActionCard(
                    title = stringResource(R.string.settings_data_and_privacy),
                    description = stringResource(R.string.settings_data_and_privacy_description),
                    leadingIcon = Icons.Default.Lock,
                    onClick = { onOpenSection(SettingsSection.DataPrivacy) },
                )
            }
            item {
                SettingsActionCard(
                    title = stringResource(R.string.settings_help_about),
                    description = stringResource(R.string.settings_help_about_description),
                    leadingIcon = Icons.Default.Info,
                    onClick = { onOpenSection(SettingsSection.HelpAbout) },
                )
            }
        }

        if (section == SettingsSection.Appearance) {
        item { SectionTitle(stringResource(R.string.settings_appearance)) }
        item {
            SettingsCard {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { onThemeMode(mode) },
                            label = { Text(themeModeLabel(mode)) },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
                SwitchSetting(
                    title = stringResource(R.string.dynamic_material_color),
                    description = stringResource(R.string.dynamic_material_color_description),
                    checked = settings.dynamicColor,
                    onCheckedChange = onDynamicColor,
                )
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
                Text(
                    stringResource(R.string.settings_language),
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AppLanguage.entries.forEach { language ->
                        FilterChip(
                            selected = appLanguage == language,
                            onClick = { onAppLanguage(language) },
                            label = { Text(appLanguageLabel(language)) },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                }
            }
        }
        }

        if (section == SettingsSection.Renewal) {
        item { SectionTitle(stringResource(R.string.settings_renewal)) }
        item {
            SettingsCard {
                Text(
                    stringResource(R.string.default_warning_period),
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    warningPeriods.forEach { days ->
                        FilterChip(
                            selected = settings.warningPeriodDays == days,
                            onClick = { onWarningPeriod(days) },
                            label = { Text(stringResource(R.string.days_short, days)) },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = customWarning,
                        onValueChange = { customWarning = it.filter(Char::isDigit).take(3) },
                        label = { Text(stringResource(R.string.custom_days)) },
                        singleLine = true,
                        colors = omniTextFieldColors(),
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = { customWarning.toIntOrNull()?.let(onWarningPeriod) },
                        enabled = (customWarning.toIntOrNull() ?: 0) > 0,
                        shape = CircleShape,
                    ) {
                        Text(stringResource(R.string.action_set))
                    }
                }
            }
        }
        }

        if (section == SettingsSection.DataPrivacy) {
        item { SectionTitle(stringResource(R.string.settings_privacy)) }
        item {
            SettingsCard {
                SwitchSetting(
                    title = stringResource(R.string.mask_phone_numbers),
                    description = stringResource(R.string.mask_phone_numbers_description),
                    checked = settings.maskPhoneNumbers,
                    onCheckedChange = onMaskPhoneNumbers,
                )
            }
        }
        }

        if (section == SettingsSection.Renewal) {
        item { SectionTitle(stringResource(R.string.settings_notifications)) }
        if (!permissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Column(
                        Modifier.padding(OmniCardPadding),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            stringResource(R.string.notification_permission_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            stringResource(R.string.notification_permission_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            shape = CircleShape,
                        ) {
                            Text(stringResource(R.string.enable_notifications))
                        }
                    }
                }
            }
        }
        item {
            NotificationHealthCard(
                availability = notificationAvailability,
                lastReminderCheckAt = settings.lastReminderCheckAt,
                onSendTestNotification = onSendTestNotification,
                onOpenNotificationSettings = onOpenNotificationSettings,
            )
        }
        item {
            SettingsCard {
                val allSelected = reminderOptions.all(settings.reminderOffsets::contains)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.reminder_timing),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            stringResource(R.string.reminder_timing_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(
                        onClick = {
                            onReminderOffsets(if (allSelected) emptySet() else reminderOptions.toSet())
                        },
                    ) {
                        Text(
                            stringResource(
                                if (allSelected) R.string.action_clear_all
                                else R.string.action_select_all,
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                ReminderChipGroup(
                    title = stringResource(R.string.reminder_before_renewal),
                    options = advanceReminderOptions,
                    selected = settings.reminderOffsets,
                    onSelectionChanged = onReminderOffsets,
                    label = { offset -> stringResource(R.string.days_short, offset) },
                )
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                ReminderChipGroup(
                    title = stringResource(R.string.reminder_due_and_overdue),
                    options = dueReminderOptions,
                    selected = settings.reminderOffsets,
                    onSelectionChanged = onReminderOffsets,
                    label = { offset -> reminderLabel(offset) },
                )
                }
            }

        item { SectionTitle(stringResource(R.string.settings_currency)) }
        item {
            SettingsCard {
                CurrencyPickerField(
                    selectedCode = settings.defaultCurrency,
                    onSelected = onDefaultCurrency,
                    label = stringResource(R.string.default_currency),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        }

        if (section == SettingsSection.DataPrivacy) {
        item { SectionTitle(stringResource(R.string.settings_data)) }
        item {
            val lastBackupAt = settings.lastBackupAt
            val backupIsStale = lastBackupAt == null ||
                lastBackupAt.plusSeconds(30L * 24 * 60 * 60).isBefore(Instant.now())
            SettingsCard {
                Text(
                    stringResource(R.string.backup_status),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    lastBackupAt?.let { backupAt ->
                        stringResource(R.string.last_backup_time, reminderCheckTimeLabel(backupAt))
                    } ?: stringResource(R.string.no_backup_created),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (backupIsStale) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                if (backupIsStale) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.backup_recommended),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsActionCard(
                    title = stringResource(R.string.export_backup),
                    iconRotation = -90f,
                    onClick = {
                        exportLauncher.launch("omnisim-backup-${LocalDate.now()}.json")
                    },
                )
                SettingsActionCard(
                    title = stringResource(R.string.import_backup),
                    iconRotation = 90f,
                    onClick = {
                        importLauncher.launch(arrayOf("application/json", "text/plain"))
                    },
                )
                if (recoverySnapshotAvailable) {
                    SettingsActionCard(
                        title = stringResource(R.string.restore_safety_snapshot),
                        description = stringResource(R.string.restore_safety_snapshot_description),
                        onClick = onPrepareRecoveryRestore,
                    )
                }
                SettingsActionCard(
                    title = stringResource(R.string.export_history_csv),
                    description = stringResource(R.string.export_history_csv_description),
                    iconRotation = -90f,
                    onClick = {
                        csvExportLauncher.launch("omnisim-renewals-${LocalDate.now()}.csv")
                    },
                )
            }
        }

        item { SectionTitle(stringResource(R.string.settings_help_and_info)) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsActionCard(
                    title = stringResource(R.string.legal_documents),
                    description = stringResource(R.string.legal_documents_description),
                    leadingIcon = Icons.Default.Lock,
                    onClick = onOpenLegalDocuments,
                )
                SettingsActionCard(
                    title = stringResource(R.string.privacy_permissions),
                    description = stringResource(R.string.privacy_permissions_description),
                    leadingIcon = Icons.Default.Lock,
                    onClick = onOpenPrivacyPermissions,
                )
            }
        }
        }

        if (section == SettingsSection.HelpAbout) {
        item { SectionTitle(stringResource(R.string.settings_help_and_info)) }
        item {
            SettingsActionCard(
                title = stringResource(R.string.usage_guide),
                description = stringResource(R.string.usage_guide_description),
                leadingIcon = Icons.Default.Info,
                onClick = onOpenUsageGuide,
            )
        }
        item { SectionTitle(stringResource(R.string.settings_about)) }
        item {
            SettingsActionCard(
                title = stringResource(R.string.check_for_updates),
                description = stringResource(
                    R.string.check_for_updates_description,
                    BuildConfig.VERSION_NAME,
                ),
                leadingIcon = Icons.Default.Info,
                onClick = onCheckForUpdates,
            )
        }
        item {
            SettingsCard {
                Text("OmniSIM ${BuildConfig.VERSION_NAME}", fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(R.string.app_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.open_source_license), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.background_limitation),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        }
    }

    if (section == SettingsSection.DataPrivacy) pendingRestore?.let { payload ->
        AlertDialog(
            onDismissRequest = onCancelRestore,
            title = {
                OmniDialogSystemBars()
                Text(stringResource(R.string.restore_backup_title))
            },
            text = {
                Text(
                    stringResource(
                        R.string.restore_backup_message,
                        pluralStringResource(
                            R.plurals.backup_sim_count,
                            payload.sims.size,
                            payload.sims.size,
                        ),
                        pluralStringResource(
                            R.plurals.backup_history_count,
                            payload.history.size,
                            payload.history.size,
                        ),
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmRestore) {
                    Text(stringResource(R.string.action_restore_backup))
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelRestore) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

}
