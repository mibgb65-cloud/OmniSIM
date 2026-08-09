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

@Composable
internal fun SaveProgressOverlay(
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
internal fun FormProgressHeader(currentStep: Int) {
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
internal fun SimTypeField(
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
internal fun CycleField(
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
