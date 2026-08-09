package app.omnisim.android.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.omnisim.android.R
import app.omnisim.android.data.local.entity.RenewalHistoryEntity
import app.omnisim.android.data.local.entity.SimEntity
import app.omnisim.android.domain.util.RenewalHistoryRange
import app.omnisim.android.domain.util.filterRenewalHistory
import app.omnisim.android.ui.components.SimAvatar
import app.omnisim.android.ui.components.displayDate
import app.omnisim.android.ui.components.rememberCurrentDate
import app.omnisim.android.ui.theme.OmniCardPadding
import app.omnisim.android.ui.theme.OmniScreenPadding
import app.omnisim.android.ui.theme.OmniSectionSpacing
import java.util.Locale

@Composable
fun RenewalHistoryScreen(
    history: List<RenewalHistoryEntity>,
    sims: List<SimEntity>,
    onOpenSim: (String) -> Unit,
) {
    val today = rememberCurrentDate()
    var selectedSimId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedRange by rememberSaveable { mutableStateOf(RenewalHistoryRange.All) }
    var showSimMenu by remember { mutableStateOf(false) }
    var showRangeMenu by remember { mutableStateOf(false) }
    val simById = remember(sims) { sims.associateBy(SimEntity::id) }
    val historySimIds = remember(history) { history.mapTo(mutableSetOf(), RenewalHistoryEntity::simId) }
    val availableSims = remember(sims, historySimIds) {
        sims.filter { it.id in historySimIds }.sortedBy { it.name.lowercase(Locale.ROOT) }
    }
    val filteredHistory = remember(history, selectedSimId, selectedRange, today, simById) {
        filterRenewalHistory(
            history = history,
            simId = selectedSimId,
            range = selectedRange,
            today = today,
        ).filter { it.simId in simById }
    }
    val selectedSimName = availableSims.find { it.id == selectedSimId }?.name
        ?: stringResource(R.string.history_all_sims)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = OmniScreenPadding,
            top = 8.dp,
            end = OmniScreenPadding,
            bottom = OmniSectionSpacing,
        ),
    ) {
        item(key = "history-filters") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Column(
                    modifier = Modifier.padding(OmniCardPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        pluralStringResource(
                            R.plurals.renewal_history_record_count,
                            filteredHistory.size,
                            filteredHistory.size,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        HistoryFilterMenu(
                            label = selectedSimName,
                            expanded = showSimMenu,
                            onExpandedChange = { showSimMenu = it },
                            modifier = Modifier.weight(1f),
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.history_all_sims)) },
                                onClick = {
                                    selectedSimId = null
                                    showSimMenu = false
                                },
                            )
                            availableSims.forEach { sim ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = sim.name,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    onClick = {
                                        selectedSimId = sim.id
                                        showSimMenu = false
                                    },
                                )
                            }
                        }
                        HistoryFilterMenu(
                            label = historyRangeLabel(selectedRange),
                            expanded = showRangeMenu,
                            onExpandedChange = { showRangeMenu = it },
                            modifier = Modifier.weight(1f),
                        ) {
                            RenewalHistoryRange.entries.forEach { range ->
                                DropdownMenuItem(
                                    text = { Text(historyRangeLabel(range)) },
                                    onClick = {
                                        selectedRange = range
                                        showRangeMenu = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        if (filteredHistory.isEmpty()) {
            item(key = "history-empty") {
                EmptyHistoryState(hasAnyHistory = history.isNotEmpty())
            }
        } else {
            itemsIndexed(
                items = filteredHistory,
                key = { _, item -> item.id },
            ) { index, item ->
                val sim = simById.getValue(item.simId)
                HistoryTimelineItem(
                    history = item,
                    sim = sim,
                    isFirst = index == 0,
                    isLast = index == filteredHistory.lastIndex,
                    onClick = { onOpenSim(sim.id) },
                )
            }
        }
    }
}

@Composable
private fun HistoryFilterMenu(
    label: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier) {
        Surface(
            onClick = { onExpandedChange(!expanded) },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = CircleShape,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            content = content,
        )
    }
}

@Composable
private fun HistoryTimelineItem(
    history: RenewalHistoryEntity,
    sim: SimEntity,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val nodeColor = MaterialTheme.colorScheme.primary
    val amountText = history.amount?.let { amount ->
        listOfNotNull(
            history.currency?.trim()?.uppercase(Locale.ROOT)?.takeIf(String::isNotEmpty),
            formatHistoryAmount(amount),
        ).joinToString(" ")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
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
                verticalAlignment = Alignment.Top,
            ) {
                SimAvatar(sim.name, Modifier.size(44.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
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
                        amountText?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                            )
                        }
                    }
                    Text(
                        text = stringResource(
                            R.string.history_renewed_on,
                            history.renewalDate.displayDate(),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    history.nextRenewalDate?.let { nextDate ->
                        Text(
                            text = stringResource(
                                R.string.next_date_value,
                                nextDate.displayDate(),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    history.notes?.takeIf(String::isNotBlank)?.let { notes ->
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = notes,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyHistoryState(hasAnyHistory: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(
                    if (hasAnyHistory) R.string.history_no_matches else R.string.no_renewal_history,
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (!hasAnyHistory) {
                Text(
                    text = stringResource(R.string.history_empty_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun historyRangeLabel(range: RenewalHistoryRange): String = stringResource(
    when (range) {
        RenewalHistoryRange.All -> R.string.history_all_time
        RenewalHistoryRange.Last30Days -> R.string.history_last_30_days
        RenewalHistoryRange.Last90Days -> R.string.history_last_90_days
        RenewalHistoryRange.LastYear -> R.string.history_last_year
    },
)

private fun formatHistoryAmount(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else "%.2f".format(value)
