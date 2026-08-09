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
    onUpdateRenewal: (String, LocalDate, LocalDate, Double?, String?) -> Unit,
    onUndoRenewal: (String) -> Unit,
    onReminderSettings: (String, Boolean, Set<Int>?) -> Unit,
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
                onUpdateRenewal = onUpdateRenewal,
                onUndoRenewal = onUndoRenewal,
                onReminderSettings = onReminderSettings,
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
