package com.citacomsys.inventory.admin

import com.citacomsys.auth.session.AuthenticationOrchestrator
import com.citacomsys.core.security.data.AppReleaseRepository
import com.citacomsys.core.security.service.SecurityAlertService
import com.citacomsys.core.security.service.SecurityThrottlerService
import com.citacomsys.core.storage.ArtifactStorageProvider
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.inventory.admin.auth.AdminAuthController
import com.citacomsys.inventory.admin.auth.AdminLoginService
import com.citacomsys.inventory.admin.auth.adminAuthRoute
import com.citacomsys.inventory.admin.billing.gateway.AdminBillingController
import com.citacomsys.inventory.admin.billing.gateway.adminBillingRoute
import com.citacomsys.inventory.admin.dashboard.AdminDashboardService
import com.citacomsys.inventory.admin.dashboard.adminDashboardRoute
import com.citacomsys.inventory.admin.device.AdminDeviceService
import com.citacomsys.inventory.admin.device.deviceRoute
import com.citacomsys.inventory.admin.routes.configureAdminReleaseManagementEndpoints
import com.citacomsys.inventory.admin.system.AdminSystemService
import com.citacomsys.inventory.admin.system.adminSystemRoute
import com.citacomsys.inventory.admin.tenants.AdminTenantService
import com.citacomsys.inventory.admin.tenants.adminTenantRoute
import com.citacomsys.inventory.admin.tenants.publicTenantRoute
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.core.context.loadKoinModules
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.ktor.ext.inject

val adminServiceModule = module {
    single {
        AdminLoginService(
            adminRepository = get(),
            passwordHasher = get(),
            mfaService = get(),
            sessionIssuer = get<AuthenticationOrchestrator>(),
            auditor = get(),
            securityThrottler = get<SecurityThrottlerService>()
        )
    }
    single {
        AdminAuthController(
            adminLoginService = get(),
            refreshTokenUseCase = get()
        )
    }

    singleOf(::AdminBillingController)
    singleOf(::AdminTenantService)
    singleOf(::AdminDeviceService)
    single { 
        AdminSystemService(
            packageRepo = get(),
            smsPackageRepo = get(),
            campaignRepo = get(),
            roleRepository = get(),
            vault = get(),
            auditor = get()
        )
    }
    singleOf(::AdminDashboardService)
}

fun Application.configureAdminApp() {
    loadKoinModules(adminServiceModule)

    routing {
        adminAuthRoute()
        publicTenantRoute(inject<AdminTenantService>().value)

        route("api/v1/admin") {
            authenticate("admin-access-auth") {
                // Lazy evaluations inside endpoints stop startup race conditions
                adminTenantRoute(inject<AdminTenantService>().value)
                adminDashboardRoute(inject<AdminDashboardService>().value)
                adminBillingRoute(
                    inject<TelemetryAuditor>().value,
                    inject<SecurityAlertService>().value,
                    inject<AdminBillingController>().value
                )
                deviceRoute(inject<AdminDeviceService>().value)
                adminSystemRoute(inject<AdminSystemService>().value)

                // Platform & Infrastructure Management
                route("platform") {
                    configureAdminReleaseManagementEndpoints(
                        releaseRepository = inject<AppReleaseRepository>().value,
                        auditor = inject<TelemetryAuditor>().value,
                        storageProvider = inject<ArtifactStorageProvider>().value
                    )
                }
            }
        }
    }
}
