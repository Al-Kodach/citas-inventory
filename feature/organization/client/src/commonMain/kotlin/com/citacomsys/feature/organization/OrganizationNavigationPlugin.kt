package com.citacomsys.feature.organization

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.citacomsys.api.organization.nav.BranchManagementNav
import com.citacomsys.api.organization.nav.OrganizationHubNav
import com.citacomsys.api.organization.nav.OrganizationMonitoringNav
import com.citacomsys.api.organization.nav.OrganizationProfileNav
import com.citacomsys.api.organization.nav.OrganizationReportsNav
import com.citacomsys.api.organization.nav.OrganizationSiemAuditNav
import com.citacomsys.api.organization.nav.WorkspaceDetailsNav
import com.citacomsys.api.organization.nav.WorkspaceListNav
import com.citacomsys.api.workspace.NewWorkspaceNav
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.mesh.server.EngineState
import com.citacomsys.core.mesh.service.MeshManager
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.AppOperatingMode
import com.citacomsys.core.spi.LocalNavigator
import com.citacomsys.core.spi.MeshDiagnosticsNav
import com.citacomsys.core.spi.OrgNavigationPlugin
import com.citacomsys.core.spi.QuickAction
import com.citacomsys.core.spi.TenantSecurityNav
import com.citacomsys.kernel.service.PlatformRuntimeStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.citacomsys.core.spi.scenes.ListDetailScene
import com.citacomsys.feature.organization.ui.audit.OrganizationSiemAuditScreen
import com.citacomsys.feature.organization.ui.details.WorkspaceDetailsScreen
import com.citacomsys.feature.organization.ui.hub.OrganizationDashboardScreen
import com.citacomsys.feature.organization.ui.hub.dashboard.impl.BusinessReportsWidget
import com.citacomsys.feature.organization.ui.hub.dashboard.impl.MeshSyncHealthWidget
import com.citacomsys.feature.organization.ui.hub.dashboard.impl.NodeActivityStreamWidget
import com.citacomsys.feature.organization.ui.monitoring.OrganizationMonitoringScreen
import com.citacomsys.feature.organization.ui.profile.OrganizationProfileScreen
import com.citacomsys.feature.organization.ui.reports.OrganizationReportsScreen
import com.citacomsys.feature.organization.ui.workspace.WorkspaceListScreen
import com.citacomsys.types.UserCapability
import com.design.components.AppSettingsItem
import com.citacomsys.core.spi.DashboardPlacement
import com.citacomsys.core.spi.MeshNodePairingCenterNav
import com.citacomsys.core.spi.Navigator
import com.citacomsys.core.spi.PluginDashboardWidget
import com.citacomsys.core.spi.PluginSettingsContribution
import com.citacomsys.core.spi.PluginTabItem
import com.citacomsys.core.spi.SettingsGroup
import com.citacomsys.feature.workspace.ui.branch.BranchManagementScreen
import kotlinx.serialization.modules.SerializersModule
import org.koin.mp.KoinPlatform

class OrganizationNavigationPlugin : OrgNavigationPlugin {
    override val id = "organization.module"

    override val serializersModule: SerializersModule = SerializersModule {}

    override fun isVisible(route: NavKey?, item: Any, mode: AppOperatingMode): Boolean {
        return mode == AppOperatingMode.ORG
    }

    override val pluginSettingsContributions: List<PluginSettingsContribution>
        get() {
            return listOf(
                PluginSettingsContribution.CustomWidget(
                    id = "organization.profile",
                    group = SettingsGroup.ACCOUNT,
                    order = 0,
                    content = {
                        val navigator = LocalNavigator.current
                        AppSettingsItem(
                            title = "Company Profile",
                            subtitle = "Legal identity and organization metrics",
                            icon = AppIcons.Business,
                            onClick = { navigator.navigateToSingleTop(OrganizationProfileNav) }
                        )
                    },
                    requiredCapabilities = setOf(UserCapability.MANAGE_SETTINGS)
                ),
                PluginSettingsContribution.CustomWidget(
                    id = "organization.reports",
                    group = SettingsGroup.ACCOUNT,
                    order = 1,
                    content = {
                        val navigator = LocalNavigator.current
                        AppSettingsItem(
                            title = "Business Reports",
                            subtitle = "Consolidated revenue and inventory valuation",
                            icon = AppIcons.Analytics,
                            onClick = { navigator.navigateToSingleTop(OrganizationReportsNav) }
                        )
                    }
                ),
                PluginSettingsContribution.CustomWidget(
                    id = "core.security_hub",
                    group = SettingsGroup.SECURITY,
                    order = 0,
                    content = {
                        val navigator = LocalNavigator.current
                        AppSettingsItem(
                            title = "Security Hub",
                            subtitle = "Active security policies and cloud audit",
                            icon = AppIcons.Security,
                            onClick = { navigator.navigateToSingleTop(TenantSecurityNav) }
                        )
                    }
                ),
                PluginSettingsContribution.CustomWidget(
                    id = "organization.siem_audit",
                    group = SettingsGroup.SECURITY,
                    order = 1,
                    content = {
                        val navigator = LocalNavigator.current
                        AppSettingsItem(
                            title = "SIEM Security Audit",
                            subtitle = "Cross-branch security and hardware attestation feed",
                            icon = AppIcons.Security,
                            onClick = { navigator.navigateToSingleTop(OrganizationSiemAuditNav) }
                        )
                    }
                ),
                PluginSettingsContribution.CustomWidget(
                    id = "core.organization_monitoring",
                    group = SettingsGroup.SECURITY,
                    order = 2,
                    content = {
                        val navigator = LocalNavigator.current
                        AppSettingsItem(
                            title = "Monitoring",
                            subtitle = "Global audit feed across all workspaces",
                            icon = AppIcons.Analytics,
                            onClick = { navigator.navigateToSingleTop(OrganizationMonitoringNav) }
                        )
                    }
                ),
                PluginSettingsContribution.CustomWidget(
                    id = "core.mesh_node_pairing_center",
                    group = SettingsGroup.HUB,
                    order = 6,
                    content = {
                        val navigator = LocalNavigator.current
                        AppSettingsItem(
                            title = "Node Pairing Center",
                            subtitle = "View registered terminal nodes and server engine",
                            icon = AppIcons.Devices,
                            onClick = { navigator.navigateToSingleTop(MeshNodePairingCenterNav) }
                        )
                    }
                ),
            )
        }

