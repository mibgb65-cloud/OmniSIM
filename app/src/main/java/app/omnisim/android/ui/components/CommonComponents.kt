package app.omnisim.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.omnisim.android.R
import app.omnisim.android.data.local.entity.SimEntity
import app.omnisim.android.domain.model.RenewalStatus
import app.omnisim.android.domain.util.calculateScheduledNextRenewalDate
import app.omnisim.android.domain.util.daysUntilRenewal
import app.omnisim.android.domain.util.maskPhoneNumber
import app.omnisim.android.ui.theme.OmniCardPadding
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun LocalDate.displayDate(): String {
    val languageTag = LocalLocale.current.toLanguageTag()
    val locale = remember(languageTag) { Locale.forLanguageTag(languageTag) }
    val formatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    }
    return remember(this, formatter) { format(formatter) }
}

@Composable
fun daysRemainingLabel(days: Long): String = when {
    days < 0 -> {
        val count = (-days).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        pluralStringResource(R.plurals.days_overdue, count, count)
    }
    days == 0L -> stringResource(R.string.status_due_today)
    else -> {
        val count = days.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        pluralStringResource(R.plurals.days_left, count, count)
    }
}

@Composable
fun RenewalStatus.label(): String = stringResource(
    when (this) {
        RenewalStatus.Active -> R.string.status_active
        RenewalStatus.DueSoon -> R.string.status_due_soon
        RenewalStatus.DueToday -> R.string.status_due_today
        RenewalStatus.Overdue -> R.string.status_overdue
        RenewalStatus.Archived -> R.string.status_archived
    },
)

private data class StatusColors(
    val container: Color,
    val content: Color,
)

@Composable
private fun statusColors(status: RenewalStatus): StatusColors = when (status) {
    RenewalStatus.Active -> StatusColors(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.onPrimaryContainer,
    )
    RenewalStatus.DueSoon -> StatusColors(
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.onTertiaryContainer,
    )
    RenewalStatus.DueToday,
    RenewalStatus.Overdue,
    -> StatusColors(
        MaterialTheme.colorScheme.errorContainer,
        MaterialTheme.colorScheme.onErrorContainer,
    )
    RenewalStatus.Archived -> StatusColors(
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun StatusChip(status: RenewalStatus) {
    val colors = statusColors(status)
    Surface(
        color = colors.container,
        contentColor = colors.content,
        shape = CircleShape,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(5.dp)
                    .background(colors.content, CircleShape),
            )
            Text(status.label(), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun SimAvatar(name: String, modifier: Modifier = Modifier) {
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "S"
    Surface(
        modifier = modifier
            .size(44.dp)
            .clearAndSetSemantics { },
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(initial, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun SimSummaryRow(
    sim: SimEntity,
    status: RenewalStatus,
    maskNumbers: Boolean,
    today: LocalDate,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { role = Role.Button },
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.padding(OmniCardPadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SimAvatar(sim.name)
            Column(Modifier.weight(1f)) {
                Text(
                    sim.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                val phone = if (maskNumbers) maskPhoneNumber(sim.phoneNumber) else sim.phoneNumber
                val supporting = listOfNotNull(
                    sim.carrier.takeIf { it != sim.name },
                    phone,
                ).distinct().joinToString(" · ")
                if (supporting.isNotBlank()) {
                    Text(
                        supporting,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                Text(
                    sim.nextRenewalDate.displayDate(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val remaining = daysUntilRenewal(today, sim.nextRenewalDate)
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                StatusChip(status)
                Text(
                    daysRemainingLabel(remaining),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (remaining <= 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    value: LocalDate,
    onValueChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    val valueLabel = value.displayDate()
    Column(modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Surface(
            onClick = { showPicker = true },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "$label, $valueLabel" },
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface),
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    valueLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
    if (showPicker) {
        val pickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = value.toPickerMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onValueChange(it.toLocalDate()) }
                    showPicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) {
            OmniDialogSystemBars()
            DatePicker(state = pickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenewalSheet(
    sim: SimEntity,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate, Double?, String?) -> Unit,
) {
    var renewalDate by remember(sim.id) { mutableStateOf(LocalDate.now()) }
    var nextRenewalDate by remember(sim.id) {
        mutableStateOf(
            calculateScheduledNextRenewalDate(
                LocalDate.now(),
                sim.renewalCycleDays,
                sim.renewalDayOfMonth,
            )
                ?: sim.nextRenewalDate,
        )
    }
    var amount by remember(sim.id) { mutableStateOf(sim.renewalPrice?.toString().orEmpty()) }
    var notes by remember(sim.id) { mutableStateOf("") }
    val parsedAmount = amount.toDoubleOrNull()
    val amountValid = amount.isBlank() || (parsedAmount != null && parsedAmount >= 0)

    LaunchedEffect(renewalDate, sim.renewalCycleDays, sim.renewalDayOfMonth) {
        calculateScheduledNextRenewalDate(
            renewalDate,
            sim.renewalCycleDays,
            sim.renewalDayOfMonth,
        )?.let { nextRenewalDate = it }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
    ) {
        OmniDialogSystemBars()
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            OmniSheetHeader(
                title = stringResource(R.string.renew_sheet_title, sim.name),
                onClose = onDismiss,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                DateField(stringResource(R.string.renewal_date), renewalDate, { renewalDate = it })
                DateField(stringResource(R.string.next_renewal), nextRenewalDate, { nextRenewalDate = it })
                sim.renewalCycleDays?.let { cycleDays ->
                    Text(
                        stringResource(R.string.cycle_calculation_hint, cycleDays),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                sim.renewalDayOfMonth?.let { dayOfMonth ->
                    Text(
                        stringResource(R.string.monthly_calculation_hint, dayOfMonth),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OmniTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = stringResource(R.string.amount),
                    prefix = { Text(sim.currency ?: "") },
                    isError = !amountValid,
                    supportingText = if (!amountValid) {
                        { Text(stringResource(R.string.error_non_negative_amount)) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OmniTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = stringResource(R.string.notes_optional),
                    singleLine = false,
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
            }
            Surface(
                modifier = Modifier.imePadding(),
                color = MaterialTheme.colorScheme.background,
            ) {
                OmniPrimaryButton(
                    text = stringResource(R.string.confirm_renewal),
                    onClick = {
                        onConfirm(
                            renewalDate,
                            nextRenewalDate,
                            parsedAmount,
                            notes.takeIf(String::isNotBlank),
                        )
                    },
                    enabled = amountValid,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
fun omniTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    errorContainerColor = MaterialTheme.colorScheme.surface,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface,
    disabledBorderColor = MaterialTheme.colorScheme.outline,
)

@Composable
fun OmniTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    textFieldModifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isError: Boolean = false,
    readOnly: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null,
    prefix: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
) {
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            minLines = minLines,
            keyboardOptions = keyboardOptions,
            isError = isError,
            readOnly = readOnly,
            trailingIcon = trailingIcon,
            prefix = prefix,
            supportingText = supportingText,
            colors = omniTextFieldColors(),
            shape = MaterialTheme.shapes.medium,
            modifier = textFieldModifier
                .semantics { contentDescription = label }
                .fillMaxWidth(),
        )
    }
}

private fun LocalDate.toPickerMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
