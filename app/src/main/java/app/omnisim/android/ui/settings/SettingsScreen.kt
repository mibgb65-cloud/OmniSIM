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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.omnisim.android.BuildConfig
import app.omnisim.android.R
import app.omnisim.android.backup.BackupPayload
import app.omnisim.android.data.preferences.AppSettings
import app.omnisim.android.data.preferences.ThemeMode
import app.omnisim.android.ui.components.OmniDialogSystemBars
import app.omnisim.android.ui.components.CurrencyPickerField
import app.omnisim.android.ui.components.omniTextFieldColors
import app.omnisim.android.ui.AppUpdateUiState
import app.omnisim.android.ui.theme.OmniCardPadding
import app.omnisim.android.ui.theme.OmniScreenPadding
import app.omnisim.android.ui.theme.OmniSectionSpacing
import java.time.LocalDate

private val warningPeriods = listOf(3, 7, 14, 30)
private val reminderOptions = listOf(30, 14, 7, 3, 1, 0, -1)

@Composable
fun SettingsScreen(
    settings: AppSettings,
    appLanguage: AppLanguage,
    pendingRestore: BackupPayload?,
    onThemeMode: (ThemeMode) -> Unit,
    onAppLanguage: (AppLanguage) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onWarningPeriod: (Int) -> Unit,
    onMaskPhoneNumbers: (Boolean) -> Unit,
    onReminderOffsets: (Set<Int>) -> Unit,
    onDefaultCurrency: (String) -> Unit,
    onExport: (android.net.Uri) -> Unit,
    onImport: (android.net.Uri) -> Unit,
    onConfirmRestore: () -> Unit,
    onCancelRestore: () -> Unit,
    onOpenPrivacyPermissions: () -> Unit,
    onOpenUsageGuide: () -> Unit,
    onCheckForUpdates: () -> Unit,
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
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { permissionGranted = it }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let(onExport) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(onImport) }

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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                reminderOptions.forEach { offset ->
                    val checked = offset in settings.reminderOffsets
                    ReminderSettingCard(
                        label = reminderLabel(offset),
                        checked = checked,
                        onToggle = {
                            onReminderOffsets(
                                if (checked) settings.reminderOffsets - offset
                                else settings.reminderOffsets + offset,
                            )
                        },
                    )
                }
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

        item { SectionTitle(stringResource(R.string.settings_data)) }
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
            }
        }

        item { SectionTitle(stringResource(R.string.settings_help_and_info)) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsActionCard(
                    title = stringResource(R.string.privacy_permissions),
                    description = stringResource(R.string.privacy_permissions_description),
                    leadingIcon = Icons.Default.Lock,
                    onClick = onOpenPrivacyPermissions,
                )
                SettingsActionCard(
                    title = stringResource(R.string.usage_guide),
                    description = stringResource(R.string.usage_guide_description),
                    leadingIcon = Icons.Default.Info,
                    onClick = onOpenUsageGuide,
                )
            }
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

    pendingRestore?.let { payload ->
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

@Composable
internal fun AppUpdateDialog(
    state: AppUpdateUiState,
    onRetry: () -> Unit,
    onDownload: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    when (state) {
        AppUpdateUiState.Idle -> Unit
        AppUpdateUiState.Checking -> AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                OmniDialogSystemBars()
                Text(stringResource(R.string.checking_for_updates))
            },
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    Text(stringResource(R.string.checking_for_updates_description))
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
        AppUpdateUiState.Failed -> AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                OmniDialogSystemBars()
                Text(stringResource(R.string.update_check_failed))
            },
            text = { Text(stringResource(R.string.update_check_failed_description)) },
            confirmButton = {
                TextButton(onClick = onRetry) {
                    Text(stringResource(R.string.action_retry))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
        is AppUpdateUiState.UpToDate -> AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                OmniDialogSystemBars()
                Text(stringResource(R.string.app_up_to_date))
            },
            text = {
                Text(stringResource(R.string.app_up_to_date_description, state.latestVersion))
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_ok))
                }
            },
        )
        is AppUpdateUiState.Available -> {
            val release = state.release
            AlertDialog(
                onDismissRequest = onDismiss,
                title = {
                    OmniDialogSystemBars()
                    Text(stringResource(R.string.update_available, release.version))
                },
                text = {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(release.title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            stringResource(
                                R.string.current_and_latest_version,
                                BuildConfig.VERSION_NAME,
                                release.version,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        Text(
                            stringResource(R.string.whats_new),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(release.notes ?: stringResource(R.string.no_release_notes))
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onDownload(release.apkDownloadUrl)
                            onDismiss()
                        },
                    ) {
                        Text(stringResource(R.string.download_update))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
            )
        }
    }
}

@Composable
private fun SectionTitle(value: String) {
    Column {
        Spacer(Modifier.height(12.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun SettingsActionCard(
    title: String,
    iconRotation: Float = 0f,
    description: String? = null,
    leadingIcon: ImageVector = Icons.AutoMirrored.Filled.ArrowForward,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape,
            ) {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(10.dp)
                        .rotate(iconRotation),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ReminderSettingCard(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = { onToggle() },
            ),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
            )
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun SettingsCard(
    contentPadding: PaddingValues = PaddingValues(OmniCardPadding),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}

@Composable
private fun SwitchSetting(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier
                .weight(1f)
                .padding(end = 16.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(2.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun themeModeLabel(mode: ThemeMode): String = stringResource(
    when (mode) {
        ThemeMode.System -> R.string.theme_system
        ThemeMode.Light -> R.string.theme_light
        ThemeMode.Dark -> R.string.theme_dark
    },
)

@Composable
private fun appLanguageLabel(language: AppLanguage): String = stringResource(
    when (language) {
        AppLanguage.System -> R.string.language_system
        AppLanguage.SimplifiedChinese -> R.string.language_simplified_chinese
        AppLanguage.English -> R.string.language_english
    },
)

@Composable
private fun reminderLabel(offset: Int): String = when (offset) {
    -1 -> stringResource(R.string.reminder_overdue)
    0 -> stringResource(R.string.reminder_on_day)
    1 -> stringResource(R.string.reminder_one_day_before)
    else -> pluralStringResource(R.plurals.reminder_days_before, offset, offset)
}
