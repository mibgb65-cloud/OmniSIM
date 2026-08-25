package app.omnisim.android.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.omnisim.android.R
import app.omnisim.android.data.preferences.AppSettings
import app.omnisim.android.data.preferences.ReminderCheckResult
import app.omnisim.android.notification.NotificationAvailability
import app.omnisim.android.notification.isReminderCheckFresh
import java.time.Instant

@Composable
internal fun NotificationHealthCard(
    availability: NotificationAvailability,
    settings: AppSettings,
    onSendTestNotification: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
) {
    val context = LocalContext.current
    val lastCheckAt = settings.lastReminderCheckAt
    val fresh = isReminderCheckFresh(lastCheckAt, Instant.now())
    val backgroundHealthy = fresh && settings.lastReminderCheckResult in setOf(
        ReminderCheckResult.NotRun,
        ReminderCheckResult.Success,
    )
    SettingsCard {
        Text(
            stringResource(R.string.notification_health_title),
            style = MaterialTheme.typography.titleSmall,
        )
        NotificationStatusLine(
            title = stringResource(R.string.notification_delivery_status),
            value = notificationDeliveryStatus(availability),
            healthy = availability.canPost,
        )
        NotificationStatusLine(
            title = stringResource(R.string.notification_background_status),
            value = reminderBackgroundStatus(settings, fresh),
            healthy = backgroundHealthy,
        )
        Text(
            stringResource(R.string.background_limitation),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TextButton(onClick = { openBackgroundSettings(context) }) {
                Text(stringResource(R.string.open_background_settings))
            }
            if (!availability.canPost) {
                TextButton(onClick = onOpenNotificationSettings) {
                    Text(stringResource(R.string.open_notification_settings))
                }
            }
            Button(
                onClick = onSendTestNotification,
                enabled = availability.canPost,
                shape = CircleShape,
            ) {
                Text(stringResource(R.string.send_test_notification))
            }
        }
    }
}

@Composable
private fun notificationDeliveryStatus(availability: NotificationAvailability): String = stringResource(
    when {
        !availability.runtimePermissionGranted -> R.string.notification_status_permission_required
        !availability.appNotificationsEnabled -> R.string.notification_status_app_disabled
        !availability.channelEnabled -> R.string.notification_status_channel_disabled
        else -> R.string.notification_status_ready
    },
)

@Composable
private fun reminderBackgroundStatus(settings: AppSettings, fresh: Boolean): String {
    val checkTime = settings.lastReminderCheckAt?.let { reminderCheckTimeLabel(it) }
        ?: return stringResource(R.string.notification_background_not_run)
    return when {
        !fresh -> stringResource(R.string.notification_background_stale, checkTime)
        settings.lastReminderCheckResult == ReminderCheckResult.NotificationsBlocked ->
            stringResource(R.string.notification_background_blocked, checkTime)
        settings.lastReminderCheckResult == ReminderCheckResult.Failed ->
            stringResource(R.string.notification_background_failed, checkTime)
        settings.lastReminderCheckResult == ReminderCheckResult.NotRun ->
            stringResource(R.string.notification_background_previous, checkTime)
        else -> stringResource(
            R.string.notification_background_success,
            checkTime,
            settings.lastReminderCheckScannedCount,
            settings.lastReminderCheckSentCount,
        )
    }
}

@Composable
private fun NotificationStatusLine(title: String, value: String, healthy: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            modifier = Modifier.weight(1.4f),
            style = MaterialTheme.typography.bodySmall,
            color = if (healthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
    }
}

private fun openBackgroundSettings(context: Context) {
    val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    runCatching {
        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }.onFailure { context.startActivity(fallback) }
}
