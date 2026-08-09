package app.omnisim.android.ui.editsim

import android.os.Build
import android.os.SystemClock
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import app.omnisim.android.R
import app.omnisim.android.backup.isSafeWebUrl
import app.omnisim.android.data.local.entity.SimEntity
import app.omnisim.android.domain.util.callingCodeCountries
import app.omnisim.android.domain.util.calculateNextRenewalDate
import app.omnisim.android.domain.util.calculateScheduledNextRenewalDate
import app.omnisim.android.domain.util.formatInternationalPhoneNumber
import app.omnisim.android.domain.util.isSupportedCurrencyCode
import app.omnisim.android.domain.util.splitInternationalPhoneNumber
import app.omnisim.android.ui.components.CurrencyPickerField
import app.omnisim.android.ui.SimDraft
import app.omnisim.android.ui.components.DateField
import app.omnisim.android.ui.components.OmniPrimaryButton
import app.omnisim.android.ui.components.OmniSecondaryButton
import app.omnisim.android.ui.components.OmniTextField
import app.omnisim.android.ui.components.omniTextFieldColors
import app.omnisim.android.ui.components.rememberCurrentDate
import java.time.LocalDate
import kotlin.math.hypot
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val cyclePresets = listOf(30, 60, 90, 120, 180, 365)
private const val CustomCycle = -1
private const val MonthlyCycle = -2
private const val FormStepCount = 3
private const val MinimumSavingDurationMillis = 1_000L
private const val SuccessCheckDurationMillis = 520
private const val SuccessHoldDurationMillis = 350L

private enum class SaveTransitionState {
    Idle,
    Saving,
    Success,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSimScreen(
    existing: SimEntity?,
    defaultCurrency: String,
    onSave: (SimDraft, (Boolean) -> Unit) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialCycle = existing?.renewalCycleDays ?: 90
    val today = rememberCurrentDate()
    val configuration = LocalConfiguration.current
    @Suppress("DEPRECATION")
    val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        configuration.locales[0]
    } else {
        configuration.locale
    }
    val countries = remember(locale) { callingCodeCountries(locale) }
    val initialPhoneParts = remember(existing?.id, locale) {
        splitInternationalPhoneNumber(
            value = existing?.phoneNumber,
            fallbackRegionCode = existing?.countryCode,
            locale = locale,
        )
    }
    val initialRegionCode = initialPhoneParts.regionCode
        ?: locale.country.takeIf { deviceRegion -> countries.any { it.regionCode == deviceRegion } }
        ?: "US"

