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
import app.omnisim.android.data.update.launchUpdateInstaller
import app.omnisim.android.ui.AppViewModel
import app.omnisim.android.ui.appUpdateStateForRoute
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OmniSimApp(
    viewModel: AppViewModel,
    externalSimId: String?,
    onExternalNavigationHandled: () -> Unit,
    playLaunchAnimation: Boolean,
    launchAnimationStarted: Boolean,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val appUpdateState by viewModel.appUpdateState.collectAsStateWithLifecycle()
    val appUpdateDownloadState by viewModel.appUpdateDownloadState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route ?: Routes.Home
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val resources = LocalResources.current
    val uriHandler = LocalUriHandler.current
    val isPrimary = route in bottomDestinations.map(BottomDestination::route)
    var showAddSim by rememberSaveable { mutableStateOf(false) }
    val launchAnimationsEnabled = rememberSystemAnimationsEnabled()
    var launchRevealFinished by remember(playLaunchAnimation) {
        mutableStateOf(!playLaunchAnimation)
    }

    LaunchedEffect(playLaunchAnimation, launchAnimationStarted, launchAnimationsEnabled) {
        if (playLaunchAnimation && launchAnimationStarted) {
            if (launchAnimationsEnabled) {
                delay(LAUNCH_REVEAL_DURATION_MILLIS)
            }
            launchRevealFinished = true
        }
    }
    val showLaunch = playLaunchAnimation && (!launchRevealFinished || state.isLoading)
    val hasLegalConsent = state.legalConsentVersion >= CURRENT_LEGAL_CONSENT_VERSION
    val canCheckForUpdates = !state.isLoading && !showLaunch && hasLegalConsent

    LaunchedEffect(canCheckForUpdates, viewModel) {
        if (canCheckForUpdates) {
            viewModel.checkForUpdatesOnLaunch(BuildConfig.VERSION_NAME)
            viewModel.loadExchangeRates()
        }
    }
    LaunchedEffect(viewModel, resources) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(resources.getString(message.text))
        }
    }
    LaunchedEffect(externalSimId, hasLegalConsent) {
        if (hasLegalConsent) externalSimId?.let {
            navController.navigate(Routes.detail(it)) { launchSingleTop = true }
            onExternalNavigationHandled()
        }
    }

    OmniSimTheme(state.settings) {
        val bottomNavigationContentPadding =
            96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            Scaffold(
                bottomBar = {
                    if (isPrimary) {
                        FloatingBottomNavigation(
                            currentRoute = route,
                            onSelect = { destination ->
                                val currentEntry = navController.currentBackStackEntry
                                if (
                                    destination.route != route &&
                                    currentEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED
                                ) {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                        )
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                containerColor = Color.Transparent,
            ) { padding ->
                val layoutDirection = LocalLayoutDirection.current
                val navHostModifier = Modifier.absolutePadding(
                    left = padding.calculateLeftPadding(layoutDirection),
                    right = padding.calculateRightPadding(layoutDirection),
                )
                val primaryPageModifier = Modifier
                val secondaryPageModifier = Modifier
                    .statusBarsPadding()
                    .navigationBarsPadding()
                NavHost(
                    navController = navController,
                    startDestination = Routes.Home,
                    modifier = navHostModifier,
                ) {
                composable(Routes.Home) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .omniPrimaryPageBackground(),
                    ) {
                        HomeScreen(
                            sims = state.sims,
                            history = state.history,
                            settings = state.settings,
                            onAdd = { showAddSim = true },
                            onOpenSim = { navController.navigate(Routes.detail(it)) },
                            onRenew = { sim, actual, next, amount, notes ->
                                viewModel.recordRenewal(sim.id, actual, next, amount, notes)
                            },
                            onUpdateRenewal = viewModel::updateRenewal,
                            onUndoRenewal = viewModel::undoRenewal,
                            onReminderSettings = viewModel::setSimReminderSettings,
                            onOpenWebsite = { url -> if (isSafeWebUrl(url)) uriHandler.openUri(url) },
                            onEditSim = { navController.navigate(Routes.edit(it)) },
                            onArchive = viewModel::setArchived,
                            onDelete = viewModel::delete,
                            bottomContentPadding = bottomNavigationContentPadding,
                        )
                    }
                }
                composable(Routes.Sims) {
                    OmniPageSurface(
                        title = stringResource(R.string.nav_sims),
                        modifier = primaryPageModifier,
                        titleStyle = OmniPageTitleStyle.Bubble,
                        action = {
                            OmniCircleIconButton(
                                onClick = { showAddSim = true },
                                emphasized = true,
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = stringResource(R.string.action_add_sim),
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        },
                    ) {
                        SimListScreen(
                            sims = state.sims,
                            settings = state.settings,
                            onOpenSim = { navController.navigate(Routes.detail(it)) },
                            bottomContentPadding = bottomNavigationContentPadding,
                        )
                    }
                }
                composable(Routes.Usage) {
                    OmniPageSurface(
                        title = stringResource(R.string.nav_usage),
                        modifier = primaryPageModifier,
                        titleStyle = OmniPageTitleStyle.Bubble,
                    ) {
                        UsageScreen(
                            sims = state.sims,
                            history = state.history,
                            defaultCurrency = state.settings.defaultCurrency,
                            exchangeRateState = state.exchangeRates,
                            onRefreshRates = viewModel::refreshExchangeRates,
                            onOpenSim = { navController.navigate(Routes.detail(it)) },
                            onOpenHistory = { navController.navigate(Routes.History) },
                            bottomContentPadding = bottomNavigationContentPadding,
                        )
                    }
                }
                composable(Routes.History) {
                    OmniPageSurface(
                        title = stringResource(R.string.all_renewal_history),
                        modifier = secondaryPageModifier,
                        navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                        onNavigate = { navController.popBackStack() },
                    ) {
                        RenewalHistoryScreen(
                            history = state.history,
                            sims = state.sims,
                            onOpenSim = { navController.navigate(Routes.detail(it)) },
                        )
                    }
                }
                composable(Routes.Settings) {
                    OmniPageSurface(
                        title = stringResource(R.string.nav_settings),
                        modifier = primaryPageModifier,
                        titleStyle = OmniPageTitleStyle.Bubble,
                    ) {
                        SettingsScreen(
                            section = SettingsSection.Overview,
                            settings = state.settings,
                            exchangeRateSnapshot = state.exchangeRateSnapshot,
                            appLanguage = AppLanguageController.current(),
                            pendingRestore = state.pendingRestore,
                            recoverySnapshotAvailable = state.recoverySnapshotAvailable,
                            onThemeMode = viewModel::setThemeMode,
                            onAppLanguage = AppLanguageController::set,
                            onDynamicColor = viewModel::setDynamicColor,
                            onWarningPeriod = viewModel::setWarningPeriod,
                            onMaskPhoneNumbers = viewModel::setMaskPhoneNumbers,
                            onReminderOffsets = viewModel::setReminderOffsets,
                            onDefaultCurrency = viewModel::setDefaultCurrency,
                            onExport = viewModel::exportBackup,
                            onExportHistoryCsv = viewModel::exportHistoryCsv,
                            onImport = viewModel::prepareRestore,
                            onPrepareRecoveryRestore = viewModel::prepareRecoveryRestore,
                            onConfirmRestore = viewModel::confirmRestore,
                            onCancelRestore = viewModel::cancelRestore,
                            onNotificationsEnabled = viewModel::checkRemindersNow,
                            onSendTestNotification = viewModel::sendTestNotification,
                            onOpenNotificationSettings = {
                                openNotificationSettings(context)
                            },
                            onOpenLegalDocuments = {
                                navController.navigate(Routes.LegalDocuments)
                            },
                            onOpenPrivacyPermissions = {
                                navController.navigate(Routes.PrivacyPermissions)
                            },
                            onOpenUsageGuide = {
                                navController.navigate(Routes.UsageGuide)
                            },
                            onCheckForUpdates = {
                                viewModel.checkForUpdates(BuildConfig.VERSION_NAME)
                            },
                            onOpenSection = {
                                navController.navigate(Routes.settingsCategory(it))
                            },
                            bottomContentPadding = bottomNavigationContentPadding,
                        )
                    }
                }
                composable(Routes.SettingsCategory) { entry ->
                    val section = entry.arguments?.getString("category")
                        ?.let { value -> SettingsSection.entries.firstOrNull { it.name == value } }
                        ?: SettingsSection.Appearance
                    val title = stringResource(
                        when (section) {
                            SettingsSection.Overview -> R.string.nav_settings
                            SettingsSection.Appearance -> R.string.settings_appearance_and_language
                            SettingsSection.Renewal -> R.string.settings_renewal_and_notifications
                            SettingsSection.DataPrivacy -> R.string.settings_data_and_privacy
                            SettingsSection.HelpAbout -> R.string.settings_help_about
                        },
                    )
                    OmniPageSurface(
                        title = title,
                        modifier = secondaryPageModifier,
                        navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                        onNavigate = { navController.popBackStack() },
                    ) {
                        SettingsScreen(
                            section = section,
                            settings = state.settings,
                            exchangeRateSnapshot = state.exchangeRateSnapshot,
                            appLanguage = AppLanguageController.current(),
                            pendingRestore = state.pendingRestore,
                            recoverySnapshotAvailable = state.recoverySnapshotAvailable,
                            onThemeMode = viewModel::setThemeMode,
                            onAppLanguage = AppLanguageController::set,
                            onDynamicColor = viewModel::setDynamicColor,
                            onWarningPeriod = viewModel::setWarningPeriod,
                            onMaskPhoneNumbers = viewModel::setMaskPhoneNumbers,
                            onReminderOffsets = viewModel::setReminderOffsets,
                            onDefaultCurrency = viewModel::setDefaultCurrency,
                            onExport = viewModel::exportBackup,
                            onExportHistoryCsv = viewModel::exportHistoryCsv,
                            onImport = viewModel::prepareRestore,
                            onPrepareRecoveryRestore = viewModel::prepareRecoveryRestore,
                            onConfirmRestore = viewModel::confirmRestore,
                            onCancelRestore = viewModel::cancelRestore,
                            onNotificationsEnabled = viewModel::checkRemindersNow,
                            onSendTestNotification = viewModel::sendTestNotification,
                            onOpenNotificationSettings = {
                                openNotificationSettings(context)
                            },
                            onOpenLegalDocuments = {
                                navController.navigate(Routes.LegalDocuments)
                            },
                            onOpenPrivacyPermissions = {
                                navController.navigate(Routes.PrivacyPermissions)
                            },
                            onOpenUsageGuide = {
                                navController.navigate(Routes.UsageGuide)
                            },
                            onCheckForUpdates = {
                                viewModel.checkForUpdates(BuildConfig.VERSION_NAME)
                            },
                            onOpenSection = {},
                            bottomContentPadding = 0.dp,
                        )
                    }
                }
                composable(Routes.LegalDocuments) {
                    OmniPageSurface(
                        title = stringResource(R.string.legal_documents),
                        modifier = secondaryPageModifier,
                        navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                        onNavigate = { navController.popBackStack() },
                    ) {
                        LegalDocumentsScreen()
                    }
                }
                composable(Routes.PrivacyPermissions) {
                    OmniPageSurface(
                        title = stringResource(R.string.privacy_permissions),
                        modifier = secondaryPageModifier,
                        navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                        onNavigate = { navController.popBackStack() },
                    ) {
                        PrivacyPermissionsScreen(
                            onOpenSystemPermissions = {
                                context.startActivity(
                                    Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    },
                                )
                            },
                        )
                    }
                }
                composable(Routes.UsageGuide) {
                    OmniPageSurface(
                        title = stringResource(R.string.usage_guide),
                        modifier = secondaryPageModifier,
                        navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                        onNavigate = { navController.popBackStack() },
                    ) {
                        UsageGuideScreen()
                    }
                }
                composable(Routes.Detail) { entry ->
                    val id = entry.arguments?.getString("simId")
                    val sim = state.sims.find { it.id == id }
                    OmniPageSurface(
                        title = stringResource(R.string.title_sim_details),
                        modifier = secondaryPageModifier,
                        navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                        onNavigate = { navController.popBackStack() },
                    ) {
                        if (sim == null) {
                            Text(
                                stringResource(R.string.sim_not_found),
                                modifier = Modifier.padding(24.dp),
                            )
                        } else {
                            SimDetailScreen(
                                sim = sim,
                                history = state.history.filter { it.simId == sim.id },
                                settings = state.settings,
                                onRenew = { actual, next, amount, notes ->
                                    viewModel.recordRenewal(sim.id, actual, next, amount, notes)
                                },
                                onUpdateRenewal = viewModel::updateRenewal,
                                onUndoRenewal = viewModel::undoRenewal,
                                onReminderSettings = { enabled, offsets ->
                                    viewModel.setSimReminderSettings(sim.id, enabled, offsets)
                                },
                                onOpenWebsite = { url -> if (isSafeWebUrl(url)) uriHandler.openUri(url) },
                                onEdit = { navController.navigate(Routes.edit(sim.id)) },
                                onArchive = { viewModel.setArchived(sim.id, it) },
                                onDelete = {
                                    viewModel.delete(sim.id)
                                    navController.navigate(Routes.Sims) {
                                        popUpTo(Routes.Home)
                                        launchSingleTop = true
                                    }
                                },
                            )
                        }
                    }
                }
                composable(Routes.Edit) { entry ->
                    val id = entry.arguments?.getString("simId")
                    val sim = state.sims.find { it.id == id }
                    OmniPageSurface(
                        title = stringResource(R.string.title_edit_sim),
                        modifier = secondaryPageModifier,
                        navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                        onNavigate = { navController.popBackStack() },
                    ) {
                        if (sim == null) {
                            Text(
                                stringResource(R.string.sim_not_found),
                                modifier = Modifier.padding(24.dp),
                            )
                        } else {
                            AddEditSimScreen(
                                existing = sim,
                                defaultCurrency = state.settings.defaultCurrency,
                                exchangeRateSnapshot = state.exchangeRateSnapshot,
                                onSave = viewModel::saveSim,
                                onDone = { navController.popBackStack() },
                            )
                        }
                    }
                }
                }

                if (showAddSim) {
                    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    ModalBottomSheet(
                        onDismissRequest = { showAddSim = false },
                        sheetState = sheetState,
                        dragHandle = null,
                        containerColor = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
                    ) {
                        OmniDialogSystemBars()
                        Column(Modifier.fillMaxSize()) {
                            OmniSheetHeader(
                                title = stringResource(R.string.title_add_sim),
                                onClose = { showAddSim = false },
                            )
                            AddEditSimScreen(
                                existing = null,
                                defaultCurrency = state.settings.defaultCurrency,
                                exchangeRateSnapshot = state.exchangeRateSnapshot,
                                onSave = viewModel::saveSim,
                                onDone = {
                                    showAddSim = false
                                    navController.navigate(Routes.Home) {
                                        popUpTo(Routes.Home)
                                        launchSingleTop = true
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showLaunch,
                modifier = Modifier.fillMaxSize(),
                enter = EnterTransition.None,
                exit = if (launchAnimationsEnabled) {
                    fadeOut(tween(240)) + scaleOut(
                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                        targetScale = 0.985f,
                    )
                } else {
                    ExitTransition.None
                },
            ) {
                OmniLaunchScreen(
                    animationsEnabled = launchAnimationsEnabled,
                    startAnimation = launchAnimationStarted,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        if (hasLegalConsent && state.pendingRestore == null) {
            AppUpdateDialog(
                state = appUpdateStateForRoute(appUpdateState, route == Routes.Home),
                downloadState = appUpdateDownloadState,
                onRetry = { viewModel.checkForUpdates(BuildConfig.VERSION_NAME) },
                onDownload = viewModel::downloadUpdate, onCancelDownload = viewModel::cancelUpdateDownload,
                onInstall = { apkFile -> if (launchUpdateInstaller(context, apkFile)) viewModel.dismissUpdateDialog() },
                onDismiss = viewModel::dismissUpdateDialog,
            )
        }
        if (!state.isLoading && !showLaunch && !hasLegalConsent) {
            LegalConsentDialog(
                onAgree = viewModel::acceptLegalConsent,
                onDecline = { (context as? Activity)?.finishAndRemoveTask() },
            )
        }
    }
}
