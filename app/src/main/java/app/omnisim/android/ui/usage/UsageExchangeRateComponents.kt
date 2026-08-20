package app.omnisim.android.ui.usage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.omnisim.android.R
import app.omnisim.android.data.exchange.ExchangeRateCoverage
import app.omnisim.android.ui.ExchangeRateFailureReason
import app.omnisim.android.ui.ExchangeRateUiState
import app.omnisim.android.ui.components.SimAvatar
import app.omnisim.android.ui.components.displayDate
import app.omnisim.android.ui.theme.OmniCardPadding
import java.time.Instant
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
internal fun ConvertedTotalSummary(
    total: app.omnisim.android.domain.util.ConvertedCostTotal,
    targetCurrency: String,
    exchangeRateState: ExchangeRateUiState,
    excludedCosts: List<SimCost>,
    onOpenSim: (String) -> Unit,
    onRefreshRates: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(OmniCardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.total_cost),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    targetCurrency,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    formatCost(total.daily),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.per_day_suffix),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 6.dp, bottom = 5.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CostMetric(
                    label = stringResource(R.string.cost_30_days),
                    value = "$targetCurrency ${formatCost(total.daily * 30)}",
                    modifier = Modifier.weight(1f),
                )
                CostMetric(
                    label = stringResource(R.string.cost_365_days),
                    value = "$targetCurrency ${formatCost(total.daily * 365)}",
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(12.dp))
            ExchangeRateStatusLine(exchangeRateState, onRefreshRates)
            ExcludedCurrencyCosts(
                costs = excludedCosts,
                state = exchangeRateState,
                onOpenSim = onOpenSim,
            )
        }
    }
}

@Composable
internal fun ExchangeRateUnavailableSummary(
    targetCurrency: String,
    exchangeRateState: ExchangeRateUiState,
    excludedCosts: List<SimCost>,
    onOpenSim: (String) -> Unit,
    onRefreshRates: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(OmniCardPadding)) {
            Text(
                stringResource(R.string.total_cost),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            ExchangeRateStatusLine(
                state = exchangeRateState,
                onRefreshRates = onRefreshRates,
                targetCurrency = targetCurrency,
            )
            ExcludedCurrencyCosts(
                costs = excludedCosts,
                state = exchangeRateState,
                onOpenSim = onOpenSim,
            )
        }
    }
}

@Composable
private fun ExchangeRateStatusLine(
    state: ExchangeRateUiState,
    onRefreshRates: () -> Unit,
    targetCurrency: String? = null,
) {
    val showProgress = state is ExchangeRateUiState.Idle ||
        state is ExchangeRateUiState.Loading ||
        (state as? ExchangeRateUiState.Ready)?.isRefreshing == true
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showProgress) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = exchangeRateStatusText(state, targetCurrency),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        IconButton(onClick = onRefreshRates, enabled = !showProgress) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = stringResource(R.string.action_refresh_exchange_rates),
            )
        }
    }
}

@Composable
private fun exchangeRateStatusText(
    state: ExchangeRateUiState,
    targetCurrency: String?,
): String = when (state) {
    ExchangeRateUiState.Idle,
    ExchangeRateUiState.Loading,
    -> stringResource(R.string.exchange_rate_loading)
    is ExchangeRateUiState.Unavailable -> stringResource(
        R.string.exchange_rate_unavailable_reason,
        exchangeRateFailureText(state.reason),
    )
    is ExchangeRateUiState.Ready -> readyRateStatusText(state, targetCurrency)
}

@Composable
private fun readyRateStatusText(
    state: ExchangeRateUiState.Ready,
    targetCurrency: String?,
): String {
    val snapshot = state.snapshot
    val lines = mutableListOf<String>()
    if (targetCurrency != null) {
        lines += stringResource(R.string.exchange_rate_target_unsupported, targetCurrency)
    }
    if (snapshot.ecbRateDate != null) {
        lines += stringResource(
            R.string.exchange_rate_ecb_date,
            snapshot.ecbRateDate.displayDate(),
        )
    }
    if (snapshot.inforEuroRateMonth != null) {
        lines += stringResource(
            R.string.exchange_rate_inforeuro_month,
            snapshot.inforEuroRateMonth.displayMonth(),
        )
    }
    if (snapshot.ecbRateDate == null && snapshot.inforEuroRateMonth == null) {
        lines += stringResource(
            R.string.exchange_rate_source,
            exchangeRateSourceName(snapshot.coverage),
            snapshot.rateDate.displayDate(),
        )
    }
    lines += stringResource(
        R.string.exchange_rate_last_updated,
        snapshot.fetchedAt.displayDateTime(),
    )
    if (state.isRefreshing) lines += stringResource(R.string.exchange_rate_refreshing)
    if (state.refreshFailure != null) {
        lines += stringResource(
            R.string.exchange_rate_refresh_failed,
            exchangeRateFailureText(state.refreshFailure),
        )
    }
    return lines.joinToString("\n")
}

@Composable
private fun exchangeRateFailureText(reason: ExchangeRateFailureReason): String = stringResource(
    when (reason) {
        ExchangeRateFailureReason.NetworkOrService -> R.string.exchange_rate_failure_network
        ExchangeRateFailureReason.InvalidData -> R.string.exchange_rate_failure_invalid_data
        ExchangeRateFailureReason.Unknown -> R.string.exchange_rate_failure_unknown
    },
)

@Composable
private fun exchangeRateSourceName(coverage: ExchangeRateCoverage): String = stringResource(
    when (coverage) {
        ExchangeRateCoverage.EcbDaily -> R.string.exchange_rate_provider_ecb
        ExchangeRateCoverage.EcbDailyWithInforEuroMonthly ->
            R.string.exchange_rate_provider_ecb_inforeuro
        ExchangeRateCoverage.InforEuroMonthly -> R.string.exchange_rate_provider_inforeuro
    },
)

@Composable
private fun ExcludedCurrencyCosts(
    costs: List<SimCost>,
    state: ExchangeRateUiState,
    onOpenSim: (String) -> Unit,
) {
    if (costs.isEmpty() || state is ExchangeRateUiState.Idle ||
        state is ExchangeRateUiState.Loading
    ) return
    Spacer(Modifier.height(10.dp))
    Text(
        pluralStringResource(R.plurals.exchange_rate_partial, costs.size, costs.size),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
    Text(
        stringResource(
            R.string.exchange_rate_excluded_currencies,
            costs.map(SimCost::currency).distinct().sorted().joinToString(", "),
        ),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    costs.forEachIndexed { index, cost ->
        if (index > 0) Spacer(Modifier.height(6.dp))
        ExcludedCurrencyRow(cost, onClick = { onOpenSim(cost.sim.id) })
    }
}

@Composable
private fun ExcludedCurrencyRow(cost: SimCost, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { role = Role.Button },
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SimAvatar(cost.sim.name)
            Column(Modifier.weight(1f)) {
                Text(
                    cost.sim.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    cost.sim.carrier,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(cost.currency, style = MaterialTheme.typography.labelLarge)
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun YearMonth.displayMonth(): String {
    val languageTag = LocalLocale.current.toLanguageTag()
    val locale = remember(languageTag) { Locale.forLanguageTag(languageTag) }
    val formatter = remember(locale) { DateTimeFormatter.ofPattern("MMMM yyyy", locale) }
    return format(formatter)
}

@Composable
private fun Instant.displayDateTime(): String {
    val languageTag = LocalLocale.current.toLanguageTag()
    val locale = remember(languageTag) { Locale.forLanguageTag(languageTag) }
    val formatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(locale)
    }
    return atZone(ZoneId.systemDefault()).format(formatter)
}
