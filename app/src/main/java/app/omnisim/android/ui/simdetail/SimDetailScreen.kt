package app.omnisim.android.ui.simdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.omnisim.android.R
import app.omnisim.android.data.local.entity.RenewalHistoryEntity
import app.omnisim.android.data.local.entity.SimEntity
import app.omnisim.android.data.preferences.AppSettings
import app.omnisim.android.domain.util.calculateRenewalStatus
import app.omnisim.android.domain.util.daysUntilRenewal
import app.omnisim.android.ui.components.RenewalSheet
import app.omnisim.android.ui.components.OmniPrimaryButton
import app.omnisim.android.ui.components.OmniSecondaryButton
import app.omnisim.android.ui.components.OmniSectionHeader
import app.omnisim.android.ui.components.OmniDialogSystemBars
import app.omnisim.android.ui.components.SimAvatar
import app.omnisim.android.ui.components.StatusChip
import app.omnisim.android.ui.components.daysRemainingLabel
import app.omnisim.android.ui.components.displayDate
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
    onOpenWebsite: (String) -> Unit,
    onEdit: () -> Unit,
    onArchive: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val today = LocalDate.now()
    val status = calculateRenewalStatus(
        today,
        sim.nextRenewalDate,
        settings.warningPeriodDays,
        sim.archived,
    )
    val remaining = daysUntilRenewal(today, sim.nextRenewalDate)
    var showRenewal by remember { mutableStateOf(false) }
    var showArchiveConfirmation by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

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
                        sim.renewalCycleDays?.let {
                            pluralStringResource(R.plurals.cycle_every_days, it, it)
                        } ?: stringResource(R.string.no_automatic_cycle),
                    )
                    sim.renewalPrice?.let {
                        InfoRow(
                            stringResource(R.string.price),
                            listOfNotNull(sim.currency, formatAmount(it)).joinToString(" "),
                        )
                    }
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
                modifier = Modifier.fillMaxWidth(),
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
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = { showDeleteConfirmation = true },
                modifier = Modifier.fillMaxWidth(),
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

@Composable
private fun SectionTitle(value: String) {
    OmniSectionHeader(text = value)
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.44f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.56f),
        )
    }
}

@Composable
private fun simTypeLabel(value: String): String = stringResource(
    if (value == "Physical SIM") R.string.type_physical_sim else R.string.type_esim,
)

private fun formatAmount(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else "%.2f".format(value)
