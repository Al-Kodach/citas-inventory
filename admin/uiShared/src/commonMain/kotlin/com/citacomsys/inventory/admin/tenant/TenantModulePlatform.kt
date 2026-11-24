@file:OptIn(ExperimentalMaterial3AdaptiveApi::class)

package com.citacomsys.inventory.admin.tenant

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.citacomsys.admin.api.tenant.TenantApiService
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.FeatureNavigationPlugin
import com.citacomsys.core.spi.PlatformFeature
import com.citacomsys.inventory.admin.device.presentation.DeviceHardwareMetadataView
import com.citacomsys.inventory.admin.security.SecurityDeviceDetailsNav
import com.citacomsys.inventory.admin.tenant.remote.TenantApiServiceImpl
import com.citacomsys.inventory.admin.tenant.ui.AdminTenantAnalyticsWidget
import com.citacomsys.inventory.admin.tenant.screen.AdminTenantDetailsScreen
import com.citacomsys.inventory.admin.tenant.screen.AdminTenantListScreen
import com.citacomsys.inventory.admin.tenant.screen.OrganizationsListViewModel
import com.citacomsys.inventory.admin.tenant.screen.AdminTenantDetailsViewModel
import com.citacomsys.inventory.admin.tenant.preregistration.AdminPreRegistrationScreen
import com.citacomsys.inventory.admin.tenant.preregistration.AdminPreRegistrationViewModel
import com.citacomsys.types.UserCapability
import com.citacomsys.core.spi.DashboardPlacement
import com.citacomsys.core.spi.Navigator
import com.citacomsys.core.spi.PluginDashboardWidget
import com.citacomsys.core.spi.PluginTabItem
import com.citacomsys.core.spi.scenes.ListDetailScene
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.core.Koin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val adminTenantSharedModule = module {
    single<TenantApiService> { TenantApiServiceImpl(get()) }
    viewModelOf(::OrganizationsListViewModel)
    viewModelOf(::AdminTenantDetailsViewModel)
    viewModelOf(::AdminPreRegistrationViewModel)
}

class TenantModulePlatform : PlatformFeature {
    @OptIn(ExperimentalSerializationApi::class)
    override val navigationPlugin: FeatureNavigationPlugin = TenantNavPlugin()

    override val requiredUserCapabilities: Set<UserCapability> = setOf(UserCapability.VIEW_TENANTS)

    override fun activateServices(koin: Koin) {
        koin.loadModules(listOf(adminTenantSharedModule))
    }

    override fun deactivate(koin: Koin) {
        koin.unloadModules(listOf(adminTenantSharedModule))
    }
}

@Serializable
@SerialName("admin.tenants")
sealed interface TenantModuleNavKey : NavKey

@Serializable
data object TenantNav : TenantModuleNavKey

@Serializable
data class TenantDetailsNav(val organizationId: String) : TenantModuleNavKey

@Serializable
data object AdminPreRegistrationNav : TenantModuleNavKey

@Serializable
data class TenantDeviceDetails(val deviceId: String) : TenantModuleNavKey

class TenantNavPlugin : FeatureNavigationPlugin {
    override val id: String = "tenants_module"

    override val tabs: List<PluginTabItem> = listOf(
        PluginTabItem(
            route = TenantNav,
            order = 2,
            selectedIcon = AppIcons.BusinessFilled,
            unselectedIcon = AppIcons.BusinessOutlined,
            label = "Business"
        )
    )

    override val dashboardWidgets: List<PluginDashboardWidget> = listOf(
        PluginDashboardWidget(
            id = "admin.tenant.analytics",
            placement = DashboardPlacement.ANALYTICS_CAROUSEL,
            order = 10,
            content = {
                AdminTenantAnalyticsWidget()
            }
        )
    )

    @OptIn(ExperimentalSerializationApi::class)
    override val serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclassesOfSealed<TenantModuleNavKey>()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun EntryProviderScope<NavKey>.registerEntries(
        navigator: Navigator,
        isCompact: Boolean
    ) {
        entry<TenantNav>(
            metadata = ListDetailScene.listPane()
        ) {
            AdminTenantListScreen(
                onTenantClick = {
                    navigator.navigateToSingleTop(TenantDetailsNav(it))
                },
                onPreRegisterClick = {
                    navigator.navigateToSingleTop(AdminPreRegistrationNav)
                }
            )
        }


        entry<TenantDetailsNav>(
            metadata = ListDetailScene.detailPane()
        ) { key ->
            AdminTenantDetailsScreen(
                organizationId = key.organizationId,
                onBack = { navigator.goBack() },
            )
        }

        entry<AdminPreRegistrationNav>(
            metadata = ListDetailScene.detailPane()
        ) {
            AdminPreRegistrationScreen(
                onBack = { navigator.goBack() }
            )
        }

        entry<TenantDeviceDetails>(
            metadata = ListDetailScene.detailPane()
        ) { key ->
            Column(Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Device Details") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.goBack() }) {
                            Icon(AppIcons.Close, contentDescription = "Back")
                        }
                    }
                )
                DeviceHardwareMetadataView(
                    deviceId = key.deviceId,
                    modifier = Modifier.weight(1f),
                    onNavigateToSecurity = {
                        navigator.navigateToSingleTop(SecurityDeviceDetailsNav(it))
                    }
                )
            }
        }
    }
}
