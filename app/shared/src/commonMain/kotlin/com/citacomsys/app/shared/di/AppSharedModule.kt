package com.citacomsys.app.shared.di

import androidx.navigation3.runtime.NavKey
import com.citacomsys.api.organization.nav.OrganizationFeatureNav
import com.citacomsys.api.sales.nav.PosModuleNavKey
import com.citacomsys.api.sales.nav.SalesModuleNavKey
import com.citacomsys.api.workspace.WorkspaceNavKey
import com.citacomsys.app.shared.app.AppStageOrchestrator
import com.citacomsys.app.shared.boot.WorkspaceInfraConfigurationManager
import com.citacomsys.app.shared.nav.AppCoreFeatures
import com.citacomsys.app.shared.screen.AppViewModel
import com.citacomsys.app.shared.sync.SyncManagerViewModel
import com.citacomsys.app.shared.util.TenantUserCapabilityManager
import com.citacomsys.app.shared.util.WorkspaceModulePolicyManager
import com.citacomsys.backup.di.backupFeatureModule
import com.citacomsys.client.customer.customerClientInfraServiceDi
import com.citacomsys.client.customer.ui.CustomerCoreFeature
import com.citacomsys.core.barcode.di.barcodeModule
import com.citacomsys.core.data.di.coreDataModule
import com.citacomsys.core.database.di.databaseModule
import com.citacomsys.core.domain.security.UserCapabilityManager
import com.citacomsys.core.mesh.di.meshModule
import com.citacomsys.core.notification.di.notificationModule
import com.citacomsys.core.printer.di.printerModule
import com.citacomsys.core.remote.di.networkModule
import com.citacomsys.core.security.tenantSecurityModule
import com.citacomsys.core.spi.AppManifest
import com.citacomsys.core.spi.CoreFeature
import com.citacomsys.core.spi.DynamicAppManifest
import com.citacomsys.core.spi.GlobalNavigationCoordinator
import com.citacomsys.core.spi.NavigationCoordinator
import com.citacomsys.core.spi.PlatformFeature
import com.citacomsys.core.spi.StartupTask
import com.citacomsys.core.sync.di.syncModule
import com.citacomsys.device.di.deviceServiceModule
import com.citacomsys.feature.auth.AuthCoreFeature
import com.citacomsys.feature.auth.di.authFeatureModule
import com.citacomsys.feature.billing.BillingCoreFeature
import com.citacomsys.feature.organization.OrganizationCoreFeature
import com.citacomsys.feature.organization.di.organizationFeatureModule
import com.citacomsys.feature.platform.PlatformCoreFeature
import com.citacomsys.feature.platform.di.platformFeatureModule
import com.citacomsys.feature.settings.api.AppSettingsNav
import com.citacomsys.feature.support.api.navigation.SupportNavKey
import com.citacomsys.feature.support.client.di.supportFeatureModule
import com.citacomsys.feature.support.client.ui.SupportCoreFeature
import com.citacomsys.feature.telemetry.di.telemetryFeatureModuleDi
import com.citacomsys.feature.workspace.WorkspaceActiveFeature
import com.citacomsys.feature.workspace.WorkspaceGateFeature
import com.citacomsys.feature.workspace.di.workspaceFeatureModule
import com.citacomsys.home.di.homeFeatureModule
import com.citacomsys.kernel.client.di.kernelClientModule
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.module.inventory.data.repository.BaseClientProductRepository
import com.citacomsys.module.inventory.di.inventoryClientInfraDI
import com.citacomsys.module.inventory.ui.InventoryFeatureModule
import com.citacomsys.pharmacy.shared.data.repository.ClientPharmacyRepository
import com.citacomsys.pharmacy.shared.pharmacyModule
import com.citacomsys.pharmacy.shared.presentation.PharmacyInventoryModule
import com.citacomsys.retail.shared.data.repository.ClientRetailRepository
import com.citacomsys.retail.shared.presentation.RetailInventoryModule
import com.citacomsys.retail.shared.retailModule
import com.citacomsys.types.BaseProduct
import com.citacomsys.types.ProductType
import com.citacomsys.sales.di.salesClientInfraModule
import com.citacomsys.sales.spi.SalesPlatformFeature
import com.citacomsys.settings.settingsFeatureModule
import com.citacomsys.settings.ui.screen.SettingsFeature
import com.citacomsys.shared.AppConfig
import com.citacomsys.user.TenantUserManagementCoreFeature
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

