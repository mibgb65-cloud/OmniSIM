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
import androidx.compose.material3.LinearProgressIndicator
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
import app.omnisim.android.data.update.AppReleaseInfo
import app.omnisim.android.data.update.AppUpdateDownloadState
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
import java.io.File

enum class SettingsSection {
    Overview,
    Appearance,
    Renewal,
    DataPrivacy,
    HelpAbout,
}
@Composable
internal fun AppUpdateDialog(
    state: AppUpdateUiState,
    downloadState: AppUpdateDownloadState?,
    onRetry: () -> Unit,
    onDownload: (AppReleaseInfo) -> Unit,
    onCancelDownload: () -> Unit,
    onInstall: (File) -> Unit,
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
                        Text(
                            stringResource(R.string.update_checksum_available),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        Text(
                            stringResource(R.string.whats_new),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        ReleaseNotesContent(
                            release.notes ?: stringResource(R.string.no_release_notes),
                        )
                        when (val download = downloadState) {
                            is AppUpdateDownloadState.Downloading -> {
                                Spacer(Modifier.height(4.dp))
                                if (download.progressPercent == null) {
                                    LinearProgressIndicator(Modifier.fillMaxWidth())
                                } else {
                                    LinearProgressIndicator(
                                        progress = { download.progressPercent / 100f },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                                Text(
                                    download.progressPercent?.let {
                                        stringResource(R.string.update_download_progress, it)
                                    } ?: stringResource(R.string.update_download_started),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            is AppUpdateDownloadState.Ready -> Text(
                                stringResource(R.string.update_download_ready),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            is AppUpdateDownloadState.Failed -> Text(
                                stringResource(R.string.update_download_failed),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                            null -> Unit
                        }
                    }
                },
                confirmButton = {
                    when (val download = downloadState) {
                        is AppUpdateDownloadState.Downloading -> Unit
                        is AppUpdateDownloadState.Ready -> Button(
                            onClick = { onInstall(download.apkFile) },
                        ) {
                            Text(stringResource(R.string.install_update))
                        }
                        is AppUpdateDownloadState.Failed -> Button(
                            onClick = { onDownload(release) },
                        ) {
                            Text(stringResource(R.string.retry_download_update))
                        }
                        null -> Button(
                            onClick = { onDownload(release) },
                        ) {
                            Text(stringResource(R.string.download_update))
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = if (downloadState is AppUpdateDownloadState.Downloading) {
                            onCancelDownload
                        } else {
                            onDismiss
                        },
                    ) {
                        Text(
                            stringResource(
                                if (downloadState is AppUpdateDownloadState.Downloading) {
                                    R.string.cancel_download_update
                                } else {
                                    R.string.action_cancel
                                },
                            ),
                        )
                    }
                },
            )
        }
    }
}

@Composable
internal fun SectionTitle(value: String) {
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
internal fun SettingsActionCard(
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
internal fun ReminderChipGroup(
    title: String,
    options: List<Int>,
    selected: Set<Int>,
    onSelectionChanged: (Set<Int>) -> Unit,
    label: @Composable (Int) -> String,
) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(6.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { offset ->
            val checked = offset in selected
            FilterChip(
                selected = checked,
                onClick = {
                    onSelectionChanged(if (checked) selected - offset else selected + offset)
                },
                label = { Text(label(offset)) },
                modifier = Modifier.heightIn(min = 48.dp),
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

@Composable
internal fun SettingsCard(
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
internal fun SwitchSetting(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
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
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
internal fun themeModeLabel(mode: ThemeMode): String = stringResource(
    when (mode) {
        ThemeMode.System -> R.string.theme_system
        ThemeMode.Light -> R.string.theme_light
        ThemeMode.Dark -> R.string.theme_dark
    },
)

@Composable
internal fun appLanguageLabel(language: AppLanguage): String = stringResource(
    when (language) {
        AppLanguage.System -> R.string.language_system
        AppLanguage.SimplifiedChinese -> R.string.language_simplified_chinese
        AppLanguage.English -> R.string.language_english
    },
)

@Composable
internal fun reminderLabel(offset: Int): String = when (offset) {
    -1 -> stringResource(R.string.reminder_overdue)
    0 -> stringResource(R.string.reminder_on_day)
    1 -> stringResource(R.string.reminder_one_day_before)
    else -> pluralStringResource(R.plurals.reminder_days_before, offset, offset)
}

@Composable
internal fun reminderCheckTimeLabel(value: Instant): String {
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0]
        ?: java.util.Locale.ENGLISH
    val formatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(locale)
    }
    return remember(value, formatter) {
        formatter.format(value.atZone(ZoneId.systemDefault()))
    }
}
