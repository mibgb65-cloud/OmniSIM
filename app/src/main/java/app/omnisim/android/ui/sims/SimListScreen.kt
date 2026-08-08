package app.omnisim.android.ui.sims

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import app.omnisim.android.R
import app.omnisim.android.data.local.entity.SimEntity
import app.omnisim.android.data.preferences.AppSettings
import app.omnisim.android.domain.model.RenewalStatus
import app.omnisim.android.domain.util.calculateRenewalStatus
import app.omnisim.android.ui.components.SimSummaryRow
import app.omnisim.android.ui.splash.rememberSystemAnimationsEnabled
import app.omnisim.android.ui.theme.OmniRowSpacing
import app.omnisim.android.ui.theme.OmniScreenPadding
import java.time.LocalDate

private enum class SimFilter(@param:StringRes val label: Int) {
    Active(R.string.filter_active),
    DueSoon(R.string.filter_due_soon),
    Overdue(R.string.filter_overdue),
    Archived(R.string.filter_archived),
}

@Composable
fun SimListScreen(
    sims: List<SimEntity>,
    settings: AppSettings,
    onOpenSim: (String) -> Unit,
    bottomContentPadding: Dp,
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(SimFilter.Active) }
    var searchFocused by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val collapseThreshold = with(LocalDensity.current) { 12.dp.roundToPx() }
    val listScrolled by remember(listState, collapseThreshold) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 ||
                listState.firstVisibleItemScrollOffset > collapseThreshold
        }
    }
    val showSearch = query.isNotBlank() || searchFocused || !listScrolled
    val animationsEnabled = rememberSystemAnimationsEnabled()
    val searchLabel = stringResource(R.string.search_sims)
    val today = LocalDate.now()
    val visible = remember(sims, query, filter, settings.warningPeriodDays) {
        sims.filter { sim ->
            val status = calculateRenewalStatus(
                today,
                sim.nextRenewalDate,
                settings.warningPeriodDays,
                sim.archived,
            )
            val matchesFilter = when (filter) {
                SimFilter.Active -> !sim.archived
                SimFilter.DueSoon -> status in setOf(RenewalStatus.DueSoon, RenewalStatus.DueToday)
                SimFilter.Overdue -> status == RenewalStatus.Overdue
                SimFilter.Archived -> status == RenewalStatus.Archived
            }
            val needle = query.trim()
            val matchesSearch = needle.isEmpty() || listOfNotNull(
                sim.name,
                sim.carrier,
                sim.phoneNumber,
                sim.countryName,
                sim.countryCode,
            ).any { it.contains(needle, ignoreCase = true) }
            matchesFilter && matchesSearch
        }.sortedBy(SimEntity::nextRenewalDate)
    }

    Column(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = showSearch,
            enter = if (animationsEnabled) {
                expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                ) + fadeIn(tween(160))
            } else {
                EnterTransition.None
            },
            exit = if (animationsEnabled) {
                shrinkVertically(
                    shrinkTowards = Alignment.Top,
                    animationSpec = tween(200, easing = FastOutSlowInEasing),
                ) + fadeOut(tween(140))
            } else {
                ExitTransition.None
            },
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = OmniScreenPadding, vertical = 10.dp)
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { searchFocused = it.isFocused }
                            .semantics { contentDescription = searchLabel },
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (query.isEmpty()) {
                                    Text(
                                        searchLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                }
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = OmniScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(SimFilter.entries, key = SimFilter::name) { item ->
                val selected = filter == item
                Surface(
                    onClick = { filter = item },
                    modifier = Modifier
                        .height(44.dp)
                        .semantics { this.selected = selected },
                    shape = CircleShape,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    contentColor = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    border = if (selected) null else BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                    ),
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stringResource(item.label), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
        if (visible.isEmpty()) {
            Text(
                if (query.isBlank()) {
                    stringResource(R.string.no_filtered_sims, stringResource(filter.label))
                } else {
                    stringResource(R.string.no_matching_sims)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(32.dp),
            )
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    start = OmniScreenPadding,
                    top = 16.dp,
                    end = OmniScreenPadding,
                    bottom = bottomContentPadding + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(OmniRowSpacing),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(visible, key = SimEntity::id) { sim ->
                    SimSummaryRow(
                        sim = sim,
                        status = calculateRenewalStatus(
                            today,
                            sim.nextRenewalDate,
                            settings.warningPeriodDays,
                            sim.archived,
                        ),
                        maskNumbers = settings.maskPhoneNumbers,
                        today = today,
                        onClick = { onOpenSim(sim.id) },
                    )
                }
            }
        }
    }
}