class TenantAppConfig : AppConfig

expect fun iODispatcher(): CoroutineDispatcher

val coreClientModule = module {
    includes(
        kernelClientModule,
        coreDataModule,
        databaseModule,
        tenantSecurityModule,
        networkModule,
        notificationModule,
        deviceServiceModule,
        syncModule,
        meshModule,
        printerModule,
        barcodeModule
    )
}

val serviceModule = module {

    includes(
        // Services
        customerClientInfraServiceDi,
        salesClientInfraModule,
        inventoryClientInfraDI,
        retailModule,
        pharmacyModule,
    )

    // Active Base Product Repository provider
    single<BaseClientProductRepository<out BaseProduct>> {
        val sessionManager: AppSessionManager = get()
        when (sessionManager.activeProductType) {
            ProductType.PHARMACY -> get<ClientPharmacyRepository>()
            else -> get<ClientRetailRepository>()
        }
    }

    // Share feature service
    single(named("sales")) { SalesPlatformFeature() } bind PlatformFeature::class
    single(named("inventory.core")) { InventoryFeatureModule() } bind PlatformFeature::class
    single(named("inventory.pharmacy")) { PharmacyInventoryModule() } bind PlatformFeature::class
    single(named("inventory.retail")) { RetailInventoryModule() } bind PlatformFeature::class

}

val coreFeatures = module {
    single(named("core")) { AppCoreFeatures() } bind CoreFeature::class
    single(named("org")) { OrganizationCoreFeature() } bind CoreFeature::class
    single(named("auth")) { AuthCoreFeature() } bind CoreFeature::class
    single(named("billing")) { BillingCoreFeature() } bind CoreFeature::class
    single(named("platform")) { PlatformCoreFeature() } bind CoreFeature::class
    single(named("workspace")) { WorkspaceActiveFeature() } bind CoreFeature::class
    single(named("ws_recovery")) { WorkspaceGateFeature() } bind CoreFeature::class
    single(named("customerState")) { CustomerCoreFeature() } bind CoreFeature::class
    single(named("user_management")) { TenantUserManagementCoreFeature() } bind CoreFeature::class
    single(named("support")) { SupportCoreFeature() } bind CoreFeature::class
    single(named("settings_feature")) { SettingsFeature() } bind CoreFeature::class
    single(named("device_feature")) { com.citacomsys.module.device.DeviceCoreFeature() } bind CoreFeature::class

    includes(
        workspaceFeatureModule,
        backupFeatureModule,
        settingsFeatureModule,
        authFeatureModule,
        organizationFeatureModule,
        platformFeatureModule,
        homeFeatureModule,
        telemetryFeatureModuleDi,
        supportFeatureModule
    )
}


@OptIn(ExperimentalSerializationApi::class)
val appSharedModule = module {
    single<AppConfig> { TenantAppConfig() }
    single<AppManifest> { DynamicAppManifest() }

    single {
        GlobalNavigationCoordinator(
            manifest = get(),
            scope = get(),
            // Ensuring the core Serializers module is always present
            baseSerializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    subclassesOfSealed<OrganizationFeatureNav>()
                    subclassesOfSealed<WorkspaceNavKey>()
                    subclassesOfSealed<SupportNavKey>()
                    subclassesOfSealed<PosModuleNavKey>()
                    subclassesOfSealed<SalesModuleNavKey>()
                    subclass(AppSettingsNav::class, AppSettingsNav.serializer())
                }
            },
            capsManager = get()
        )
    } bind NavigationCoordinator::class

    single<CoroutineDispatcher>(named("IO")) {
        iODispatcher()
    }

    single<UserCapabilityManager> {
        TenantUserCapabilityManager(
            sessionManager = get(),
            appManifest = get()
        )
    }

    singleOf(::AppStageOrchestrator)
    viewModelOf(::AppViewModel)
    viewModelOf(::SyncManagerViewModel)

    single {
        WorkspaceModulePolicyManager(
            runtimeService = get(),
            notificationService = get(),
            manifest = get(),
            koin = get(),
            scope = get(),
            syncEngine = get()
        )
    }

    singleOf(::WorkspaceInfraConfigurationManager) bind StartupTask::class
}