    var currentStep by remember(existing?.id) { mutableIntStateOf(0) }
    var attemptedStep by remember(existing?.id) { mutableStateOf(false) }
    var name by remember(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var carrier by remember(existing?.id) { mutableStateOf(existing?.carrier.orEmpty()) }
    var planName by remember(existing?.id) { mutableStateOf(existing?.planName.orEmpty()) }
    var selectedRegionCode by remember(existing?.id) { mutableStateOf(initialRegionCode) }
    var nationalNumber by remember(existing?.id) {
        mutableStateOf(initialPhoneParts.nationalNumber)
    }
    var simType by remember(existing?.id) { mutableStateOf(existing?.simType ?: "eSIM") }
    var lastRenewalDate by remember(existing?.id) { mutableStateOf(existing?.lastRenewalDate) }
    var nextDate by remember(existing?.id) {
        mutableStateOf(
            existing?.nextRenewalDate
                ?: calculateNextRenewalDate(today, initialCycle),
        )
    }
    var cycleSelection by remember(existing?.id) {
        mutableStateOf<Int?>(
            when {
                existing?.renewalDayOfMonth != null -> MonthlyCycle
                existing?.renewalCycleDays == null -> if (existing == null) 90 else null
                existing.renewalCycleDays in cyclePresets -> existing.renewalCycleDays
                else -> CustomCycle
            },
        )
    }
    var customCycle by remember(existing?.id) {
        mutableStateOf(
            existing?.renewalCycleDays
                ?.takeIf { it !in cyclePresets }
                ?.toString()
                .orEmpty(),
        )
    }
    var monthlyDay by remember(existing?.id) {
        mutableStateOf(
            (existing?.renewalDayOfMonth ?: existing?.nextRenewalDate?.dayOfMonth ?: today.dayOfMonth)
                .toString(),
        )
    }
    var price by remember(existing?.id) { mutableStateOf(existing?.renewalPrice?.toString().orEmpty()) }
    var currency by remember(existing?.id) {
        mutableStateOf(existing?.currency ?: defaultCurrency.uppercase())
    }
    var renewalUrl by remember(existing?.id) { mutableStateOf(existing?.renewalUrl.orEmpty()) }
    var notes by remember(existing?.id) { mutableStateOf(existing?.notes.orEmpty()) }
    var cycleExpanded by remember { mutableStateOf(false) }
    var isSubmitting by remember(existing?.id) { mutableStateOf(false) }
    var saveTransitionState by remember(existing?.id) {
        mutableStateOf(SaveTransitionState.Idle)
    }
    val coroutineScope = rememberCoroutineScope()

    val cycle = when (cycleSelection) {
        CustomCycle -> customCycle.toIntOrNull()
        MonthlyCycle -> null
        else -> cycleSelection
    }
    val renewalDayOfMonth = monthlyDay.toIntOrNull().takeIf { cycleSelection == MonthlyCycle }
    val parsedPrice = price.toDoubleOrNull()
    val selectedCountry = countries.firstOrNull { it.regionCode == selectedRegionCode }
        ?: countries.first { it.regionCode == "US" }
    val phone = remember(selectedCountry.regionCode, nationalNumber) {
        formatInternationalPhoneNumber(selectedCountry.regionCode, nationalNumber).orEmpty()
    }
    val savedCountryCode = selectedCountry.regionCode
    val savedCountryName = selectedCountry.countryName

    @StringRes val identityError = when {
        name.isBlank() -> R.string.error_name_required
        carrier.isBlank() -> R.string.error_carrier_required
        simType !in setOf("eSIM", "Physical SIM") -> R.string.error_invalid_sim_type
        else -> 0
    }
    @StringRes val renewalError = when {
        cycleSelection == CustomCycle && customCycle.toIntOrNull() == null ->
            R.string.error_valid_custom_cycle
        cycleSelection == CustomCycle && (cycle ?: 0) <= 0 -> R.string.error_positive_cycle
        cycleSelection == MonthlyCycle &&
            (renewalDayOfMonth == null || renewalDayOfMonth !in 1..31) ->
            R.string.error_monthly_renewal_day
        else -> 0
    }
    @StringRes val costError = when {
        price.isNotBlank() && parsedPrice == null -> R.string.error_valid_renewal_price
        parsedPrice != null && (parsedPrice < 0 || !parsedPrice.isFinite()) ->
            R.string.error_non_negative_price
        !isSupportedCurrencyCode(currency) -> R.string.error_valid_currency
        !isSafeWebUrl(renewalUrl) -> R.string.error_valid_website
        else -> 0
    }
    val stepError = when (currentStep) {
        0 -> identityError
        1 -> renewalError
        else -> costError
    }

    val draft = SimDraft(
        id = existing?.id,
        name = name,
        carrier = carrier,
        countryCode = savedCountryCode,
        countryName = savedCountryName,
        phoneNumber = phone,
        simType = simType,
        planName = planName,
        lastRenewalDate = lastRenewalDate,
        nextRenewalDate = nextDate,
        renewalCycleDays = cycle,
        renewalDayOfMonth = renewalDayOfMonth,
        renewalPrice = parsedPrice,
        currency = currency,
        renewalUrl = renewalUrl,
        notes = notes,
    )

    val cycleLabel = when (cycleSelection) {
        null -> stringResource(R.string.no_automatic_cycle)
        CustomCycle -> stringResource(R.string.custom)
        MonthlyCycle -> renewalDayOfMonth?.let {
            stringResource(R.string.monthly_on_day, it)
        } ?: stringResource(R.string.monthly_fixed_day)
        else -> pluralStringResource(
            R.plurals.cycle_days_option,
            cycleSelection ?: 0,
            cycleSelection ?: 0,
        )
    }
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            FormProgressHeader(currentStep)

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { it / 5 } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it / 5 } + fadeOut())
                    } else {
                        (slideInHorizontally { -it / 5 } + fadeIn()) togetherWith
                            (slideOutHorizontally { it / 5 } + fadeOut())
                    }
                },
                label = "SIM form step",
                modifier = Modifier.weight(1f),
            ) { step ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    when (step) {
                    0 -> {
                        OmniTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = stringResource(R.string.display_name),
                            isError = attemptedStep && name.isBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OmniTextField(
                            value = carrier,
                            onValueChange = { carrier = it },
                            label = stringResource(R.string.carrier),
                            isError = attemptedStep && carrier.isBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OmniTextField(
                            value = planName,
                            onValueChange = { planName = it },
                            label = stringResource(R.string.plan_name_optional),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        CountryPhoneField(
                            countries = countries,
                            selectedCountry = selectedCountry,
                            nationalNumber = nationalNumber,
                            locale = locale,
                            onCountrySelected = { selectedRegionCode = it.regionCode },
                            onNationalNumberChange = { value ->
                                if (value.trimStart().startsWith("+")) {
                                    val parts = splitInternationalPhoneNumber(
                                        value = value,
                                        fallbackRegionCode = selectedRegionCode,
                                        locale = locale,
                                    )
                                    parts.regionCode?.let { selectedRegionCode = it }
                                    nationalNumber = parts.nationalNumber
                                } else {
                                    nationalNumber = value
                                }
                            },
                        )
                        SimTypeField(
                            selectedValue = simType,
                            onSelected = { simType = it },
                        )
                    }

                    1 -> {
                        lastRenewalDate?.let { currentLastRenewalDate ->
                            DateField(
                                label = stringResource(R.string.last_renewal),
                                value = currentLastRenewalDate,
                                onValueChange = { selectedDate ->
                                    lastRenewalDate = selectedDate
                                    calculateScheduledNextRenewalDate(
                                        selectedDate,
                                        cycle,
                                        renewalDayOfMonth,
                                    )?.let { nextDate = it }
                                },
                            )
                            TextButton(onClick = { lastRenewalDate = null }) {
                                Text(stringResource(R.string.clear_last_renewal_date))
                            }
                        } ?: TextButton(
                            onClick = {
                                lastRenewalDate = today
                                calculateScheduledNextRenewalDate(
                                    today,
                                    cycle,
                                    renewalDayOfMonth,
                                )?.let { nextDate = it }
                            },
                        ) {
                            Text(stringResource(R.string.add_last_renewal_date))
                        }
                        CycleField(
                            label = cycleLabel,
                            expanded = cycleExpanded,
                            onExpandedChange = { cycleExpanded = it },
                            onSelected = { selectedCycle ->
                                cycleSelection = selectedCycle
                                cycleExpanded = false
                                val selectedCycleDays = selectedCycle?.takeIf { it > 0 }
                                val selectedMonthlyDay = monthlyDay.toIntOrNull()
                                    ?.takeIf { selectedCycle == MonthlyCycle && it in 1..31 }
                                calculateScheduledNextRenewalDate(
                                    lastRenewalDate ?: today,
                                    selectedCycleDays,
                                    selectedMonthlyDay,
                                )?.let {
                                    nextDate = it
                                }
                            },
                        )
                        if (cycleSelection == CustomCycle) {
                            OmniTextField(
                                value = customCycle,
                                onValueChange = { value ->
                                    customCycle = value.filter(Char::isDigit)
                                    customCycle.toIntOrNull()?.takeIf { it > 0 }?.let {
                                        nextDate = calculateNextRenewalDate(lastRenewalDate ?: today, it)
                                    }
                                },
                                label = stringResource(R.string.custom_holding_cycle_days),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = attemptedStep && (customCycle.toIntOrNull() ?: 0) <= 0,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (cycleSelection == MonthlyCycle) {
                            OmniTextField(
                                value = monthlyDay,
                                onValueChange = { value ->
                                    monthlyDay = value.filter(Char::isDigit).take(2)
                                    monthlyDay.toIntOrNull()?.takeIf { it in 1..31 }?.let { day ->
                                        nextDate = calculateScheduledNextRenewalDate(
                                            lastRenewalDate ?: today,
                                            null,
                                            day,
                                        ) ?: nextDate
                                    }
                                },
                                label = stringResource(R.string.monthly_day_of_month),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = attemptedStep &&
                                    (renewalDayOfMonth == null || renewalDayOfMonth !in 1..31),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        DateField(
                            label = stringResource(R.string.next_holding_date),
                            value = nextDate,
                            onValueChange = { nextDate = it },
                        )
                        Text(
                            text = stringResource(
                                if (cycleSelection == null) {
                                    R.string.next_holding_date_manual_hint
                                } else {
                                    R.string.next_holding_date_hint
                                },
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    else -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OmniTextField(
                                value = price,
                                onValueChange = { price = it },
                                label = stringResource(R.string.holding_cost_optional),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                isError = attemptedStep && price.isNotBlank() &&
                                    (parsedPrice == null || parsedPrice < 0),
                                modifier = Modifier.weight(1f),
                            )
                            CurrencyPickerField(
                                selectedCode = currency,
                                onSelected = { currency = it },
                                label = stringResource(R.string.currency),
                                modifier = Modifier.weight(0.62f),
                            )
                        }
                        Text(
                            text = stringResource(R.string.per_sim_currency_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OmniTextField(
                            value = renewalUrl,
                            onValueChange = { renewalUrl = it },
                            label = stringResource(R.string.renewal_website),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
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
                    }
                }

                if (attemptedStep && stepError != 0) {
                    Text(
                        text = stringResource(stepError),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (currentStep > 0) {
                    OmniSecondaryButton(
                        text = stringResource(R.string.action_previous_step),
                        onClick = {
                            attemptedStep = false
                            currentStep--
                        },
                        modifier = Modifier.weight(0.42f),
                        enabled = !isSubmitting,
                    )
                }
                OmniPrimaryButton(
                    text = when {
                        currentStep < FormStepCount - 1 -> stringResource(R.string.action_next_step)
                        existing == null -> stringResource(R.string.action_save_sim)
                        else -> stringResource(R.string.action_save_changes)
                    },
                    onClick = {
                        attemptedStep = true
                        if (stepError == 0) {
                            if (currentStep < FormStepCount - 1) {
                                attemptedStep = false
                                currentStep++
                            } else {
                                val saveStartedAt = SystemClock.elapsedRealtime()
                                isSubmitting = true
                                if (existing == null) {
                                    saveTransitionState = SaveTransitionState.Saving
                                }
                                onSave(draft) { saved ->
                                    if (!saved) {
                                        isSubmitting = false
                                        saveTransitionState = SaveTransitionState.Idle
                                    } else if (existing != null) {
                                        onDone()
                                    } else {
                                        coroutineScope.launch {
                                            val elapsed = SystemClock.elapsedRealtime() - saveStartedAt
                                            delay(
                                                (MinimumSavingDurationMillis - elapsed)
                                                    .coerceAtLeast(0L),
                                            )
                                            saveTransitionState = SaveTransitionState.Success
                                        }
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isSubmitting,
                )
            }
        }
        }

        if (saveTransitionState != SaveTransitionState.Idle) {
            SaveProgressOverlay(
                state = saveTransitionState,
                onSuccessAnimationFinished = onDone,
            )
        }
    }
}

@Composable
private fun SaveProgressOverlay(
    state: SaveTransitionState,
    onSuccessAnimationFinished: () -> Unit,
) {
    val statusText = stringResource(
        if (state == SaveTransitionState.Saving) {
            R.string.saving_sim
        } else {
            R.string.message_sim_saved
        },
    )
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            }
            .clearAndSetSemantics {
                contentDescription = statusText
                liveRegion = LiveRegionMode.Polite
            },
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (state == SaveTransitionState.Saving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(58.dp),
                    strokeWidth = 5.dp,
                )
            } else {
                AnimatedSuccessCheck(onAnimationFinished = onSuccessAnimationFinished)
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun AnimatedSuccessCheck(onAnimationFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val circleColor = MaterialTheme.colorScheme.primaryContainer
    val checkColor = MaterialTheme.colorScheme.onPrimaryContainer

    androidx.compose.runtime.LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = SuccessCheckDurationMillis,
                easing = LinearOutSlowInEasing,
            ),
        )
        delay(SuccessHoldDurationMillis)
        onAnimationFinished()
    }

    Canvas(modifier = Modifier.size(104.dp)) {
        drawCircle(color = circleColor)

        val start = Offset(size.width * 0.24f, size.height * 0.52f)
        val middle = Offset(size.width * 0.43f, size.height * 0.70f)
        val end = Offset(size.width * 0.79f, size.height * 0.31f)
        val firstLength = hypot(
            (middle.x - start.x).toDouble(),
            (middle.y - start.y).toDouble(),
        ).toFloat()
        val secondLength = hypot(
            (end.x - middle.x).toDouble(),
            (end.y - middle.y).toDouble(),
        ).toFloat()
        val firstPortion = firstLength / (firstLength + secondLength)
        val strokeWidth = 6.dp.toPx()

        if (progress.value <= firstPortion) {
            val segmentProgress = progress.value / firstPortion
            drawLine(
                color = checkColor,
                start = start,
                end = Offset(
                    x = start.x + (middle.x - start.x) * segmentProgress,
                    y = start.y + (middle.y - start.y) * segmentProgress,
                ),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        } else {
            drawLine(
                color = checkColor,
                start = start,
                end = middle,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            val segmentProgress = (progress.value - firstPortion) / (1f - firstPortion)
            drawLine(
                color = checkColor,
                start = middle,
                end = Offset(
                    x = middle.x + (end.x - middle.x) * segmentProgress,
                    y = middle.y + (end.y - middle.y) * segmentProgress,
                ),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun FormProgressHeader(currentStep: Int) {
    val titles = listOf(
        stringResource(R.string.form_step_sim_information),
        stringResource(R.string.form_step_holding_rules),
        stringResource(R.string.form_step_cost_and_notes),
    )
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(
                R.string.form_step_progress,
                currentStep + 1,
                FormStepCount,
            ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = titles[currentStep],
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        LinearProgressIndicator(
            progress = { (currentStep + 1f) / FormStepCount },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }
}

@Composable
private fun SimTypeField(
    selectedValue: String,
    onSelected: (String) -> Unit,
) {
    val options = listOf(
        "eSIM" to R.string.type_esim,
        "Physical SIM" to R.string.type_physical_sim,
    )
    Column {
        Text(
            stringResource(R.string.sim_type),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
            ) {
                val itemSpacing = 4.dp
                val itemWidth = (maxWidth - itemSpacing) / options.size
                val selectedIndex = options.indexOfFirst { it.first == selectedValue }
                    .coerceAtLeast(0)
                val indicatorOffset by animateDpAsState(
                    targetValue = (itemWidth + itemSpacing) * selectedIndex,
                    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                    label = "SIM type indicator",
                )

                Surface(
                    modifier = Modifier
                        .offset { IntOffset(indicatorOffset.roundToPx(), 0) }
                        .width(itemWidth)
                        .fillMaxHeight(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small,
                ) {}

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                ) {
                    options.forEach { (value, labelRes) ->
                        val selected = selectedValue == value
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .selectable(
                                    selected = selected,
                                    onClick = { onSelected(value) },
                                    role = Role.RadioButton,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(labelRes),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CycleField(
    label: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelected: (Int?) -> Unit,
) {
    Column {
        Text(
            stringResource(R.string.holding_cycle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpandedChange) {
            OutlinedTextField(
                value = label,
                onValueChange = {},
                readOnly = true,
                colors = omniTextFieldColors(),
                shape = MaterialTheme.shapes.medium,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.no_automatic_cycle)) },
                    onClick = { onSelected(null) },
                )
                cyclePresets.forEach { days ->
                    DropdownMenuItem(
                        text = { Text(pluralStringResource(R.plurals.cycle_days_option, days, days)) },
                        onClick = { onSelected(days) },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.monthly_fixed_day)) },
                    onClick = { onSelected(MonthlyCycle) },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.custom)) },
                    onClick = { onSelected(CustomCycle) },
                )
            }
        }
    }
}
