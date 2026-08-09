package app.omnisim.android.ui.home

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import app.omnisim.android.R
import app.omnisim.android.data.local.entity.SimEntity
import app.omnisim.android.data.local.entity.RenewalHistoryEntity
import app.omnisim.android.data.preferences.AppSettings
import app.omnisim.android.domain.model.RenewalStatus
import app.omnisim.android.domain.util.calculateRenewalStatus
import app.omnisim.android.domain.util.countryFlag
import app.omnisim.android.domain.util.daysUntilRenewal
import app.omnisim.android.domain.util.localizedCountryName
import app.omnisim.android.domain.util.maskPhoneNumber
import app.omnisim.android.domain.util.resolvePhoneNumberRegionCode
import app.omnisim.android.ui.components.RenewalSheet
import app.omnisim.android.ui.components.OmniPrimaryButton
import app.omnisim.android.ui.components.OmniSectionHeader
import app.omnisim.android.ui.components.OmniSheetHeader
import app.omnisim.android.ui.components.OmniPageHeader
import app.omnisim.android.ui.components.OmniDialogSystemBars
import app.omnisim.android.ui.components.SimAvatar
import app.omnisim.android.ui.components.StatusChip
import app.omnisim.android.ui.components.daysRemainingLabel
import app.omnisim.android.ui.components.displayDate
import app.omnisim.android.ui.components.label
import app.omnisim.android.ui.components.rememberCurrentDate
import app.omnisim.android.ui.simdetail.SimDetailScreen
import app.omnisim.android.ui.splash.rememberSystemAnimationsEnabled
import java.time.LocalDate
import java.util.Locale

