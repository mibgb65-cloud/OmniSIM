package app.omnisim.android.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.os.ConfigurationCompat
import app.omnisim.android.MainActivity
import app.omnisim.android.R
import app.omnisim.android.data.local.entity.SimEntity
import app.omnisim.android.domain.util.maskPhoneNumber
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class NotificationHelper(private val context: Context) {
    fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notification_channel_description)
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun availability(): NotificationAvailability {
        val runtimePermissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        val appNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        val channelEnabled = Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.getSystemService(NotificationManager::class.java)
                .getNotificationChannel(CHANNEL_ID)
                ?.importance != NotificationManager.IMPORTANCE_NONE
        return NotificationAvailability(
            runtimePermissionGranted = runtimePermissionGranted,
            appNotificationsEnabled = appNotificationsEnabled,
            channelEnabled = channelEnabled,
        )
    }

    fun notificationsEnabled(): Boolean = availability().canPost

    fun showTest(): Boolean {
        if (!notificationsEnabled()) return false
        val pendingIntent = PendingIntent.getActivity(
            context,
            TEST_NOTIFICATION_ID,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.test_notification_title))
            .setContentText(context.getString(R.string.test_notification_body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        return try {
            NotificationManagerCompat.from(context).notify(TEST_NOTIFICATION_ID, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun show(sim: SimEntity, daysRemaining: Long, maskPhoneNumbers: Boolean): Boolean {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return false

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_SIM_ID, sim.id)
            putExtra(MainActivity.EXTRA_OPEN_RENEWAL, true)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            sim.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = when {
            daysRemaining < 0 -> context.getString(R.string.notification_title_overdue, sim.name)
            daysRemaining == 0L -> context.getString(R.string.notification_title_today, sim.name)
            daysRemaining == 1L -> context.getString(R.string.notification_title_tomorrow, sim.name)
            else -> context.resources.getQuantityString(
                R.plurals.notification_title_days,
                daysRemaining.toInt(),
                sim.name,
                daysRemaining,
            )
        }
        val number = if (maskPhoneNumbers) maskPhoneNumber(sim.phoneNumber) else sim.phoneNumber
        val locale = ConfigurationCompat.getLocales(context.resources.configuration)[0]
            ?: java.util.Locale.getDefault()
        val due = sim.nextRenewalDate.format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale),
        )
        val body = listOfNotNull(
            number,
            context.getString(R.string.notification_due_date, due),
        ).joinToString(" · ")
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(sim.id.hashCode(), notification)
        } catch (_: SecurityException) {
            // Permission can be revoked between the explicit check and this call.
            return false
        }
        return true
    }

    companion object {
        const val CHANNEL_ID = "renewal_reminders"
        private const val TEST_NOTIFICATION_ID = 0x4F4D4E49
    }
}

data class NotificationAvailability(
    val runtimePermissionGranted: Boolean,
    val appNotificationsEnabled: Boolean,
    val channelEnabled: Boolean,
) {
    val canPost: Boolean
        get() = runtimePermissionGranted && appNotificationsEnabled && channelEnabled
}
