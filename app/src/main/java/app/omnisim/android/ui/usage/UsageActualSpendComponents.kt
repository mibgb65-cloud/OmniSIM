package app.omnisim.android.ui.usage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.omnisim.android.R
import app.omnisim.android.domain.util.ActualSpendByCurrency
import app.omnisim.android.domain.util.ActualSpendSummary
import app.omnisim.android.ui.theme.OmniCardPadding

@Composable
internal fun ActualSpendCard(
    summary: ActualSpendSummary,
    onOpenHistory: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(Modifier.padding(OmniCardPadding)) {
            Text(
                stringResource(R.string.actual_spend_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(R.string.actual_spend_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            if (summary.byCurrency.isEmpty()) {
                Text(
                    stringResource(R.string.actual_spend_no_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                summary.byCurrency.forEachIndexed { index, spend ->
                    if (index > 0) {
                        HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    }
                    ActualSpendCurrencyRow(spend)
                }
            }
            if (summary.incompleteRecordCount > 0) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onOpenHistory, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        pluralStringResource(
                            R.plurals.actual_spend_incomplete,
                            summary.incompleteRecordCount,
                            summary.incompleteRecordCount,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.actual_spend_note),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ActualSpendCurrencyRow(spend: ActualSpendByCurrency) {
    Text(
        spend.currency,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        CostMetric(
            label = stringResource(R.string.actual_spend_30_days),
            value = "${spend.currency} ${formatCost(spend.last30Days)}",
            modifier = Modifier.weight(1f),
        )
        CostMetric(
            label = stringResource(R.string.actual_spend_365_days),
            value = "${spend.currency} ${formatCost(spend.last365Days)}",
            modifier = Modifier.weight(1f),
        )
    }
}
