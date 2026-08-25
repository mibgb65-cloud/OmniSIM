package app.omnisim.android.ui.navigation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import app.omnisim.android.R

internal fun shouldOfferNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED

@Composable
internal fun rememberNotificationPermissionRequest(onResult: (Boolean) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
        onResult,
    )
    return { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
}

@Composable
internal fun NotificationPermissionPrompt(
    visible: Boolean,
    onDismiss: () -> Unit,
    onEnable: () -> Unit,
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.notification_permission_title)) },
        text = { Text(stringResource(R.string.notification_permission_description)) },
        confirmButton = {
            TextButton(onClick = onEnable) {
                Text(stringResource(R.string.enable_notifications))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_not_now))
            }
        },
    )
}
