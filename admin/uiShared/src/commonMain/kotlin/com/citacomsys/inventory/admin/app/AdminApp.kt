package com.citacomsys.inventory.admin.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.window.core.layout.WindowSizeClass
import com.citacomsys.core.components.navigationsuite.AppNavigationSuiteScaffoldLayout
import com.citacomsys.core.components.navigationsuite.NavigationItem
import com.citacomsys.core.domain.repository.PreferencesRepository
import com.citacomsys.core.notification.PlatformNotificationHelper
import com.citacomsys.core.spi.LocalNavigator
import com.citacomsys.core.spi.NavigationCoordinator
import com.citacomsys.core.spi.Navigator
import com.citacomsys.core.spi.rememberNavigationState
import com.citacomsys.core.splash.SplashScreen
import com.design.Dimensions
import com.citacomsys.core.ui.NotificationHost
import com.citacomsys.inventory.admin.app.nav.AdminDashboardNavKey
import com.citacomsys.inventory.admin.app.nav.AdminDefaultNavHost
import com.citacomsys.inventory.admin.auth.AdminAuthViewModel
import com.citacomsys.inventory.admin.auth.AdminLoginScreen
import com.design.components.AdminAppStatusBar
import kotlinx.serialization.ExperimentalSerializationApi
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(
    ExperimentalSerializationApi::class,
    ExperimentalMaterial3AdaptiveApi::class
)
@Composable
fun AdminApp(
    onExit: () -> Unit = {},
    topBarWrapper: @Composable (@Composable () -> Unit) -> Unit = { it() },
    trailingContent: @Composable RowScope.() -> Unit = {},
    viewModel: AdminDefaultAppViewModel = koinViewModel(),
    authViewModel: AdminAuthViewModel = koinViewModel(),
    orchestrator: AdminAppStageOrchestrator = koinInject(),
//    eventService: AdminSystemEventService = koinInject(),
) {
    val navSuiteState = rememberNavigationSuiteScaffoldState()
    val coordinator: NavigationCoordinator = koinInject()
    val rawNavItems by coordinator.rawNavigationItems.collectAsStateWithLifecycle()
    var cachedNavItems by remember { mutableStateOf(rawNavItems) }
    remember(rawNavItems) {
        if (rawNavItems.isNotEmpty()) {
            cachedNavItems = rawNavItems
        }
    }
    val navItemsContrib = if (rawNavItems.isNotEmpty()) rawNavItems else cachedNavItems
    val currentStage by orchestrator.currentStage.collectAsStateWithLifecycle()
    val updateCheckState by viewModel.updateCheckState.collectAsStateWithLifecycle()

    // Navigation 3 State Management
    val savedStateConfig = remember(coordinator.unifiedSerializersModule) {
        SavedStateConfiguration {
            serializersModule = coordinator.unifiedSerializersModule
        }
    }

    val topLevelRoutes = remember(navItemsContrib) {
        navItemsContrib.map { it.route }.toSet()
    }

    val navigationState =
        rememberNavigationState(
            startRoute = AdminDashboardNavKey,
            topLevelRoutes = topLevelRoutes,
            savedStateConfiguration = savedStateConfig
        )

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

    val currentRoute = navigator.currentRoute

    val widthClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val layoutType = if (widthClass.minWidthDp <= 600) {
        NavigationSuiteType.NavigationBar
    } else if (widthClass.minWidthDp <= 840) {
        NavigationSuiteType.NavigationRail
    } else {
        NavigationSuiteType.NavigationDrawer
    }

    val currentActiveModule = navigationState.topLevelRoute

    // Unified wrapper that always shows the top bar
    ShellContainer(
        topBarWrapper = topBarWrapper,
        trailingContent = trailingContent,
        prefs = koinInject(),
        onExit = onExit,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(Modifier.fillMaxSize()) {
            when (currentStage) {
                AdminAppStage.INITIALIZING -> {
                    // Splash / Loading
                    SplashScreen()
                }

                AdminAppStage.AUTHENTICATION -> {
                    Column(Modifier.fillMaxSize()) {
                        AdminLoginScreen(authViewModel)
                    }
                }

                AdminAppStage.MAIN_APP -> {
                    if (navItemsContrib.isEmpty()) {
                        SplashScreen("Loading Modules...")
                        return@ShellContainer
                    }

                    CompositionLocalProvider(LocalNavigator provides navigator) {
                        AppNavigationSuiteScaffoldLayout(
                            navigationSuiteState = navSuiteState,
                            layoutType = layoutType,
                            navItems = navItems,
                            selectedNavItemId = currentRoute,
                            isSelected = { navItem -> navItem.id == currentActiveModule },
                            onNavClick = { navItem ->
                                navigator.navigateToSingleTop(navItem.id)
                            },
                            systemTopBar = {
                                // Top bar handled by ShellContainer
                            },
                            content = {
                                AdminDefaultNavHost(
                                    navigator = navigator,
                                    modifier = Modifier.fillMaxSize(),
                                    isCompact = widthClass.minWidthDp <= 600,
                                    coordinator = coordinator,
                                    isRouteAuthorized = { _ ->
                                       true
                                    }
                                )
                            }
                        )
                    }
                }
            }

            // Global In-App Notification Host overlay for action feedback, success toasts, and error alerts
            NotificationHost(
                service = koinInject(),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

fun WindowSizeClass.isCompact(): Boolean {
    return this.minWidthDp <= 600
}

@Composable
private fun ShellContainer(
    modifier: Modifier = Modifier,
    topBarWrapper: @Composable (@Composable () -> Unit) -> Unit,
    trailingContent: @Composable RowScope.() -> Unit,
    prefs: PreferencesRepository,
    onExit: () -> Unit,
    content: @Composable () -> Unit
) {
    val isDarkTheme by prefs.isDarkTheme.collectAsStateWithLifecycle()
    val fontScale by prefs.userFontScale.collectAsStateWithLifecycle()
    val hubIconSize = Dimensions.iconSize(fontScale)
    val barVerticalPadding = if (fontScale > 1.2f) 4.dp else 8.dp

    val notificationCount by PlatformNotificationHelper.notificationCount.collectAsStateWithLifecycle()
    val hasCritical by PlatformNotificationHelper.hasCritical.collectAsStateWithLifecycle()
    val notifications = remember(notificationCount) { PlatformNotificationHelper.getNotifications() }
    var showNotificationDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        topBarWrapper {
            AdminAppStatusBar(
                systemControls = trailingContent,
                title = "Citas Inventory",
                logoPainter = null,
                isDev = false,
                badgeCount = notificationCount,
                hasCriticalAlert = hasCritical,
                onNotificationsClick = { showNotificationDialog = true },
                onThemeToggle = { prefs.toggleTheme() },
                onFontScaleToggle = { prefs.nextFontScale() },
                isDarkTheme = isDarkTheme == true,
                fontScale = fontScale,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Box(Modifier.weight(1f)) {
            content()
        }
    }
}