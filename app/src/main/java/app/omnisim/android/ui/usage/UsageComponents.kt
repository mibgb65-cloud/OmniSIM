package app.omnisim.android.ui.usage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.omnisim.android.R
import app.omnisim.android.data.local.entity.SimEntity
import app.omnisim.android.ui.components.SimAvatar
import app.omnisim.android.ui.components.OmniSectionHeader
import app.omnisim.android.ui.theme.OmniCardPadding
import app.omnisim.android.ui.theme.OmniSectionSpacing
import java.util.Locale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

@Composable
internal fun CurrencySummary(summary: CurrencyCost) {
    Text(
        summary.currency,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            formatCost(summary.daily),
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
            value = "${summary.currency} ${formatCost(summary.daily * 30)}",
            modifier = Modifier.weight(1f),
        )
        CostMetric(
            label = stringResource(R.string.cost_365_days),
            value = "${summary.currency} ${formatCost(summary.daily * 365)}",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun CostMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun SimCostRow(cost: SimCost, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { role = Role.Button },
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.padding(OmniCardPadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SimAvatar(cost.sim.name)
            Column(Modifier.weight(1f)) {
                Text(
                    cost.sim.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    cost.sim.carrier,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (cost.sim.renewalDayOfMonth != null) {
                        stringResource(
                            R.string.cost_monthly_value,
                            cost.currency,
                            formatCost(cost.sim.renewalPrice ?: 0.0),
                            cost.sim.renewalDayOfMonth,
                        )
                    } else {
                        pluralStringResource(
                            R.plurals.cost_cycle_value,
                            cost.sim.renewalCycleDays ?: 0,
                            cost.currency,
                            formatCost(cost.sim.renewalPrice ?: 0.0),
                            cost.sim.renewalCycleDays ?: 0,
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${cost.currency} ${formatCost(cost.daily)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.per_day),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun EmptyCostState(
    sims: List<SimEntity>,
    onOpenSim: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        CostIllustration()
        Spacer(Modifier.height(22.dp))
        Text(
            stringResource(R.string.no_cost_data),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.no_cost_data_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        if (sims.isNotEmpty()) {
            Spacer(Modifier.height(OmniSectionSpacing))
            OmniSectionHeader(
                text = stringResource(R.string.cost_setup_title),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            sims.forEachIndexed { index, sim ->
                MissingCostRow(sim = sim, onClick = { onOpenSim(sim.id) })
                if (index != sims.lastIndex) Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun CostIllustration() {
    Surface(
        modifier = Modifier.size(146.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = CircleShape,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 40.dp, vertical = 38.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            listOf(30.dp, 54.dp, 42.dp).forEach { barHeight ->
                Surface(
                    modifier = Modifier
                        .width(12.dp)
                        .height(barHeight),
                    color = Color(0xFF111315),
                    shape = CircleShape,
                ) {}
            }
        }
    }
}

@Composable
internal fun MissingCostRow(sim: SimEntity, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { role = Role.Button },
        color = MaterialTheme.colorScheme.surface,
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
                Text(
                    stringResource(R.string.cost_setup_sim_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

internal fun formatCost(value: Double): String {
    val pattern = if (value > 0.0 && value < 0.01) "%.4f" else "%.2f"
    return String.format(Locale.getDefault(), pattern, value)
}
