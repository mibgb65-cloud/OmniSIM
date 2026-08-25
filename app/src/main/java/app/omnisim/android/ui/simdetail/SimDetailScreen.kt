package app.omnisim.android.ui.simdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.omnisim.android.R
import app.omnisim.android.data.local.entity.RenewalHistoryEntity
import app.omnisim.android.data.local.entity.SimEntity
import app.omnisim.android.data.preferences.AppSettings
import app.omnisim.android.domain.util.calculateRenewalStatus
import app.omnisim.android.domain.util.calculateLatestRenewalPriceChange
import app.omnisim.android.domain.util.daysUntilRenewal
import app.omnisim.android.domain.util.SupportedReminderOffsets
import app.omnisim.android.domain.util.effectiveReminderOffsets
import app.omnisim.android.ui.components.RenewalSheet
import app.omnisim.android.ui.components.RenewalHistoryEditSheet
import app.omnisim.android.ui.components.OmniPrimaryButton
import app.omnisim.android.ui.components.OmniSecondaryButton
import app.omnisim.android.ui.components.OmniSectionHeader
import app.omnisim.android.ui.components.OmniDialogSystemBars
import app.omnisim.android.ui.components.OmniSheetHeader
import app.omnisim.android.ui.components.SimAvatar
import app.omnisim.android.ui.components.StatusChip
import app.omnisim.android.ui.components.daysRemainingLabel
import app.omnisim.android.ui.components.displayDate
import app.omnisim.android.ui.components.rememberCurrentDate
import app.omnisim.android.ui.theme.OmniCardPadding
import app.omnisim.android.ui.theme.OmniRowSpacing
import app.omnisim.android.ui.theme.OmniScreenPadding
import app.omnisim.android.ui.theme.OmniSectionSpacing
import java.time.LocalDate

