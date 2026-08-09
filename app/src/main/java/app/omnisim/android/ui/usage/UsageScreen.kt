package app.omnisim.android.ui.usage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.omnisim.android.R
import app.omnisim.android.data.local.entity.SimEntity
import app.omnisim.android.domain.util.CurrencyAmount
import app.omnisim.android.domain.util.ConvertedCostTotal
import app.omnisim.android.domain.util.calculateDailyHoldingCost
import app.omnisim.android.domain.util.calculateConvertedCostTotal
import app.omnisim.android.ui.ExchangeRateUiState
import app.omnisim.android.ui.components.SimAvatar
import app.omnisim.android.ui.components.OmniSectionHeader
import app.omnisim.android.ui.components.displayDate
import app.omnisim.android.ui.theme.OmniCardPadding
import app.omnisim.android.ui.theme.OmniRowSpacing
import app.omnisim.android.ui.theme.OmniScreenPadding
import app.omnisim.android.ui.theme.OmniSectionSpacing
import java.util.Locale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

internal data class SimCost(
    val sim: SimEntity,
    val currency: String,
    val daily: Double,
)

internal data class CurrencyCost(
    val currency: String,
    val daily: Double,
)

@Composable
fun UsageScreen(
    sims: List<SimEntity>,
    historyCount: Int,
    defaultCurrency: String,
    exchangeRateState: ExchangeRateUiState,
    onRefreshRates: () -> Unit,
    onOpenSim: (String) -> Unit,
    onOpenHistory: () -> Unit,
    bottomContentPadding: Dp,
) {
    LaunchedEffect(Unit) { onRefreshRates() }
    val activeSims = remember(sims) { sims.filterNot(SimEntity::archived) }
    val targetCurrency = remember(defaultCurrency) {
        defaultCurrency.trim().uppercase(Locale.ROOT).ifBlank { "USD" }
    }
    val costs = remember(activeSims, defaultCurrency) {
        activeSims.mapNotNull { sim ->
            calculateDailyHoldingCost(
                sim.renewalPrice,
                sim.renewalCycleDays,
                sim.renewalDayOfMonth,
            )?.let { daily ->
                SimCost(
                    sim = sim,
                    currency = sim.currency
                        ?.trim()
                        ?.takeIf(String::isNotEmpty)
                        ?.uppercase(Locale.ROOT)
                        ?: targetCurrency,
                    daily = daily,
                )
            }
        }.sortedByDescending(SimCost::daily)
    }
    val summaries = remember(costs) {
        costs.groupBy(SimCost::currency)
            .map { (currency, items) ->
                CurrencyCost(currency, items.sumOf(SimCost::daily))
            }
            .sortedBy(CurrencyCost::currency)
    }
    val missingCount = activeSims.size - costs.size
    val rateSnapshot = (exchangeRateState as? ExchangeRateUiState.Ready)?.snapshot
    val convertedTotal = remember(costs, targetCurrency, rateSnapshot) {
        calculateConvertedCostTotal(
            costs = costs.map { CurrencyAmount(it.currency, it.daily) },
            targetCurrency = targetCurrency,
            ratesPerEuro = rateSnapshot?.ratesPerEuro.orEmpty(),
        )
    }
    val needsExchangeRates = remember(costs, targetCurrency) {
        costs.any { it.currency != targetCurrency }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = OmniScreenPadding,
            top = 12.dp,
            end = OmniScreenPadding,
            bottom = bottomContentPadding + OmniSectionSpacing,
        ),
        verticalArrangement = Arrangement.spacedBy(OmniRowSpacing),
    ) {
        item(key = "renewal-history-entry") {
            RenewalHistoryEntryCard(
                historyCount = historyCount,
                onClick = onOpenHistory,
            )
        }
        if (summaries.isEmpty()) {
            item {
                EmptyCostState(
                    sims = activeSims,
                    onOpenSim = onOpenSim,
                )
            }
        } else {
            item {
                CostOverviewCard(
                    summaries = summaries,
                    includedCount = costs.size,
                    activeCount = activeSims.size,
                    convertedTotal = convertedTotal,
                    targetCurrency = targetCurrency,
                    exchangeRateState = exchangeRateState,
                    needsExchangeRates = needsExchangeRates,
                )
            }
            item {
                OmniSectionHeader(
                    text = stringResource(R.string.cost_breakdown),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(costs, key = { it.sim.id }) { cost ->
                SimCostRow(cost = cost, onClick = { onOpenSim(cost.sim.id) })
            }
        }

        if (summaries.isNotEmpty() && missingCount > 0) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(
                        pluralStringResource(
                            R.plurals.cost_missing_data,
                            missingCount,
                            missingCount,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(OmniCardPadding),
                    )
                }
            }
        }
    }
}
@Composable
private fun RenewalHistoryEntryCard(
    historyCount: Int,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(
            modifier = Modifier.padding(OmniCardPadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = stringResource(R.string.all_renewal_history),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.renewal_history_entry_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.renewal_history_record_count,
                        historyCount,
                        historyCount,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
@Composable
private fun CostOverviewCard(
    summaries: List<CurrencyCost>,
    includedCount: Int,
    activeCount: Int,
    convertedTotal: ConvertedCostTotal,
    targetCurrency: String,
    exchangeRateState: ExchangeRateUiState,
    needsExchangeRates: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(Modifier.padding(OmniCardPadding)) {
            Text(
                stringResource(R.string.estimated_holding_cost),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                pluralStringResource(
                    R.plurals.cost_coverage,
                    activeCount,
                    includedCount,
                    activeCount,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
            )
            Spacer(Modifier.height(18.dp))
            if (convertedTotal.includedCount > 0) {
                ConvertedTotalSummary(
                    total = convertedTotal,
                    totalCostCount = includedCount,
                    targetCurrency = targetCurrency,
                    exchangeRateState = exchangeRateState,
                    needsExchangeRates = needsExchangeRates,
                )
            } else {
                ExchangeRateUnavailableSummary(
                    targetCurrency = targetCurrency,
                    exchangeRateState = exchangeRateState,
                )
            }
            Spacer(Modifier.height(10.dp))
            summaries.forEachIndexed { index, summary ->
                if (index > 0) Spacer(Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Column(Modifier.padding(OmniCardPadding)) {
                        CurrencySummary(summary)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.cost_estimate_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
            )
        }
    }
}
