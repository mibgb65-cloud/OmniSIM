package app.omnisim.android.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.omnisim.android.R
import app.omnisim.android.data.local.entity.SimEntity
import app.omnisim.android.data.preferences.AppSettings
import app.omnisim.android.domain.util.calculateRenewalStatus
import app.omnisim.android.domain.util.daysUntilRenewal
import app.omnisim.android.domain.util.maskPhoneNumber
import app.omnisim.android.ui.components.SimAvatar
import app.omnisim.android.ui.components.StatusChip
import app.omnisim.android.ui.components.daysRemainingLabel
import app.omnisim.android.ui.components.displayDate
import java.time.LocalDate

@Composable
internal fun AttentionRenewalCard(
    sim: SimEntity,
    settings: AppSettings,
    today: LocalDate,
    onOpen: () -> Unit,
    onRenew: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val remaining = daysUntilRenewal(today, sim.nextRenewalDate)
    val status = calculateRenewalStatus(today, sim.nextRenewalDate, settings.warningPeriodDays, false)
    val phone = if (settings.maskPhoneNumbers) maskPhoneNumber(sim.phoneNumber) else sim.phoneNumber
    val identity = listOfNotNull(
        sim.carrier.takeIf(String::isNotBlank),
        phone?.takeIf(String::isNotBlank),
    ).joinToString(" · ")
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpen)
                    .semantics { role = Role.Button }
                    .padding(16.dp),
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
                            sim.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        StatusChip(status)
                    }
                    if (identity.isNotEmpty()) {
                        Text(
                            identity,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${daysRemainingLabel(remaining)} · ${sim.nextRenewalDate.displayDate()}",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(cycleLabel(sim), style = MaterialTheme.typography.bodySmall)
                }
            }
            Button(
                onClick = onRenew,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            ) {
                Text(stringResource(R.string.mark_as_renewed))
            }
        }
    }
}

@Composable
private fun cycleLabel(sim: SimEntity): String = when {
    sim.renewalCycleDays != null -> pluralStringResource(
        R.plurals.cycle_days_option,
        sim.renewalCycleDays,
        sim.renewalCycleDays,
    )
    sim.renewalDayOfMonth != null -> stringResource(R.string.monthly_on_day, sim.renewalDayOfMonth)
    else -> stringResource(R.string.no_automatic_cycle)
}
