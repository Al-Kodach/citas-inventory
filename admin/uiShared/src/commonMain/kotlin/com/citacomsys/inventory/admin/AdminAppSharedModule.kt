package com.citacomsys.inventory.admin

import androidx.navigation3.runtime.NavKey
import com.citacomsys.admin.api.auth.AdminAuthService
import com.citacomsys.admin.api.device.AdminDeviceApiService
import com.citacomsys.core.spi.PlatformFeature
import com.citacomsys.core.spi.CoreFeature
import com.citacomsys.core.domain.security.UserCapabilityManager
import com.citacomsys.core.spi.GlobalNavigationCoordinator
import com.citacomsys.inventory.admin.app.AdminCoreFeatures
import com.citacomsys.inventory.admin.app.AdminDefaultAppViewModel
import com.citacomsys.inventory.admin.app.AdminSystemEventService
import com.citacomsys.inventory.admin.appRelease.AdminReleaseManagementViewModel
import com.citacomsys.inventory.admin.appRelease.AdminReleaseUploadViewModel
import com.citacomsys.admin.api.dashboard.AdminDashboardApiService
import com.citacomsys.backup.api.nav.CreateBackupNav
import com.citacomsys.backup.api.nav.RestoreBackupNav
import com.citacomsys.core.domain.security.ActionConfirmationManager
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.security.sudo.ActorSudoAuthHandler
import com.citacomsys.core.spi.AppManifest
import com.citacomsys.core.spi.BackupNav
import com.citacomsys.core.spi.DynamicAppManifest
import com.citacomsys.core.spi.NavigationCoordinator
import com.citacomsys.core.spi.StartupTask
import com.citacomsys.core.spi.TelemetryNav
import com.citacomsys.core.spi.TenantSecurityNav
import com.citacomsys.core.sync.engine.EntitySyncEngine
import com.citacomsys.core.sync.telemetry.TelemetryBackgroundScheduler
import com.citacomsys.inventory.admin.app.AdminAppSessionManager
import com.citacomsys.inventory.admin.app.AdminAppStageOrchestrator
import com.citacomsys.inventory.admin.app.AdminDefaultACTnManager
import com.citacomsys.inventory.admin.app.AdminTelemetryBackgroundScheduler
import com.citacomsys.inventory.admin.app.NoEntitySyncEngine
import com.citacomsys.inventory.admin.app.nav.AdminDashboardNavKey
import com.citacomsys.inventory.admin.app.nav.AdminSettingsNavKey
import com.citacomsys.inventory.admin.app.nav.AdminSystemModuleNavKey
import com.citacomsys.feature.support.api.navigation.SubmitTicketNavKey
import com.citacomsys.feature.support.api.navigation.SupportHubNavKey
import com.citacomsys.feature.support.api.navigation.SupportTicketDetailNavKey
import com.citacomsys.feature.support.client.di.supportFeatureModule
import com.citacomsys.feature.support.client.ui.SupportCoreFeature
import com.citacomsys.inventory.admin.app.nav.AdminUserProfileNav
import com.citacomsys.inventory.admin.app.dashboard.presentation.AdminDashboardViewModel
import com.citacomsys.inventory.admin.app.dashboard.remote.api.KtorAdminDashboardApiService
import com.citacomsys.inventory.admin.system.presentation.capability.CapabilityMatrixViewModel
import com.citacomsys.inventory.admin.auth.AdminAuthViewModel
import com.citacomsys.inventory.admin.auth.remote.api.KtorAdminAuthApiService
import com.citacomsys.inventory.admin.billing.AdminBillingPlatformFeature
import com.citacomsys.inventory.admin.device.presentation.DeviceListViewModel
import com.citacomsys.inventory.admin.device.remote.AdminKtorDeviceApiService
import com.citacomsys.inventory.admin.security.SecurityModuleNavPlatform
import com.citacomsys.inventory.admin.security.hub.LiveSecurityActivityViewModel
import com.citacomsys.inventory.admin.system.SystemModuleNavPlatform
import com.citacomsys.inventory.admin.tenant.TenantModulePlatform
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

