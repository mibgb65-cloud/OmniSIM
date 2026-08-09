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
internal fun SectionTitle(value: String) {
    OmniSectionHeader(text = value)
}

@Composable
internal fun InfoRow(label: String, value: String) {
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
internal fun simTypeLabel(value: String): String = stringResource(
    if (value == "Physical SIM") R.string.type_physical_sim else R.string.type_esim,
)

internal fun formatAmount(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else "%.2f".format(value)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SimReminderSettingsSheet(
    sim: SimEntity,
    globalOffsets: Set<Int>,
    onDismiss: () -> Unit,
    onSave: (Set<Int>?) -> Unit,
) {
    var useCustom by remember(sim.id) { mutableStateOf(sim.reminderOffsets != null) }
    var selected by remember(sim.id) {
        mutableStateOf(effectiveReminderOffsets(sim.reminderOffsets, globalOffsets))
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        OmniDialogSystemBars()
        OmniSheetHeader(
            title = stringResource(R.string.configure_sim_reminders),
            onClose = onDismiss,
        )
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = !useCustom,
                    onClick = { useCustom = false },
                    label = { Text(stringResource(R.string.follow_global_reminders)) },
                    modifier = Modifier.height(48.dp),
                    shape = CircleShape,
                )
                FilterChip(
                    selected = useCustom,
                    onClick = { useCustom = true },
                    label = { Text(stringResource(R.string.custom_sim_reminders)) },
                    modifier = Modifier.height(48.dp),
                    shape = CircleShape,
                )
            }
            if (useCustom) {
                Text(
                    stringResource(R.string.custom_sim_reminders_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SupportedReminderOffsets.forEach { offset ->
                        val checked = offset in selected
                        FilterChip(
                            selected = checked,
                            onClick = {
                                selected = if (checked) selected - offset else selected + offset
                            },
                            label = { Text(simReminderLabel(offset)) },
                            modifier = Modifier.height(48.dp),
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }
                if (selected.isEmpty()) {
                    Text(
                        stringResource(R.string.select_at_least_one_reminder),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            OmniPrimaryButton(
                text = stringResource(R.string.action_save),
                onClick = { onSave(if (useCustom) selected else null) },
                enabled = !useCustom || selected.isNotEmpty(),
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun simReminderLabel(offset: Int): String = when (offset) {
    -1 -> stringResource(R.string.reminder_overdue)
    0 -> stringResource(R.string.reminder_on_day)
    1 -> stringResource(R.string.reminder_one_day_before)
    else -> pluralStringResource(R.plurals.reminder_days_before, offset, offset)
}
