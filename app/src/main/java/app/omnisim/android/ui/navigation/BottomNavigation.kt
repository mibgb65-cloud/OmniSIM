package app.omnisim.android.ui.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.annotation.StringRes
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.omnisim.android.BuildConfig
import app.omnisim.android.backup.isSafeWebUrl
import app.omnisim.android.R
import app.omnisim.android.data.preferences.CURRENT_LEGAL_CONSENT_VERSION
import app.omnisim.android.data.update.isTrustedUpdateDownloadUrl
import app.omnisim.android.ui.AppViewModel
import app.omnisim.android.ui.components.OmniCircleIconButton
import app.omnisim.android.ui.components.OmniPageSurface
import app.omnisim.android.ui.components.OmniPageTitleStyle
import app.omnisim.android.ui.components.OmniSheetHeader
import app.omnisim.android.ui.components.OmniDialogSystemBars
import app.omnisim.android.ui.components.omniPrimaryPageBackground
import app.omnisim.android.ui.editsim.AddEditSimScreen
import app.omnisim.android.ui.home.HomeScreen
import app.omnisim.android.ui.history.RenewalHistoryScreen
import app.omnisim.android.ui.info.LegalConsentDialog
import app.omnisim.android.ui.info.LegalDocumentsScreen
import app.omnisim.android.ui.info.PrivacyPermissionsScreen
import app.omnisim.android.ui.info.UsageGuideScreen
import app.omnisim.android.ui.settings.AppLanguageController
import app.omnisim.android.ui.settings.AppUpdateDialog
import app.omnisim.android.ui.settings.SettingsScreen
import app.omnisim.android.ui.settings.SettingsSection
import app.omnisim.android.ui.simdetail.SimDetailScreen
import app.omnisim.android.ui.sims.SimListScreen
import app.omnisim.android.ui.splash.LAUNCH_REVEAL_DURATION_MILLIS
import app.omnisim.android.ui.splash.OmniLaunchScreen
import app.omnisim.android.ui.splash.rememberSystemAnimationsEnabled
import app.omnisim.android.ui.theme.OmniSimTheme
import app.omnisim.android.ui.theme.OmniScreenPadding
import app.omnisim.android.ui.usage.UsageScreen
import kotlinx.coroutines.delay

internal object Routes {
    const val Home = "home"
    const val Sims = "sims"
    const val Usage = "usage"
    const val Settings = "settings"
    const val SettingsCategory = "settings/category/{category}"
    const val History = "history"
    const val PrivacyPermissions = "settings/privacy-permissions"
    const val LegalDocuments = "settings/legal-documents"
    const val UsageGuide = "settings/usage-guide"
    const val Detail = "sim/{simId}"
    const val Edit = "sim/{simId}/edit"

    fun detail(id: String) = "sim/$id"
    fun edit(id: String) = "sim/$id/edit"
    fun settingsCategory(section: SettingsSection) = "settings/category/${section.name}"
}

internal data class BottomDestination(
    val route: String,
    @param:StringRes val label: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

internal val bottomDestinations = listOf(
    BottomDestination(Routes.Home, R.string.nav_home, Icons.Outlined.Home),
    BottomDestination(Routes.Sims, R.string.nav_sims, Icons.AutoMirrored.Outlined.List),
    BottomDestination(Routes.Usage, R.string.nav_usage, usageIcon()),
    BottomDestination(Routes.Settings, R.string.nav_settings, Icons.Outlined.Settings),
)

private fun usageIcon(): ImageVector =
    ImageVector.Builder(
        name = "Usage",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2.2f,
            strokeLineCap = StrokeCap.Round,
        ) {
            moveTo(5f, 19f)
            verticalLineTo(13f)
            moveTo(12f, 19f)
            verticalLineTo(5f)
            moveTo(19f, 19f)
            verticalLineTo(9f)
        }
    }.build()

internal fun openNotificationSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
        }
    } else {
        Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    }
    context.startActivity(intent)
}

@Composable
internal fun FloatingBottomNavigation(
    currentRoute: String,
    onSelect: (BottomDestination) -> Unit,
) {
    val navigationHeight = 76.dp
    val indicatorHeight = 64.dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = OmniScreenPadding, vertical = 10.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
            shape = CircleShape,
            shadowElevation = 4.dp,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
            ),
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(navigationHeight),
            ) {
                val selectedIndex = bottomDestinations.indexOfFirst { it.route == currentRoute }
                    .coerceAtLeast(0)
                val itemWidth = maxWidth / bottomDestinations.size
                val indicatorWidth = itemWidth - 8.dp
                val indicatorOffset by animateDpAsState(
                    targetValue = itemWidth * selectedIndex + (itemWidth - indicatorWidth) / 2,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    label = "bottom-navigation-indicator",
                )
                Surface(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = indicatorOffset.roundToPx(),
                                y = ((navigationHeight - indicatorHeight) / 2).roundToPx(),
                            )
                        }
                        .width(indicatorWidth)
                        .height(indicatorHeight),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.90f),
                    shape = RoundedCornerShape(indicatorHeight / 2),
                ) {}
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(navigationHeight),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    bottomDestinations.forEach { destination ->
                        val selected = currentRoute == destination.route
                        val label = stringResource(destination.label)
                        val itemColor by animateColorAsState(
                            targetValue = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            label = "bottom-navigation-content",
                        )
                        val interactionSource = remember(destination.route) {
                            MutableInteractionSource()
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(navigationHeight)
                                .selectable(
                                    selected = selected,
                                    interactionSource = interactionSource,
                                    indication = null,
                                    role = Role.Tab,
                                    onClick = {
                                        if (!selected) onSelect(destination)
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Box(
                                    modifier = Modifier.size(36.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        destination.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                        tint = itemColor,
                                    )
                                }
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = itemColor,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