    override val dashboardWidgets: List<PluginDashboardWidget>
        get() = listOf(
            PluginDashboardWidget(
                id = "organization.business_reports",
                placement = DashboardPlacement.GRID,
                order = 1,
                content = {
                    BusinessReportsWidget()
                }
            ),
            PluginDashboardWidget(
                id = "organization.mesh_sync_health",
                placement = DashboardPlacement.GRID,
                order = 2,
                content = {
                    MeshSyncHealthWidget()
                }
            ),
            PluginDashboardWidget(
                id = "organization.node_activities",
                placement = DashboardPlacement.ACTIVITY_STREAM,
                order = 30,
                content = {
                    NodeActivityStreamWidget()
                }
            )
        )

    override val quickActions: List<QuickAction>
        get() = listOf(
            QuickAction(
                id = "organization.quick_action.toggle_hub",
                label = "Mother Hub Engine",
                icon = AppIcons.Public,
                onClick = { navigator ->
                    val koin = KoinPlatform.getKoin()
                    val meshManager = koin.get<MeshManager>()
                    val platformStore = koin.get<PlatformRuntimeStore>()
                    val sessionManager = koin.get<AppSessionManager>()

                    val orgId = platformStore.organizationId
                    val activeWsId = sessionManager.activeWorkspaceId
                        ?: platformStore.organizationConfig.value?.workspaces?.firstOrNull()?.id
                    val isBound = !orgId.isNullOrBlank() && !activeWsId.isNullOrBlank()

                    if (!isBound) {
                        navigator.navigateToSingleTop(MeshDiagnosticsNav)
                    } else {
                        val scope = koin.get<CoroutineScope>()
                        scope.launch {
                            if (meshManager.state.value == EngineState.Running) {
                                meshManager.stopMesh()
                            } else {
                                meshManager.startMesh()
                            }
                        }
                    }
                },
                order = 5
            )
        )

    override val tabs: List<PluginTabItem> = listOf(
        PluginTabItem(
            route = OrganizationHubNav,
            label = "Home",
            selectedIcon = AppIcons.Home,
            unselectedIcon = AppIcons.Home,
            order = 1
        ),
        PluginTabItem(
            route = WorkspaceListNav,
            label = "Business",
            selectedIcon = AppIcons.Storefront,
            unselectedIcon = AppIcons.Storefront,
            order = 2
        )
    )

    @OptIn(ExperimentalMaterial3Api::class)
    override fun EntryProviderScope<NavKey>.registerEntries(
        navigator: Navigator,
        isCompact: Boolean
    ) {
        entry<WorkspaceListNav>(
            metadata = ListDetailScene.listPane()
        ) {
            WorkspaceListScreen(
                onWorkspaceClick = { workspaceId ->
                    navigator.navigateToSingleTop(WorkspaceDetailsNav(workspaceId))
                },
                onSetupNewWorkspace = { navigator.navigateToSingleTop(NewWorkspaceNav) }
            )
        }

        entry<WorkspaceDetailsNav>(
            metadata = ListDetailScene.listPane()
        ) { navKey ->
            WorkspaceDetailsScreen(
                isCompact = isCompact,
                workspaceId = navKey.workspaceId,
                onBack = { navigator.goBack() },
            )
        }

        entry<BranchManagementNav>(
            metadata = ListDetailScene.detailPane()
        ) { key ->
            BranchManagementScreen(
                workspaceId = key.workspaceId,
                goBack = { navigator.goBack() },
            )
        }

        entry<OrganizationProfileNav> {
            OrganizationProfileScreen()
        }

        entry<OrganizationHubNav> {
            OrganizationDashboardScreen(
                onNavigateToProfile = {
                    navigator.navigateToSingleTop(OrganizationProfileNav)
                }
            )
        }

        entry<OrganizationReportsNav> {
            OrganizationReportsScreen(
                summary = null,
                onBackClick = { navigator.goBack() }
            )
        }

        entry<OrganizationSiemAuditNav> {
            OrganizationSiemAuditScreen(
                auditLogs = emptyList(),
                onBackClick = { navigator.goBack() }
            )
        }

        entry<OrganizationMonitoringNav> { OrganizationMonitoringScreen() }
    }
}
