package com.citacomsys.app.shared.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.savedstate.serialization.SavedStateConfiguration
import com.citacomsys.api.organization.nav.OrganizationHubNav
import com.citacomsys.api.workspace.WorkspaceDashboardNav
import com.citacomsys.app.shared.boot.AppPlatform
import com.citacomsys.app.shared.nav.SaaSNavHost
import com.citacomsys.app.shared.screen.WorkspaceBootstrapLoader
import com.citacomsys.core.components.navigationsuite.AppNavigationSuiteScaffoldV2
import com.citacomsys.core.components.navigationsuite.NavigationItem
import com.citacomsys.core.data.session.BranchSwitcherViewModel
import com.citacomsys.core.domain.repository.PreferencesRepository
import com.citacomsys.core.domain.security.BranchUiModel
import com.citacomsys.core.domain.security.UserCapabilityManager
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.notification.system.ui.AppNotificationBanner
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.AppManifest
import com.citacomsys.core.spi.AppOperatingMode
import com.citacomsys.core.spi.LocalNavigator
import com.citacomsys.core.spi.NavigationCoordinator
import com.citacomsys.core.spi.Navigator
import com.citacomsys.core.spi.rememberNavigationState
import com.citacomsys.core.splash.SplashScreen
import com.citacomsys.core.ui.NotificationHost
import com.citacomsys.feature.api.telemetry.CommonUpdateState
import com.citacomsys.feature.api.telemetry.PlatformAppUpdater
import com.citacomsys.feature.auth.ui.screen.AuthHostScreen
import com.citacomsys.feature.platform.ui.welcome.WelcomeScreen
import com.citacomsys.feature.telemetry.presentation.screen.ForceUpdateGateScreen
import com.citacomsys.kernel.client.WorkspaceConfigurationManager
import com.citacomsys.kernel.service.PlatformClientService
import com.citacomsys.kernel.service.PlatformRuntimeStore
import com.citacomsys.shared.event.AppLifecycleEvent
import com.citacomsys.types.UserCapability
import com.design.AppTheme
import com.design.AppTypography
import com.design.ThemeManager
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppShell(
    modifier: Modifier = Modifier,
    orchestrator: AppStageOrchestrator = koinInject(),
    appManifest: AppManifest = koinInject(),
    onExit: () -> Unit = {},
    topBarWrapper: @Composable (@Composable () -> Unit) -> Unit = { it() },
    trailingContent: @Composable RowScope.() -> Unit = {}
) {
    val stage by orchestrator.currentStage.collectAsStateWithLifecycle()
    val operatingMode by appManifest.operatingMode.collectAsStateWithLifecycle()
    val widthClass = currentWindowAdaptiveInfoV2().windowSizeClass

    // Fullscreen Splash during initialization and resolving identity phases
    if (stage.isInitState) {
        InitializationScreen(
            lifecycleState = AppPlatform.lifecycleState.collectAsStateWithLifecycle().value
        )
        return
    }

    // Unified wrapper that always shows the top bar
    AppShellContainer(
        topBarWrapper = topBarWrapper,
        trailingContent = trailingContent,
        onExit = onExit,
        modifier = modifier
    ) {
        AnimatedContent(
            targetState = stage,
            transitionSpec = {
                fadeIn(animationSpec = tween(400, easing = FastOutSlowInEasing)) +
                        scaleIn(initialScale = 0.98f, animationSpec = tween(400, easing = FastOutSlowInEasing)) togetherWith
                        fadeOut(animationSpec = tween(350, easing = FastOutSlowInEasing))
            },
            modifier = Modifier.fillMaxSize(),
            label = "AppStageTransition"
        ) { target ->
            when (target) {
                AppStage.WELCOME -> {
                    WelcomeScreen(
                        isCompact = widthClass.minWidthDp <= 600,
                        onNewRegistration = {},
                        onLoginHub = {
                            orchestrator.setStage(AppStage.AUTHENTICATION)
                        }
                    )
                }

                AppStage.W_SPACE_CONFIGURATION -> {
                    val scope = rememberCoroutineScope()
                    val configurationManager: WorkspaceConfigurationManager = koinInject()
                    val sessionManager: AppSessionManager = koinInject()
                    val manifest: AppManifest = koinInject()
                    val event = orchestrator.workspaceSelectEvent
                    if (event != null) {
                        WorkspaceBootstrapLoader(
                            configurationManager = configurationManager,
                            event = event,
                            onComplete = orchestrator::workspaceConfigurationCompleted,
                            onFail = {
                                scope.launch {
                                    sessionManager.clearWorkspaceSession()
                                    manifest.setOperatingMode(AppOperatingMode.ORG)
                                    orchestrator.setStage(AppStage.MAIN_APP)
                                }
                            }
                        )
                    }
                }

                AppStage.AUTHENTICATION -> {
                    val store: PlatformRuntimeStore = koinInject()
                    AuthHostScreen(
                        onAuthSuccess = {
                            orchestrator.evaluate()
                        },
                        goBack = {
                            if (store.getOrgConfig() == null) {
                                orchestrator.setStage(AppStage.WELCOME)
                            }
                        }
                    )
                }

                AppStage.MAIN_APP -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        MainScreen(
                            sessionManager = koinInject(),
                            orchestrator = orchestrator,
                            operatingMode = operatingMode
                        )

                        // Global In-App Notification Host overlay for action feedback, success toasts, and error alerts
                        NotificationHost(
                            service = koinInject<InAppNotificationService>(),
                            modifier = Modifier.fillMaxSize()
                        )

                        // AppNotificationBanner for system background notification banners
                        AppNotificationBanner(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        )

                        // Global Notification Action Handler for Mesh & Sync events
                        NotificationActionHandler()
                    }
                }

                else -> {}
            }
        }
    }
}

