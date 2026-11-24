package com.citacomsys.inventory.admin.system

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.citacomsys.admin.api.system.SystemApiService
import com.citacomsys.core.spi.PlatformFeature
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.FeatureNavigationPlugin
import com.citacomsys.core.spi.Navigator
import com.citacomsys.inventory.admin.app.nav.AdminSystemModuleNavKey
import com.citacomsys.core.spi.scenes.ListDetailScene
import com.citacomsys.inventory.admin.appRelease.AdminReleaseManagementScreen
import com.citacomsys.inventory.admin.appRelease.AdminReleaseUploadScreen
import com.citacomsys.inventory.admin.system.remote.KtorSystemApiService
import com.citacomsys.inventory.admin.system.presentation.pkg.AdminPackageDetailsScreen
import com.citacomsys.inventory.admin.system.presentation.pkg.AdminPackageDetailsViewModel
import com.citacomsys.inventory.admin.system.presentation.pkg.AdminPackageListScreen
import com.citacomsys.inventory.admin.system.presentation.pkg.AdminPackageListViewModel
import com.citacomsys.inventory.admin.system.presentation.promo.AdminPlatformContentScreen
import com.citacomsys.inventory.admin.system.presentation.promo.AdminPlatformContentViewModel
import com.citacomsys.inventory.admin.system.presentation.promo.AdminPromoCampaignScreen
import com.citacomsys.inventory.admin.system.presentation.promo.AdminPromoCampaignViewModel
import com.citacomsys.inventory.admin.system.presentation.smsPkg.AdminSmsPackageScreen
import com.citacomsys.inventory.admin.system.presentation.smsPkg.AdminSmsPackageViewModel
import com.citacomsys.inventory.admin.system.presentation.AdminSystemHomeScreen
import com.citacomsys.inventory.admin.system.presentation.settings.AdminSystemSettingsScreen
import com.citacomsys.inventory.admin.system.presentation.settings.AdminSystemSettingsViewModel
import com.citacomsys.inventory.admin.system.presentation.capability.CapabilityMatrixScreen
import com.citacomsys.core.spi.PluginTabItem
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.core.Koin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

@Serializable
sealed interface AdminSystemNav : NavKey

@SerialName("admin.system.settings")
@Serializable
data object AdminSystemSettingsNav : AdminSystemNav

@SerialName("admin.system.packages")
@Serializable
data object AdminPackageNav : AdminSystemNav

@SerialName("admin.system.packages.details")
@Serializable
data class AdminPackageDetailsNav(val packageId: String) : AdminSystemNav

@SerialName("admin.system.sms")
@Serializable
data object AdminSmsPackageNav : AdminSystemNav

@SerialName("admin.system.promotions")
@Serializable
data object PromoCampaignNav : AdminSystemNav

@SerialName("admin.system.content")
@Serializable
data object PlatformContentNav : AdminSystemNav

@SerialName("admin.system.capabilities")
@Serializable
data object CapabilityMatrixNav : AdminSystemNav


// Apps
@Serializable
data object AdminAppReleases : AdminSystemNav

@Serializable
data object AdminAppReleaseUpload : AdminSystemNav


private val adminSystemSharedModule = module {
    single<SystemApiService> { KtorSystemApiService(get()) }
    viewModelOf(::AdminPackageListViewModel)
    viewModelOf(::AdminPackageDetailsViewModel)
    viewModelOf(::AdminSmsPackageViewModel)
    viewModelOf(::AdminPromoCampaignViewModel)
    viewModelOf(::AdminSystemSettingsViewModel)
    viewModelOf(::AdminPlatformContentViewModel)
}

class SystemModuleNavPlatform : PlatformFeature {
    @OptIn(ExperimentalSerializationApi::class)
    override val navigationPlugin: FeatureNavigationPlugin =
        object : FeatureNavigationPlugin {
            override val id: String = "system"

            override val tabs: List<PluginTabItem> = listOf(
                PluginTabItem(
                    route = AdminSystemModuleNavKey,
                    selectedIcon = AppIcons.AdminPanelSettingsFilled,
                    unselectedIcon = AppIcons.AdminPanelSettingsOutlined,
                    label = "System"
                )
            )

            override val serializersModule: SerializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    subclassesOfSealed<AdminSystemNav>()
                    subclass(AdminSystemModuleNavKey::class, AdminSystemModuleNavKey.serializer())
                }
            }

            override fun EntryProviderScope<NavKey>.registerEntries(
                navigator: Navigator,
                isCompact: Boolean
            ) {
                // Root System Hub (Grid of modules)
                entry<AdminSystemModuleNavKey> {
                    AdminSystemHomeScreen()
                }

                entry<AdminAppReleaseUpload> {
                    AdminReleaseUploadScreen(
                        onDismiss = { navigator.goBack() }
                    )
                }

                // Product Package Management List
                entry<AdminPackageNav>(
                    metadata = ListDetailScene.listPane()
                ) {
                    AdminPackageListScreen()
                }

                // Package Details: Focused Dialog Scene
                // Ideal for complex pkg tasks while keeping the list visible in the background.
                entry<AdminPackageDetailsNav>(
                    metadata = ListDetailScene.detailPane()
                ) { key ->
                    AdminPackageDetailsScreen(
                        packageId = key.packageId,
                        onBack = { navigator.goBack() }
                    )
                }

                entry<AdminSmsPackageNav> {
                    AdminSmsPackageScreen(onBack = { navigator.goBack() })
                }

                // Global Platform Settings (MoMo, Support, Background Jobs)
                entry<AdminSystemSettingsNav> {
                    AdminSystemSettingsScreen(onBack = { navigator.goBack() })
                }

                // Promotion Campaigns
                entry<PromoCampaignNav> {
                    AdminPromoCampaignScreen(onBack = { navigator.goBack() })
                }

                entry<PlatformContentNav> {
                    AdminPlatformContentScreen(onBack = { navigator.goBack() })
                }

                entry<CapabilityMatrixNav> {
                    CapabilityMatrixScreen(onBack = { navigator.goBack() })
                }

                entry<AdminAppReleases> {
                    AdminReleaseManagementScreen(onBack = { navigator.goBack() })
                }
            }
        }

    override fun activateServices(koin: Koin) {
        koin.loadModules(listOf(adminSystemSharedModule))
    }

    override fun deactivate(koin: Koin) {
        koin.unloadModules(listOf(adminSystemSharedModule))
    }
}

