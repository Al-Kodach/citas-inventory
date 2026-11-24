package com.citacomsys.settings.ui.screen

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.CoreFeature
import com.citacomsys.core.spi.LegalNav
import com.citacomsys.core.spi.LocalNavigator
import com.citacomsys.core.spi.MeshDiagnosticsNav
import com.citacomsys.core.spi.MeshDiscoveryNav
import com.citacomsys.core.spi.MeshNodePairingCenterNav
import com.citacomsys.core.spi.SoftwareInfoNav
import com.citacomsys.core.spi.SyncPolicyNav
import com.citacomsys.core.spi.UserProfileNav
import com.citacomsys.core.ui.legal.LegalComplianceScreen
import com.citacomsys.feature.api.telemetry.CommonUpdateState
import com.citacomsys.feature.api.telemetry.PlatformAppUpdater
import com.citacomsys.feature.settings.api.AppSettingsNav
import com.citacomsys.settings.settingsFeatureModule
import com.citacomsys.settings.ui.mesh.MeshDiagnosticsScreen
import com.citacomsys.settings.ui.mesh.MeshDiscoveryScreen
import com.citacomsys.settings.ui.mesh.MeshNodePairingCenterScreen
import com.citacomsys.settings.ui.screen.settings.SettingsScreen
import com.citacomsys.settings.ui.screen.software.SoftwareInfoScreen
import com.citacomsys.settings.ui.sync.SyncPolicyScreen
import com.design.components.AppSettingsItem
import com.citacomsys.core.spi.FeatureNavigationPlugin
import com.citacomsys.core.spi.Navigator
import com.citacomsys.core.spi.PluginSettingsContribution
import com.citacomsys.core.spi.PluginTabItem
import com.citacomsys.core.spi.SettingsGroup
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.koinInject
import org.koin.core.Koin

class SettingsFeature : CoreFeature {
    override val id: String = "core.settings"

    override val navigationPlugin: FeatureNavigationPlugin = SettingsNavPlugin()

    override fun activate(koin: Koin) {
        koin.loadModules(listOf(settingsFeatureModule))
    }
}