expect fun platformModule(): Module

private val adminDeviceSharedModule = module {
    includes(platformModule())
    single<AdminDeviceApiService> { AdminKtorDeviceApiService(get()) }
    viewModelOf(::AdminReleaseManagementViewModel)
    viewModelOf(::DeviceListViewModel)
    viewModel {
        AdminReleaseUploadViewModel(
            notification = get(),
            httpClient = get(),
            vault = get(named("vault"))
        )
    }
    factory {
        ActorSudoAuthHandler(get(), get(), get(), get())
    }
}

val adminFeatureModule = module {
    singleOf(::AdminBillingPlatformFeature) {
        bind<PlatformFeature>()
    }
    singleOf(::SystemModuleNavPlatform) {
        bind<PlatformFeature>()
    }
    singleOf(::SecurityModuleNavPlatform) {
        bind<PlatformFeature>()
    }
    singleOf(::AdminCoreFeatures) {
        bind<CoreFeature>()
    }
    singleOf(::TenantModulePlatform) {
        bind<PlatformFeature>()
    }
    single(named("support")) { SupportCoreFeature() } bind CoreFeature::class

    includes(supportFeatureModule)
}

val adminAppSharedModule = module {
    single<AdminAuthService> {
        KtorAdminAuthApiService(client = get(named("authClient")))
    }

    single<UserCapabilityManager> {
        AdminUserCapabilityManager(get(named("vault")), get())
    }
    single<EntitySyncEngine> {
        NoEntitySyncEngine()
    }

    single<AppManifest> { DynamicAppManifest() }
    single<ActionConfirmationManager> { AdminDefaultACTnManager(get(), get()) }

    single {
        GlobalNavigationCoordinator(
            manifest = get(),
            scope = get(),
            // Ensuring the core Serializers module is always present
            baseSerializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(AdminDashboardNavKey::class, AdminDashboardNavKey.serializer())
                    subclass(AdminSettingsNavKey::class, AdminSettingsNavKey.serializer())
                    subclass(AdminUserProfileNav::class, AdminUserProfileNav.serializer())
                    subclass(AdminSystemModuleNavKey::class, AdminSystemModuleNavKey.serializer())
                    subclass(TelemetryNav::class, TelemetryNav.serializer())
                    subclass(TenantSecurityNav::class, TenantSecurityNav.serializer())
                    subclass(BackupNav::class, BackupNav.serializer())
                    subclass(CreateBackupNav::class, CreateBackupNav.serializer())
                    subclass(RestoreBackupNav::class, RestoreBackupNav.serializer())
                    subclass(SupportHubNavKey::class, SupportHubNavKey.serializer())
                    subclass(SubmitTicketNavKey::class, SubmitTicketNavKey.serializer())
                    subclass(SupportTicketDetailNavKey::class, SupportTicketDetailNavKey.serializer())
                }
            },
            capsManager = get()
        )
    } bind NavigationCoordinator::class

    single<AppSessionManager> {
        AdminAppSessionManager(get(), get())
    }

    includes(
        adminFeatureModule,
        adminDeviceSharedModule,
    )
    singleOf(::AdminAppStageOrchestrator)
    singleOf(::AdminSystemEventService)
    singleOf(::AdminSystemEventService) bind StartupTask::class

    viewModelOf(::AdminDefaultAppViewModel)
    viewModelOf(::AdminAuthViewModel)
    viewModelOf(::CapabilityMatrixViewModel)
    viewModelOf(::AdminDashboardViewModel)
    viewModelOf(::LiveSecurityActivityViewModel)

    single<AdminDashboardApiService> {
        KtorAdminDashboardApiService(get())
    }

    singleOf(::AdminTelemetryBackgroundScheduler) bind TelemetryBackgroundScheduler::class
}
