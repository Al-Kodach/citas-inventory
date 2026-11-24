package com.citacomsys.inventory.admin.security

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.citacomsys.feature.api.telemetry.SiemApiService
import com.citacomsys.core.spi.PlatformFeature

import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.FeatureNavigationPlugin
import com.citacomsys.core.spi.Navigator
import com.citacomsys.core.spi.scenes.ListDetailScene
import com.citacomsys.inventory.admin.app.nav.AdminDeviceMetadataNav
import com.citacomsys.inventory.admin.device.presentation.DeviceHardwareMetaInfoViewModel
import com.citacomsys.inventory.admin.device.presentation.DeviceHardwareMetadataView
import com.citacomsys.inventory.admin.security.audit.SecurityAuditFeedScreen
import com.citacomsys.inventory.admin.security.audit.SecurityAuditViewModel
import com.citacomsys.inventory.admin.security.audit.SecurityTraceDetailsScreen
import com.citacomsys.inventory.admin.device.presentation.DeviceListViewModel
import com.citacomsys.inventory.admin.security.device.SecurityDeviceDetailsScreen
import com.citacomsys.inventory.admin.security.device.SecurityDeviceDetailsViewModel
import com.citacomsys.inventory.admin.security.device.SecurityDeviceListScreen
import com.citacomsys.inventory.admin.security.remote.KtorSiemApiService
import com.citacomsys.inventory.admin.security.siem.SiemDashboardScreen
import com.citacomsys.inventory.admin.security.siem.SiemDashboardViewModel
import com.citacomsys.core.spi.PluginTabItem
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.core.Koin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

private val securityModule = module {
    viewModelOf(::SiemDashboardViewModel)
    single<SiemApiService> { KtorSiemApiService(get()) }
    viewModelOf(::SecurityAuditViewModel)
    viewModelOf(::SecurityDeviceDetailsViewModel)
    viewModelOf(::DeviceListViewModel)
    viewModelOf(::DeviceHardwareMetaInfoViewModel)
}

class SecurityModuleNavPlatform : PlatformFeature {
    @OptIn(ExperimentalSerializationApi::class)
    override val navigationPlugin: FeatureNavigationPlugin = AdminSecurityNavPlugin()

    override fun activateServices(koin: Koin) {
        koin.loadModules(listOf(securityModule))
    }

    override fun deactivate(koin: Koin) {
        koin.unloadModules(listOf(securityModule))
    }
}

@Serializable
sealed interface SecurityNavKey : NavKey

@Serializable
@SerialName("admin.security")
data object SecurityNav : SecurityNavKey

@SerialName("admin.security.siem")
@Serializable
data object SiemDashboardNav : SecurityNavKey

@SerialName("admin.security.workstations")
@Serializable
data object SecurityDeviceListNav : SecurityNavKey

@Serializable
@SerialName("admin.security.device.details")
data class SecurityDeviceDetailsNav(val deviceId: String) : SecurityNavKey

@SerialName("admin.security.audit.details")
@Serializable
data class SecurityAuditDetailsNav(val requestId: String) : SecurityNavKey


@SerialName("admin.security.audit")
@Serializable
data object SecurityAuditLogsNav : SecurityNavKey


@OptIn(ExperimentalMaterial3AdaptiveApi::class)
class AdminSecurityNavPlugin : FeatureNavigationPlugin {
    override val id: String = "security"

    @OptIn(ExperimentalSerializationApi::class)
    override val serializersModule: SerializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclassesOfSealed<SecurityNavKey>()
        }
    }

    override val tabs: List<PluginTabItem> = listOf(
        PluginTabItem(
            route = SecurityNav,
            selectedIcon = AppIcons.ShieldFilled,
            unselectedIcon = AppIcons.ShieldOutlined,
            label = "Security"
        )
    )

    override fun EntryProviderScope<NavKey>.registerEntries(
        navigator: Navigator,
        isCompact: Boolean
    ) {
        entry<SecurityNav> {
            SecurityHubScreen(
                onNavigateToDevices = {
                    navigator.navigateToSingleTop(SecurityDeviceListNav)
                },
                onNavigateToAuditLogs = {
                    navigator.navigateToSingleTop(SecurityAuditLogsNav)
                },
                onNavigateToSiemDashboard = {
                    navigator.navigateToSingleTop(SiemDashboardNav)
                }
            )
        }

        entry<SiemDashboardNav> {
            SiemDashboardScreen(onBack = { navigator.goBack() })
        }

        entry<SecurityDeviceListNav>(
            metadata = ListDetailScene.listPane()
        ) {
            SecurityDeviceListScreen()
        }

        entry<SecurityDeviceDetailsNav>(
            metadata = ListDetailScene.detailPane()
        ) {
            SecurityDeviceDetailsScreen(
                it.deviceId,
                onNavigateToTrace = {
                    navigator.navigateToSingleTop(SecurityAuditDetailsNav(it))
                },
                goBack = { navigator.goBack() }
            )
        }

        entry<AdminDeviceMetadataNav>(
            metadata = ListDetailScene.detailPane()
        ) { key ->
            DeviceHardwareMetadataView(key.deviceId)
        }

        // The Global Live Audit Feed
        entry<SecurityAuditLogsNav>(
            metadata = ListDetailScene.listPane()
        ) {
            SecurityAuditFeedScreen(
                goback = { navigator.goBack() },
                onNavigateToTrace = { requestId ->
                    navigator.navigateToSingleTop(SecurityAuditDetailsNav(requestId))
                }
            )
        }

        // The Deep Dive Trace View
        entry<SecurityAuditDetailsNav>(
            metadata = ListDetailScene.detailPane()
        ) { key ->
            SecurityTraceDetailsScreen(
                requestId = key.requestId,
                onBack = { navigator.goBack() }
            )
        }
    }
}