@Composable
fun SimDetailScreen(
    sim: SimEntity,
    history: List<RenewalHistoryEntity>,
    settings: AppSettings,
    onRenew: (LocalDate, LocalDate, Double?, String?) -> Unit,
    onUpdateRenewal: (String, LocalDate, LocalDate, Double?, String?) -> Unit,
    onUndoRenewal: (String) -> Unit,
    onReminderSettings: (Boolean, Set<Int>?) -> Unit,
    onOpenWebsite: (String) -> Unit,
    onEdit: () -> Unit,
    onArchive: (Boolean) -> Unit,
    onDelete: () -> Unit,
    openRenewalRequested: Boolean = false,
    onOpenRenewalHandled: () -> Unit = {},
) {
    val today = rememberCurrentDate()
    val status = calculateRenewalStatus(
        today,
        sim.nextRenewalDate,
        settings.warningPeriodDays,
        sim.archived,
    )
    val remaining = daysUntilRenewal(today, sim.nextRenewalDate)
    var showRenewal by remember { mutableStateOf(false) }
    var editingRenewal by remember { mutableStateOf<RenewalHistoryEntity?>(null) }
    var pendingUndo by remember { mutableStateOf<RenewalHistoryEntity?>(null) }
    var showReminderSettings by remember { mutableStateOf(false) }
    var showArchiveConfirmation by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val latestHistoryId = remember(history) {
        history.maxByOrNull(RenewalHistoryEntity::createdAt)?.id
    }
    val priceChange = remember(history, sim.id) {
        calculateLatestRenewalPriceChange(history, sim.id)
    }
    LaunchedEffect(openRenewalRequested) {
        if (openRenewalRequested) {
            showRenewal = true
            onOpenRenewalHandled()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = OmniScreenPadding,
            top = 12.dp,
            end = OmniScreenPadding,
            bottom = OmniSectionSpacing,
        ),
        verticalArrangement = Arrangement.spacedBy(OmniRowSpacing),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SimAvatar(sim.name)
                Column(Modifier.weight(1f)) {
                    Text(
                        sim.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        listOfNotNull(sim.carrier, sim.countryName).distinct().joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusChip(status)
            }
        }
        item {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Column(Modifier.padding(OmniCardPadding)) {
                    Text(
                        stringResource(R.string.renewal),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.68f),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(daysRemainingLabel(remaining), style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.height(16.dp))
                    InfoRow(stringResource(R.string.next_renewal), sim.nextRenewalDate.displayDate())
                    sim.lastRenewalDate?.let {
                        InfoRow(stringResource(R.string.last_renewal), it.displayDate())
                    }
                    InfoRow(
                        stringResource(R.string.cycle),
                        when {
                            sim.renewalCycleDays != null -> pluralStringResource(
                                R.plurals.cycle_every_days,
                                sim.renewalCycleDays,
                                sim.renewalCycleDays,
                            )
                            sim.renewalDayOfMonth != null -> stringResource(
                                R.string.monthly_on_day,
                                sim.renewalDayOfMonth,
                            )
                            else -> stringResource(R.string.no_automatic_cycle)
                        },
                    )
                    sim.renewalPrice?.let {
                        InfoRow(
                            stringResource(R.string.price),
                            listOfNotNull(sim.currency, formatAmount(it)).joinToString(" "),
                        )
                    }
                    priceChange?.let { RenewalPriceChangeRow(it) }
                    if (!sim.archived) {
                        Spacer(Modifier.height(18.dp))
                        OmniPrimaryButton(
                            text = stringResource(R.string.mark_as_renewed),
                            onClick = { showRenewal = true },
                        )
                    }
                }
            }
        }
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.large,
            ) {
                Column(Modifier.padding(OmniCardPadding)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .toggleable(
                                value = sim.remindersEnabled,
                                role = Role.Switch,
                                onValueChange = {
                                    onReminderSettings(it, sim.reminderOffsets)
                                },
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.sim_reminders),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                when {
                                    !sim.remindersEnabled -> stringResource(R.string.sim_reminders_disabled)
                                    sim.reminderOffsets == null -> stringResource(
                                        R.string.sim_reminders_follow_global,
                                    )
                                    else -> pluralStringResource(
                                        R.plurals.sim_custom_reminder_count,
                                        sim.reminderOffsets.size,
                                        sim.reminderOffsets.size,
                                    )
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = sim.remindersEnabled,
                            onCheckedChange = null,
                        )
                    }
                    if (sim.remindersEnabled) {
                        TextButton(
                            onClick = { showReminderSettings = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                        ) {
                            Text(stringResource(R.string.configure_sim_reminders))
                        }
                    }
                }
            }
        }
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    Modifier.padding(OmniCardPadding),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(stringResource(R.string.sim_information), style = MaterialTheme.typography.titleMedium)
                    InfoRow(stringResource(R.string.sim_type), simTypeLabel(sim.simType))
                    sim.countryName?.let {
                        InfoRow(
                            stringResource(R.string.country),
                            listOfNotNull(it, sim.countryCode).joinToString(" · "),
                        )
                    }
                    sim.phoneNumber?.let { InfoRow(stringResource(R.string.phone_number), it) }
                    sim.planName?.let { InfoRow(stringResource(R.string.plan), it) }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                sim.renewalUrl?.let { url ->
                    OmniSecondaryButton(
                        text = stringResource(R.string.open_renewal_website),
                        onClick = { onOpenWebsite(url) },
                        modifier = Modifier.weight(1f),
                    )
                }
                OmniSecondaryButton(
                    text = stringResource(R.string.action_edit),
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            TextButton(
                onClick = { showArchiveConfirmation = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(
                    stringResource(
                        if (sim.archived) R.string.action_restore_sim else R.string.action_archive_sim,
                    ),
                )
            }
        }
        sim.notes?.let { notes ->
            item {
                SectionTitle(stringResource(R.string.notes))
                Spacer(Modifier.height(6.dp))
                Text(
                    notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { SectionTitle(stringResource(R.string.renewal_history)) }
        if (history.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.no_renewal_history),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(history, key = RenewalHistoryEntity::id) { renewal ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Column(Modifier.padding(OmniCardPadding)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                renewal.renewalDate.displayDate(),
                                fontWeight = FontWeight.SemiBold,
                            )
                            renewal.amount?.let {
                                Text(
                                    listOfNotNull(renewal.currency, formatAmount(it))
                                        .joinToString(" "),
                                )
                            }
                        }
                        renewal.nextRenewalDate?.let {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.next_date_value, it.displayDate()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        renewal.notes?.let {
                            Spacer(Modifier.height(4.dp))
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(
                                onClick = { editingRenewal = renewal },
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                Text(stringResource(R.string.edit_renewal_record))
                            }
                        }
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = { showDeleteConfirmation = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(
                    stringResource(R.string.action_delete_sim),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    if (showRenewal) {
        RenewalSheet(
            sim = sim,
            onDismiss = { showRenewal = false },
            onConfirm = { actual, next, amount, notes ->
                onRenew(actual, next, amount, notes)
                showRenewal = false
            },
        )
    }
    if (showReminderSettings) {
        SimReminderSettingsSheet(
            sim = sim,
            globalOffsets = settings.reminderOffsets,
            onDismiss = { showReminderSettings = false },
            onSave = { offsets ->
                onReminderSettings(true, offsets)
                showReminderSettings = false
            },
        )
    }
    editingRenewal?.let { renewal ->
        RenewalHistoryEditSheet(
            history = renewal,
            currency = renewal.currency ?: sim.currency,
            canUndo = renewal.id == latestHistoryId && renewal.previousNextRenewalDate != null,
            onDismiss = { editingRenewal = null },
            onConfirm = { actual, next, amount, notes ->
                onUpdateRenewal(renewal.id, actual, next, amount, notes)
                editingRenewal = null
            },
            onUndoRequested = {
                editingRenewal = null
                pendingUndo = renewal
            },
        )
    }
    pendingUndo?.let { renewal ->
        AlertDialog(
            onDismissRequest = { pendingUndo = null },
            title = {
                OmniDialogSystemBars()
                Text(stringResource(R.string.undo_renewal_title))
            },
            text = { Text(stringResource(R.string.undo_renewal_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUndoRenewal(renewal.id)
                        pendingUndo = null
                    },
                ) {
                    Text(
                        stringResource(R.string.action_confirm_undo),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingUndo = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
    if (showArchiveConfirmation) {
        AlertDialog(
            onDismissRequest = { showArchiveConfirmation = false },
            title = {
                OmniDialogSystemBars()
                Text(
                    stringResource(
                        if (sim.archived) R.string.restore_sim_title else R.string.archive_sim_title,
                        sim.name,
                    ),
                )
            },
            text = {
                Text(
                    stringResource(
                        if (sim.archived) R.string.restore_sim_message else R.string.archive_sim_message,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onArchive(!sim.archived)
                    showArchiveConfirmation = false
                }) {
                    Text(
                        stringResource(
                            if (sim.archived) R.string.action_restore else R.string.action_archive,
                        ),
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveConfirmation = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = {
                OmniDialogSystemBars()
                Text(stringResource(R.string.delete_sim_title, sim.name))
            },
            text = { Text(stringResource(R.string.delete_sim_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    onDelete()
                }) {
                    Text(
                        stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}