@Composable
private fun InitializationScreen(
    appUpdater: PlatformAppUpdater = koinInject(),
    lifecycleState: AppLifecycleEvent
) {
    val splashMessage = when (lifecycleState) {
        AppLifecycleEvent.Bootstrapping -> "Configuring Platform..."
        AppLifecycleEvent.WorkspaceInfraConfiguration -> "Activating Industrial Context..."
        else -> "Initializing Citas Platform..."
    }

    val updateState by appUpdater.syncState.collectAsStateWithLifecycle(CommonUpdateState.Idle)

    val isMandatory =
        (updateState as? CommonUpdateState.UpdateAvailable)?.release?.isMandatoryForceUpdate == true

    if (isMandatory) {
        ForceUpdateGateScreen(
            onBypassAllowed = { },
            onNoUpdate = { }
        )
    } else {
        SplashScreen(splashMessage)
    }
}

@Composable
private fun MainScreen(
    sessionManager: AppSessionManager,
    orchestrator: AppStageOrchestrator,
    operatingMode: AppOperatingMode,
    coordinator: NavigationCoordinator = koinInject(),
    platformService: PlatformClientService = koinInject(),
    prefs: PreferencesRepository = koinInject(),
) {
    val navSuiteState = rememberNavigationSuiteScaffoldState()
    val rawNavItems by coordinator.rawNavigationItems.collectAsStateWithLifecycle()
    val workStationInfo by sessionManager.workStationInfo.collectAsStateWithLifecycle()

    var cachedNavItems by remember { mutableStateOf(rawNavItems) }
    remember(rawNavItems) {
        if (rawNavItems.isNotEmpty()) {
            cachedNavItems = rawNavItems
        }
    }
    val navItemsContrib = if (rawNavItems.isNotEmpty()) rawNavItems else cachedNavItems
    val activeNavState by orchestrator.activeNavState.collectAsStateWithLifecycle(null)

    // Navigation 3 State Management
    val unifiedSerializersModule by coordinator.unifiedSerializersModuleFlow.collectAsStateWithLifecycle()
    val savedStateConfig = remember(unifiedSerializersModule) {
        SavedStateConfiguration {
            serializersModule = unifiedSerializersModule
        }
    }

    val topLevelRoutes = remember(navItemsContrib, operatingMode) {
        navItemsContrib.map { it.route }.toSet()
    }

    val expectedStartRoute = if (workStationInfo.isOrgMode) OrganizationHubNav else WorkspaceDashboardNav

    val navigationState = key(
        operatingMode,
        topLevelRoutes
    ) {
        val validState = activeNavState?.takeIf {
            it.startRoute == expectedStartRoute && it.backStacks.keys.containsAll(topLevelRoutes)
        }
        validState ?: rememberNavigationState(
            startRoute = expectedStartRoute,
            topLevelRoutes = topLevelRoutes,
            savedStateConfiguration = savedStateConfig
        )
    }

    LaunchedEffect(navigationState, operatingMode) {
        orchestrator.recordNavState(navigationState, operatingMode)
    }

    val navigator = remember(navigationState) {
        Navigator(navigationState, coordinator)
    }

    val navItems = remember(navItemsContrib) {
        navItemsContrib.map { item ->
            NavigationItem(
                item.route,
                item.label,
                item.unselectedIcon,
                item.selectedIcon,
                item.isFooter
            )
        }
    }

    val widthClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val layoutType = if (widthClass.minWidthDp <= 600) {
        NavigationSuiteType.NavigationBar
    } else if (widthClass.minWidthDp <= 840) {
        NavigationSuiteType.NavigationRail
    } else {
        NavigationSuiteType.NavigationDrawer
    }

    val currentActiveModule = navigationState.topLevelRoute

    AnimatedContent(navItemsContrib.isEmpty()) { st ->
        when (st) {
            true -> SplashScreen("Launching dashboard...")
            false -> CompositionLocalProvider(LocalNavigator provides navigator) {
                AppNavigationSuiteScaffoldV2(
                    navigationSuiteState = navSuiteState,
                    layoutType = layoutType,
                    navItems = navItems,
                    isSelected = { navItem -> navItem.id == currentActiveModule },
                    onNavClick = { navItem ->
                        navigator.navigateToSingleTop(navItem.id)
                    },
                    sidebarHeaderContent = { isExpanded, onToggleExpand ->
                        SidebarHeaderSection(
                            isExpanded = isExpanded,
                            onToggleExpand = onToggleExpand
                        )
                    },
                    footerContent = { isExpanded ->
                        SidebarFooterSection(
                            isExpanded = isExpanded,
                            orchestrator = orchestrator,
                            sessionManager = sessionManager
                        )
                    },
                    content = {
                        SaaSNavHost(
                            navigator = navigator,
                            modifier = Modifier.fillMaxSize(),
                            isCompact = widthClass.minWidthDp <= 600,
                            coordinator = coordinator
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun AppShellContainer(
    topBarWrapper: @Composable (@Composable () -> Unit) -> Unit,
    trailingContent: @Composable RowScope.() -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        topBarWrapper {
            AppStatusBar(
                systemControls = trailingContent
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

private fun formatBranchLabel(id: String, name: String): String {
    if (id == "CEO_MODE") return "ALL"
    val words = name.trim().split(" ").filter { it.isNotBlank() }
    return when {
        words.isEmpty() -> "BR"
        words.size == 1 -> if (words[0].length <= 4) words[0].uppercase() else words[0].take(3)
            .uppercase()

        else -> words.take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
    }
}

@Composable
private fun SidebarHeaderSection(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit = {},
    branchViewModel: BranchSwitcherViewModel = koinViewModel(),
    sessionManager: AppSessionManager = koinInject(),
) {
    val viewBranches by branchViewModel.viewBranches.collectAsStateWithLifecycle()
    val isBranchLocked by sessionManager.isBranchLocked.collectAsStateWithLifecycle()

    val workStationInfo by sessionManager.workStationInfo.collectAsStateWithLifecycle()

    SidebarHeaderContent(
        viewBranches = viewBranches,
        orgName = workStationInfo.orgName,
        activeBranchName = workStationInfo.activeWorkspaceName,
        logo = workStationInfo.logo,
        isOrgMode = workStationInfo.isOrgMode,
        isBranchLocked = isBranchLocked,
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand,
        onSelectViewBranch = { branchId -> branchViewModel.selectViewBranch(branchId) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SidebarHeaderContent(
    viewBranches: List<BranchUiModel>,
    activeBranchName: String,
    orgName: String,
    logo: String,
    isOrgMode: Boolean,
    isBranchLocked: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit = {},
    onSelectViewBranch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (!isExpanded) {
            // Collapsed Rail Header: Workspace/Org Avatar Badge acts as expand toggle
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    onClick = onToggleExpand,
                    shape = CircleShape,
                    color = if (!isOrgMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = logo,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (!isOrgMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                if (!isOrgMode && viewBranches.isNotEmpty()) {
                    val activeBranch =
                        viewBranches.find { it.isSelected } ?: viewBranches.firstOrNull()
                    val label =
                        formatBranchLabel(activeBranch?.id ?: "", activeBranch?.name ?: "ALL")

                    Surface(
                        shape = CircleShape,
                        color = if (activeBranch?.id == "CEO_MODE") MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label.take(2),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (activeBranch?.id == "CEO_MODE") MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        } else {
            // Expanded Sidebar Header: Prominent Current Workspace / Store / Org Display & Branch Switcher
            var expanded by remember { mutableStateOf(false) }
            val canSwitch = !isOrgMode && !isBranchLocked && viewBranches.size > 1

            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    onClick = { if (canSwitch) expanded = true },
                    enabled = canSwitch,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (!isOrgMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = logo,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isOrgMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            val storeName = if (!isOrgMode) activeBranchName
                            else orgName.ifBlank { "Company" }

                            Text(
                                text = storeName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (!isOrgMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(6.dp)
                                ) {}
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (!isOrgMode) (if (isBranchLocked) "Store Locked" else "Store Mode") else "Company Mode",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (isBranchLocked) {
                            Icon(
                                imageVector = AppIcons.Lock,
                                contentDescription = "Branch Locked to Device",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp)
                            )
                        } else if (canSwitch) {
                            Icon(
                                imageVector = AppIcons.ExpandMore,
                                contentDescription = "Switch Branch",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                if (canSwitch) {
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.width(260.dp)
                    ) {
                        viewBranches.forEach { branch ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = branch.name,
                                        fontWeight = if (branch.isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    onSelectViewBranch(branch.id)
                                    expanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (branch.id == "CEO_MODE") AppIcons.Business else AppIcons.Place,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SidebarFooterSection(
    isExpanded: Boolean,
    orchestrator: AppStageOrchestrator,
    sessionManager: AppSessionManager,
    capabilityManager: UserCapabilityManager = koinInject(),
) {
    val userCaps by capabilityManager.capabilities.collectAsStateWithLifecycle()
    val workStationInfo by sessionManager.workStationInfo.collectAsStateWithLifecycle()
    val canViewOrg = userCaps.contains(UserCapability.VIEW_ORGANIZATIONS) ||
            userCaps.contains(UserCapability.MANAGE_ORGANIZATIONS)
    val hasActiveWorkspace =
        sessionManager.activeWorkspaceIdFlow.collectAsStateWithLifecycle().value != null

    val isOrgSelected = workStationInfo.isOrgMode

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (isExpanded) {

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            ) {
                SegmentedButton(
                    selected = !isOrgSelected,
                    onClick = { if (isOrgSelected && hasActiveWorkspace) orchestrator.switchAppMode() },
                    enabled = hasActiveWorkspace || !isOrgSelected,
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = {
                        Icon(
                            imageVector = AppIcons.Storefront,
                            contentDescription = "Store Mode",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                ) {
                    Text(
                        text = "Store",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                SegmentedButton(
                    selected = isOrgSelected,
                    onClick = { if (!isOrgSelected && canViewOrg) orchestrator.switchAppMode() },
                    enabled = canViewOrg || isOrgSelected,
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = {
                        Icon(
                            imageVector = AppIcons.Business,
                            contentDescription = "Company Mode",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                ) {
                    Text(
                        text = "Company",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            // Collapsed Single Icon Switcher Button
            if (!isOrgSelected && canViewOrg) {
                Surface(
                    onClick = { orchestrator.switchAppMode() },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.Business,
                            contentDescription = "Switch to Company Mode",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else if (isOrgSelected && hasActiveWorkspace) {
                Surface(
                    onClick = { orchestrator.switchAppMode() },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.Storefront,
                            contentDescription = "Switch to Store Mode",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