@Composable
fun HomeScreen(
    sims: List<SimEntity>,
    history: List<RenewalHistoryEntity>,
    settings: AppSettings,
    onAdd: () -> Unit,
    onOpenSim: (String) -> Unit,
    onRenew: (SimEntity, LocalDate, LocalDate, Double?, String?) -> Unit,
    onOpenWebsite: (String) -> Unit,
    onEditSim: (String) -> Unit,
    onArchive: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    bottomContentPadding: Dp,
) {
    val today = rememberCurrentDate()
    val statusBarPadding = WindowInsets.statusBars
        .asPaddingValues()
        .calculateTopPadding()
    val active = remember(sims) {
        sims.filterNot(SimEntity::archived).sortedBy(SimEntity::nextRenewalDate)
    }
    var renewalSim by remember { mutableStateOf<SimEntity?>(null) }
    var selectedSimId by rememberSaveable { mutableStateOf<String?>(null) }
    var showSimPicker by rememberSaveable { mutableStateOf(false) }

    if (active.isEmpty()) {
        EmptyHome(onAdd)
    } else {
        val nextSim = active.find { it.id == selectedSimId } ?: active.first()
        val otherSims = active.filterNot { it.id == nextSim.id }
        val listState = rememberLazyListState()

        LaunchedEffect(nextSim.id) {
            listState.scrollToItem(0)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(
                top = statusBarPadding,
                bottom = bottomContentPadding + 24.dp,
            ),
        ) {
            item(key = "renewal-hero") {
                RenewalHero(
                    sim = nextSim,
                    settings = settings,
                    today = today,
                    onSelect = { showSimPicker = true },
                    onOpen = { onOpenSim(nextSim.id) },
                    onRenew = { renewalSim = nextSim },
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OmniSectionHeader(text = stringResource(R.string.upcoming))
                    Text(
                        otherSims.size.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (otherSims.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text(
                                stringResource(R.string.nothing_needs_attention),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.renewals_up_to_date),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = otherSims,
                    key = { _, sim -> "upcoming-${sim.id}" },
                ) { index, sim ->
                    UpcomingTimelineItem(
                        sim = sim,
                        settings = settings,
                        today = today,
                        isFirst = index == 0,
                        isLast = index == otherSims.lastIndex,
                        onClick = { onOpenSim(sim.id) },
                    )
                }
            }
        }

        if (showSimPicker) {
            SimPickerSheet(
                sims = active,
                history = history,
                selectedId = nextSim.id,
                settings = settings,
                today = today,
                onSelect = { sim ->
                    selectedSimId = sim.id
                    showSimPicker = false
                },
                onRenew = onRenew,
                onOpenWebsite = onOpenWebsite,
                onEdit = { sim ->
                    showSimPicker = false
                    onEditSim(sim.id)
                },
                onArchive = { sim, archived -> onArchive(sim.id, archived) },
                onDelete = { sim -> onDelete(sim.id) },
                onAdd = {
                    showSimPicker = false
                    onAdd()
                },
                onDismiss = { showSimPicker = false },
            )
        }
    }

    renewalSim?.let { sim ->
        RenewalSheet(
            sim = sim,
            onDismiss = { renewalSim = null },
            onConfirm = { actual, next, amount, notes ->
                onRenew(sim, actual, next, amount, notes)
                renewalSim = null
            },
        )
    }
}

@Composable
private fun RenewalHero(
    sim: SimEntity,
    settings: AppSettings,
    today: LocalDate,
    onSelect: () -> Unit,
    onOpen: () -> Unit,
    onRenew: () -> Unit,
) {
    val status = calculateRenewalStatus(
        today,
        sim.nextRenewalDate,
        settings.warningPeriodDays,
        false,
    )
    val remaining = daysUntilRenewal(today, sim.nextRenewalDate)
    val phone = if (settings.maskPhoneNumbers) maskPhoneNumber(sim.phoneNumber) else sim.phoneNumber

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 14.dp, end = 20.dp, bottom = 28.dp),
    ) {
        Surface(
            onClick = onSelect,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .widthIn(max = 300.dp)
                .height(56.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = CircleShape,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SimCountryAvatar(sim, Modifier.size(36.dp))
                Column(Modifier.widthIn(max = 190.dp)) {
                    Text(
                        "${sim.name} · ${status.label()}",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        phone ?: sim.carrier,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 46.dp, bottom = 38.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.next_renewal),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.68f),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                daysRemainingLabel(remaining),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                sim.nextRenewalDate.displayDate(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            HeroAction(
                icon = Icons.Default.Check,
                label = stringResource(R.string.mark_as_renewed),
                onClick = onRenew,
            )
            HeroAction(
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                label = stringResource(R.string.action_view_details),
                onClick = onOpen,
            )
        }
    }
}

@Composable
private fun SimCountryAvatar(sim: SimEntity, modifier: Modifier = Modifier) {
    val regionCode = remember(sim.phoneNumber, sim.countryCode) {
        resolvePhoneNumberRegionCode(sim.phoneNumber, sim.countryCode)
    }
    val flag = remember(regionCode) { countryFlag(regionCode) }
    val languageTag = LocalLocale.current.toLanguageTag()
    val countryName = remember(regionCode, languageTag) {
        regionCode?.let {
            localizedCountryName(it, Locale.forLanguageTag(languageTag))
        }
    }
    val description = countryName ?: sim.countryName ?: stringResource(R.string.country)

    Surface(
        modifier = modifier
            .size(44.dp)
            .clearAndSetSemantics { contentDescription = description },
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (flag != null) {
                Text(flag, style = MaterialTheme.typography.titleLarge)
            } else {
                Icon(
                    Icons.Default.Place,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun HeroAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.width(132.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .size(66.dp)
                .semantics {
                    contentDescription = label
                    role = Role.Button
                },
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = CircleShape,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(27.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

@Composable
private fun UpcomingTimelineItem(
    sim: SimEntity,
    settings: AppSettings,
    today: LocalDate,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    val status = calculateRenewalStatus(
        today,
        sim.nextRenewalDate,
        settings.warningPeriodDays,
        false,
    )
    val remaining = daysUntilRenewal(today, sim.nextRenewalDate)
    val phone = if (settings.maskPhoneNumbers) maskPhoneNumber(sim.phoneNumber) else sim.phoneNumber
    val supportingText = listOfNotNull(
        sim.carrier.takeIf(String::isNotBlank),
        phone?.takeIf(String::isNotBlank),
    ).joinToString(" · ")
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val nodeColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 20.dp),
    ) {
        Box(
            modifier = Modifier
                .width(30.dp)
                .fillMaxHeight()
                .drawBehind {
                    val centerX = size.width / 2f
                    val nodeY = 28.dp.toPx()
                    val strokeWidth = 2.dp.toPx()
                    if (!isFirst) {
                        drawLine(
                            color = lineColor,
                            start = Offset(centerX, 0f),
                            end = Offset(centerX, nodeY),
                            strokeWidth = strokeWidth,
                        )
                    }
                    if (!isLast) {
                        drawLine(
                            color = lineColor,
                            start = Offset(centerX, nodeY),
                            end = Offset(centerX, size.height),
                            strokeWidth = strokeWidth,
                        )
                    }
                    drawCircle(
                        color = nodeColor,
                        radius = 5.dp.toPx(),
                        center = Offset(centerX, nodeY),
                    )
                },
        )
        Surface(
            onClick = onClick,
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 12.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SimAvatar(sim.name, Modifier.size(44.dp))
                Column(Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = sim.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        StatusChip(status)
                    }
                    if (supportingText.isNotEmpty()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = supportingText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            sim.nextRenewalDate.displayDate(),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            daysRemainingLabel(remaining),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (
                                status in setOf(RenewalStatus.Overdue, RenewalStatus.DueToday)
                            ) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SimPickerSheet(
    sims: List<SimEntity>,
    history: List<RenewalHistoryEntity>,
    selectedId: String,
    settings: AppSettings,
    today: LocalDate,
    onSelect: (SimEntity) -> Unit,
    onRenew: (SimEntity, LocalDate, LocalDate, Double?, String?) -> Unit,
    onOpenWebsite: (String) -> Unit,
    onEdit: (SimEntity) -> Unit,
    onArchive: (SimEntity, Boolean) -> Unit,
    onDelete: (SimEntity) -> Unit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
) {
    var detailSimId by rememberSaveable { mutableStateOf<String?>(null) }
    val detailSim = detailSimId?.let { id -> sims.find { it.id == id } }
    val animationsEnabled = rememberSystemAnimationsEnabled()
    val windowSize = LocalWindowInfo.current.containerSize
    val screenHeight = with(LocalDensity.current) { windowSize.height.toDp() }
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val collapsedHeight = minOf(
        screenHeight * 0.82f,
        (218 + sims.size * 126).dp,
    )
    val expandedHeight = screenHeight - statusBarHeight - 8.dp
    val sheetHeight = animateDpAsState(
        targetValue = if (detailSim == null) collapsedHeight else expandedHeight,
        animationSpec = if (animationsEnabled) {
            tween(durationMillis = 460, easing = FastOutSlowInEasing)
        } else {
            snap()
        },
        label = "simPickerHeight",
    )
    val scrimAlpha = animateFloatAsState(
        targetValue = if (detailSim == null) 0.32f else 0f,
        animationSpec = if (animationsEnabled) tween(280) else snap(),
        label = "simPickerScrim",
    )
    val scrimColor = MaterialTheme.colorScheme.scrim

    BackHandler(enabled = detailSim != null) { detailSimId = null }

    Dialog(
        onDismissRequest = {
            if (detailSim != null) detailSimId = null else onDismiss()
        },
        properties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        OmniDialogSystemBars()
        val dialogView = LocalView.current
        SideEffect {
            (dialogView.parent as? DialogWindowProvider)?.window?.let { window ->
                window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                window.setDimAmount(0f)
            }
        }
        val dismissInteractionSource = remember { MutableInteractionSource() }
        Box(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind { drawRect(scrimColor.copy(alpha = scrimAlpha.value)) }
                    .clickable(
                        interactionSource = dismissInteractionSource,
                        indication = null,
                        onClick = {
                            if (detailSim != null) detailSimId = null else onDismiss()
                        },
                    ),
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(sheetHeight.value),
                color = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
                shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
            ) {
                AnimatedContent(
                    targetState = detailSim?.id,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        if (animationsEnabled) {
                            fadeIn(tween(durationMillis = 180)) togetherWith
                                fadeOut(tween(durationMillis = 140))
                        } else {
                            fadeIn(snap()) togetherWith fadeOut(snap())
                        }
                    },
                    label = "simPickerToDetails",
                ) { targetId ->
                    val targetSim = targetId?.let { id -> sims.find { it.id == id } }
                    if (targetSim == null) {
                        SimPickerContent(
                            sims = sims,
                            selectedId = selectedId,
                            settings = settings,
                            today = today,
                            onSelect = onSelect,
                            onOpenDetails = { detailSimId = it.id },
                            onAdd = onAdd,
                            onDismiss = onDismiss,
                        )
                    } else {
                        Column(Modifier.fillMaxSize()) {
                            OmniPageHeader(
                                title = stringResource(R.string.title_sim_details),
                                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                                onNavigate = { detailSimId = null },
                            )
                            Box(Modifier.weight(1f)) {
                                SimDetailScreen(
                                    sim = targetSim,
                                    history = history.filter { it.simId == targetSim.id },
                                    settings = settings,
                                    onRenew = { actual, next, amount, notes ->
                                        onRenew(targetSim, actual, next, amount, notes)
                                    },
                                    onOpenWebsite = onOpenWebsite,
                                    onEdit = { onEdit(targetSim) },
                                    onArchive = { onArchive(targetSim, it) },
                                    onDelete = { onDelete(targetSim) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SimPickerContent(
    sims: List<SimEntity>,
    selectedId: String,
    settings: AppSettings,
    today: LocalDate,
    onSelect: (SimEntity) -> Unit,
    onOpenDetails: (SimEntity) -> Unit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OmniSheetHeader(
            title = stringResource(R.string.select_or_add_sim),
            onClose = onDismiss,
        )
        val bottomInset = WindowInsets.navigationBars
            .asPaddingValues()
            .calculateBottomPadding()
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                bottom = 28.dp + bottomInset,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(sims, key = SimEntity::id) { sim ->
                val selected = sim.id == selectedId
                val status = calculateRenewalStatus(
                    today,
                    sim.nextRenewalDate,
                    settings.warningPeriodDays,
                    false,
                )
                val phone = if (settings.maskPhoneNumbers) {
                    maskPhoneNumber(sim.phoneNumber)
                } else {
                    sim.phoneNumber
                }
                Surface(
                    onClick = { onSelect(sim) },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(
                        if (selected) 2.dp else 1.dp,
                        if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected, onClick = null)
                        Column(Modifier.weight(1f)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                StatusChip(status)
                                val scheduleLabel = when {
                                    sim.renewalCycleDays != null -> pluralStringResource(
                                        R.plurals.cycle_days_option,
                                        sim.renewalCycleDays,
                                        sim.renewalCycleDays,
                                    )
                                    sim.renewalDayOfMonth != null -> stringResource(
                                        R.string.monthly_on_day,
                                        sim.renewalDayOfMonth,
                                    )
                                    else -> null
                                }
                                scheduleLabel?.let { label ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        shape = CircleShape,
                                    ) {
                                        Text(
                                            label,
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(sim.name, style = MaterialTheme.typography.titleLarge)
                            Text(
                                phone ?: sim.carrier,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Surface(
                            onClick = { onOpenDetails(sim) },
                            modifier = Modifier.size(52.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            shape = CircleShape,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.action_view_details),
                                )
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    onClick = onAdd,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                        )
                        Text(
                            stringResource(R.string.add_new_sim),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.sim_picker_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyHome(onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.no_sims_yet),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.no_sims_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        OmniPrimaryButton(
            onClick = onAdd,
            text = stringResource(R.string.action_add_sim),
        )
    }
}