class SettingsNavPlugin : FeatureNavigationPlugin {
    override val id: String = "core.settings"
    override val serializersModule: SerializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(AppSettingsNav::class, AppSettingsNav.serializer())
            subclass(MeshDiagnosticsNav::class, MeshDiagnosticsNav.serializer())
            subclass(MeshDiscoveryNav::class, MeshDiscoveryNav.serializer())
            subclass(MeshNodePairingCenterNav::class, MeshNodePairingCenterNav.serializer())
            subclass(SyncPolicyNav::class, SyncPolicyNav.serializer())
            subclass(LegalNav::class, LegalNav.serializer())
            subclass(UserProfileNav::class, UserProfileNav.serializer())
            subclass(SoftwareInfoNav::class, SoftwareInfoNav.serializer())
        }
    }

    override val tabs = listOf(
        PluginTabItem(
            route = AppSettingsNav,
            label = "Settings",
            order = 999,
            selectedIcon = AppIcons.SettingsFilled,
            unselectedIcon = AppIcons.SettingsOutlined,
            isFooter = true
        ),
        PluginTabItem(
            route = UserProfileNav,
            label = "Profile",
            order = 948,
            selectedIcon = AppIcons.AccountCircle,
            unselectedIcon = AppIcons.AccountCircle,
            isFooter = true
        )
    )

    override val pluginSettingsContributions: List<PluginSettingsContribution> = listOf(
//        PluginSettingsContribution.CustomWidget(
//            id = "core.mesh_discovery",
//            group = SettingsGroup.NODE,
//            order = 5,
//            content = {
//                val navigator = LocalNavigator.current
//                AppSettingsItem(
//                    title = "Local Server Discovery",
//                    subtitle = "Scan local LAN for Mother Hubs and connect terminal",
//                    icon = AppIcons.Search,
//                    onClick = { navigator.navigateToSingleTop(MeshDiscoveryNav) }
//                )
//            }
//        ),

        PluginSettingsContribution.CustomWidget(
            id = "core.mesh_diagnostics",
            group = SettingsGroup.SYSTEM,
            order = 10,
            content = {
                val navigator = LocalNavigator.current
                AppSettingsItem(
                    title = "Industrial Mesh Diagnostics",
                    subtitle = "Monitor local LAN hub state and connected satellites",
                    icon = AppIcons.Devices,
                    onClick = { navigator.navigateToSingleTop(MeshDiagnosticsNav) }
                )
            }
        ),
        PluginSettingsContribution.CustomWidget(
            id = "core.sync_policy",
            group = SettingsGroup.SYSTEM,
            order = 11,
            content = {
                val navigator = LocalNavigator.current
                AppSettingsItem(
                    title = "Sync Policy & Constraints",
                    subtitle = "Configure workspace sync scope and Mother Hub relay rules",
                    icon = AppIcons.Sync,
                    onClick = { navigator.navigateToSingleTop(SyncPolicyNav) }
                )
            }
        ),
        PluginSettingsContribution.CustomWidget(
            id = "core.legal",
            group = SettingsGroup.SYSTEM,
            order = 12,
            content = {
                val navigator = LocalNavigator.current
                AppSettingsItem(
                    title = "Legal & Compliance",
                    subtitle = "Terms of service and privacy policy",
                    icon = AppIcons.Rule,
                    onClick = { navigator.navigateToSingleTop(LegalNav) }
                )
            }
        ),
        // SYSTEM
        PluginSettingsContribution.CustomWidget(
            id = "core.software_info",
            group = SettingsGroup.SYSTEM,
            order = 0,
            content = {
                val navigator = LocalNavigator.current
                val appUpdater: PlatformAppUpdater = koinInject()
                val updateState by appUpdater.syncState.collectAsStateWithLifecycle(
                    CommonUpdateState.Idle
                )

                val subtitleText = when (val state = updateState) {
                    is CommonUpdateState.UpdateAvailable -> "New version v${state.release.version} available!"
                    is CommonUpdateState.Downloading -> "Downloading update: ${(state.progress * 100).toInt()}%"
                    is CommonUpdateState.ReadyToInstall -> "Update ready to install"
                    else -> "v${appUpdater.currentVersion} • Software Up to Date"
                }

                AppSettingsItem(
                    title = "Software Information",
                    subtitle = subtitleText,
                    icon = AppIcons.Info,
                    onClick = { navigator.navigateToSingleTop(SoftwareInfoNav) }
                )
            }
        )
    )

    override fun EntryProviderScope<NavKey>.registerEntries(
        navigator: Navigator,
        isCompact: Boolean
    ) {
        entry<AppSettingsNav> {
            SettingsScreen(
                onHeaderClick = {
                    navigator.navigateToSingleTop(UserProfileNav)
                },
                onBack = if (isCompact) navigator::goBack else null
            )
        }

        entry<MeshDiagnosticsNav> {
            MeshDiagnosticsScreen(onBackClick = { navigator.goBack() })
        }

        entry<MeshDiscoveryNav> {
            MeshDiscoveryScreen(
                onBackClick = { navigator.goBack() },
                onNavigateToPairingCenter = { navigator.navigateToSingleTop(MeshNodePairingCenterNav) }
            )
        }

        entry<MeshNodePairingCenterNav> {
            MeshNodePairingCenterScreen(
                onBackClick = { navigator.goBack() },
                onNavigateToDiscovery = { navigator.navigateToSingleTop(MeshDiscoveryNav) }
            )
        }

        entry<SyncPolicyNav> {
            SyncPolicyScreen(onBackClick = { navigator.goBack() })
        }

        entry<LegalNav> {
            LegalComplianceScreen(onBackClick = { navigator.goBack() })
        }

        entry<SoftwareInfoNav> {
            SoftwareInfoScreen(onBackClick = { navigator.goBack() })
        }
    }
}
