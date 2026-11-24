package com.citacomsys.feature.workspace

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.citacomsys.api.workspace.NewWorkspaceNav
import com.citacomsys.api.workspace.WorkspaceBrandNav
import com.citacomsys.api.workspace.WorkspaceDashboardNav
import com.citacomsys.api.workspace.WorkspaceProfileNav
import com.citacomsys.api.workspace.WorkspaceSelectionNav
import com.citacomsys.client.customer.ui.widget.CustomerGrowthWidget
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.DashboardPlacement
import com.citacomsys.core.spi.FeatureNavigationPlugin
import com.citacomsys.core.spi.LocalNavigator
import com.citacomsys.core.spi.MeshDiscoveryNav
import com.citacomsys.core.spi.Navigator
import com.citacomsys.core.spi.PluginDashboardWidget
import com.citacomsys.core.spi.PluginSettingsContribution
import com.citacomsys.core.spi.PluginTabItem
import com.citacomsys.core.spi.QuickAction
import com.citacomsys.core.spi.SettingsGroup
import com.citacomsys.core.spi.WsNavigationPlugin
import com.citacomsys.feature.workspace.ui.dashboard.WorkspaceDashboardScreen
import com.citacomsys.feature.workspace.ui.launcher.WorkspaceSelectorScreen
import com.citacomsys.feature.workspace.ui.profile.WorkspaceProfileScreen
import com.citacomsys.feature.workspace.ui.registration.orchestrator.WorkspaceSetupHost
import com.citacomsys.feature.workspace.ui.settings.BrandingSettingsScreen
import com.citacomsys.feature.workspace.ui.widget.FastMoversWidget
import com.citacomsys.feature.workspace.ui.widget.WorkspaceResourceQuotaWidget
import com.citacomsys.shared.event.AppLifecycleEvent
import com.citacomsys.shared.event.EventBus
import com.citacomsys.types.UserCapability
import com.design.components.AppSettingsItem
import kotlinx.coroutines.launch
import kotlinx.serialization.modules.SerializersModule

class WorkspaceGateNavigationPlugin : FeatureNavigationPlugin {
    override val id = "workspace.gate"

    override val serializersModule: SerializersModule = SerializersModule {}

    @OptIn(ExperimentalMaterial3Api::class)
    override fun EntryProviderScope<NavKey>.registerEntries(
        navigator: Navigator,
        isCompact: Boolean
    ) {
        entry<WorkspaceSelectionNav> {
            WorkspaceSelectorScreen()
        }

        entry<NewWorkspaceNav> {
            WorkspaceSetupHost(
                onComplete = { navigator.goBack() },
                onBackToHub = { navigator.goBack() }
            )
        }
    }
}

class WorkspaceActiveNavigationPlugin : WsNavigationPlugin {
    override val id = "workspace.active"

    override val serializersModule: SerializersModule = SerializersModule {}

    override val tabs: List<PluginTabItem> = listOf(
        PluginTabItem(
            route = WorkspaceDashboardNav,
            label = "Dashboard",
            order = 0,
            selectedIcon = AppIcons.DashboardFilled,
            unselectedIcon = AppIcons.DashboardOutlined
        )
    )

    override val dashboardWidgets: List<PluginDashboardWidget> = listOf(
        PluginDashboardWidget(
            id = "workspace.fast_movers",
            placement = DashboardPlacement.LIST,
            order = 15,
            content = {
                FastMoversWidget()
            }
        ),
        PluginDashboardWidget(
            id = "workspace.customer_receivables",
            placement = DashboardPlacement.LIST,
            order = 16,
            content = {
                CustomerGrowthWidget()
            }
        ),
        PluginDashboardWidget(
            id = "workspace.resource_quota",
            placement = DashboardPlacement.GRID,
            order = 17,
            content = {
                WorkspaceResourceQuotaWidget()
            }
        )
    )

    override val pluginSettingsContributions: List<PluginSettingsContribution> = mutableListOf(
        PluginSettingsContribution.CustomWidget(
            id = "workspace.profile",
            group = SettingsGroup.ACCOUNT,
            order = 0,
            content = {
                val navigator = LocalNavigator.current
                AppSettingsItem(
                    title = "Business Profile",
                    subtitle = "Identity, contact and public details",
                    icon = AppIcons.Business,
                    onClick = { navigator.navigateToSingleTop(WorkspaceProfileNav) }
                )
            },
            requiredCapabilities = setOf(UserCapability.MANAGE_SETTINGS)
        ),
        PluginSettingsContribution.CustomWidget(
            id = "workspace.satellite_mesh_discovery",
            group = SettingsGroup.NODE,
            order = 5,
            content = {
                val navigator = LocalNavigator.current
                AppSettingsItem(
                    title = "Satellite Terminal LAN Discovery",
                    subtitle = "Scan local LAN for Mother Hubs and pair terminal",
                    icon = AppIcons.Search,
                    onClick = { navigator.navigateToSingleTop(MeshDiscoveryNav) }
                )
            }
        ),
        PluginSettingsContribution.CustomWidget(
            id = "workspace.logout",
            group = SettingsGroup.ACCOUNT,
            order = 99,
            content = {
                val scope = rememberCoroutineScope()
                AppSettingsItem(
                    title = "Logout Business",
                    subtitle = "Sign out of the current business",
                    icon = AppIcons.Logout,
                    onClick = {
                        scope.launch {
                            EventBus.publish(AppLifecycleEvent.ExistWorkspace)
                        }
                    }
                )
            }
        ),
    )

    override val quickActions: List<QuickAction> = mutableListOf(
        QuickAction(
            id = "workspace.quick_action.mesh_discovery",
            label = "Discover LAN Hubs",
            icon = AppIcons.Search,
            onClick = { navigator ->
                navigator.navigateToSingleTop(MeshDiscoveryNav)
            }
        ),
        QuickAction(
            id = "workspace.profile",
            label = "Business Profile",
            icon = AppIcons.Business,
            onClick = { navigator ->
                navigator.navigateToSingleTop(WorkspaceProfileNav)
            },
            requiredCapabilities = setOf(UserCapability.MANAGE_SETTINGS)
        ),
        QuickAction(
            id = "workspace.branding",
            label = "Visual Identity",
            icon = AppIcons.Palette,
            onClick = { navigator ->
                navigator.navigateToSingleTop(WorkspaceBrandNav)
            },
            requiredCapabilities = setOf(UserCapability.MANAGE_SETTINGS)
        )
    )


    @OptIn(ExperimentalMaterial3Api::class)
    override fun EntryProviderScope<NavKey>.registerEntries(
        navigator: Navigator,
        isCompact: Boolean
    ) {
        entry<WorkspaceDashboardNav> {
            WorkspaceDashboardScreen()
        }

        entry<WorkspaceProfileNav> {
            WorkspaceProfileScreen()
        }

        entry<WorkspaceBrandNav> {
            BrandingSettingsScreen()
        }
    }
}
