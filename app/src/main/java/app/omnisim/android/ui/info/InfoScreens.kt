package app.omnisim.android.ui.info

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.omnisim.android.R
import app.omnisim.android.ui.theme.OmniCardPadding
import app.omnisim.android.ui.theme.OmniScreenPadding
import app.omnisim.android.ui.theme.OmniSectionSpacing

@Composable
fun PrivacyPermissionsScreen(
    onOpenSystemPermissions: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var notificationsAllowed by remember(context) {
        mutableStateOf(hasNotificationPermission(context))
    }

    DisposableEffect(context, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsAllowed = hasNotificationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = OmniScreenPadding,
            top = 8.dp,
            end = OmniScreenPadding,
            bottom = OmniSectionSpacing,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HighlightInfoCard(
                title = stringResource(R.string.privacy_local_first_title),
                body = stringResource(R.string.privacy_local_first_body),
            )
        }
        item { InfoSectionTitle(stringResource(R.string.privacy_permissions_section)) }
        item {
            InfoCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.privacy_notification_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.privacy_notification_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    PermissionStatus(
                        text = when {
                            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> {
                                stringResource(R.string.permission_status_not_required)
                            }
                            notificationsAllowed -> stringResource(R.string.permission_status_allowed)
                            else -> stringResource(R.string.permission_status_not_allowed)
                        },
                        allowed = notificationsAllowed,
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onOpenSystemPermissions,
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                ) {
                    Text(stringResource(R.string.open_system_permissions))
                }
            }
        }
        item {
            InfoTextCard(
                title = stringResource(R.string.privacy_network_title),
                body = stringResource(R.string.privacy_network_body),
            )
        }
        item {
            InfoTextCard(
                title = stringResource(R.string.privacy_files_title),
                body = stringResource(R.string.privacy_files_body),
            )
        }
        item {
            InfoTextCard(
                title = stringResource(R.string.privacy_unused_permissions_title),
                body = stringResource(R.string.privacy_unused_permissions_body),
            )
        }
        item {
            InfoTextCard(
                title = stringResource(R.string.privacy_deletion_title),
                body = stringResource(R.string.privacy_deletion_body),
            )
        }
    }
}

@Composable
fun UsageGuideScreen() {
    val steps = listOf(
        GuideStep(R.string.usage_step_add_title, R.string.usage_step_add_body),
        GuideStep(R.string.usage_step_check_title, R.string.usage_step_check_body),
        GuideStep(R.string.usage_step_renew_title, R.string.usage_step_renew_body),
        GuideStep(R.string.usage_step_cost_title, R.string.usage_step_cost_body),
        GuideStep(R.string.usage_step_reminder_title, R.string.usage_step_reminder_body),
        GuideStep(R.string.usage_step_backup_title, R.string.usage_step_backup_body),
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = OmniScreenPadding,
            top = 8.dp,
            end = OmniScreenPadding,
            bottom = OmniSectionSpacing,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HighlightInfoCard(
                title = stringResource(R.string.usage_guide_intro_title),
                body = stringResource(R.string.usage_guide_intro_body),
            )
        }
        itemsIndexed(
            items = steps,
            key = { _, step -> step.title },
        ) { index, step ->
            InfoCard {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Surface(
                        modifier = Modifier.size(38.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = CircleShape,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = (index + 1).toString(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(step.title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(step.body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item {
            InfoTextCard(
                title = stringResource(R.string.usage_tips_title),
                body = stringResource(R.string.usage_tips_body),
            )
        }
    }
}

private data class GuideStep(
    @param:StringRes val title: Int,
    @param:StringRes val body: Int,
)

private fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

@Composable
private fun HighlightInfoCard(
    title: String,
    body: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(OmniCardPadding),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun InfoSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp),
    )
}

@Composable
private fun InfoTextCard(
    title: String,
    body: String,
) {
    InfoCard {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(OmniCardPadding),
            content = content,
        )
    }
}

@Composable
private fun PermissionStatus(
    text: String,
    allowed: Boolean,
) {
    Surface(
        color = if (allowed) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        contentColor = if (allowed) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        shape = CircleShape,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}
