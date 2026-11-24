package com.citacomsys.feature.telemetry.server

import com.citacomsys.core.security.data.AppReleaseRepository
import com.citacomsys.core.security.plugin.resolvedWorkspaceId
import com.citacomsys.core.security.service.SecurityAlertService
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.feature.api.telemetry.SystemEventService
import com.citacomsys.feature.telemetry.server.repository.ExposedSystemEventService
import com.citacomsys.feature.telemetry.server.repository.SiemTraceRepository
import com.citacomsys.feature.telemetry.server.repository.WorkspaceAuditLogRepository
import com.citacomsys.feature.telemetry.server.routes.appUpdateRoute
import com.citacomsys.feature.telemetry.server.routes.configureAdminSiemRoutes
import com.citacomsys.feature.telemetry.server.routes.configureSecurityAlertsEndpoint
import com.citacomsys.feature.telemetry.server.routes.configureSiemAnalyticsRoutes
import com.citacomsys.feature.telemetry.server.routes.configureSiemIngestionEndpoint
import com.citacomsys.shared.event.EntityChangeEvent
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import org.koin.dsl.module
import org.koin.ktor.plugin.koin

val telemetryServiceModule = module {
    single { SiemTraceRepository() }
    single { WorkspaceAuditLogRepository() }
    single<TelemetryAuditor> { SystemAuditLoggerImpl() }
    single<SystemEventService<EntityChangeEvent>> { ExposedSystemEventService() }
}

fun Application.configureTelemetryRoutes() {

    routing {
        appUpdateRoute(koin().get<AppReleaseRepository>())

        route("api/v1/security") {
            authenticate("access-auth") {
                get("/audit-feed") {
                    val repo = koin().get<WorkspaceAuditLogRepository>()
                    val workspaceId = call.resolvedWorkspaceId
                    val branchId = call.request.queryParameters["branchId"]
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50

                    val feed = repo.getWorkspaceAuditFeed(workspaceId, branchId, limit)
                    call.respond(feed)
                }
            }
        }

        route("api/v1/admin/") {
            authenticate("admin-access-auth") {
                configureSiemAnalyticsRoutes()
                configureAdminSiemRoutes()
            }
        }

        authenticate("device-access-auth") {
            configureSecurityAlertsEndpoint(
                auditor = koin().get<TelemetryAuditor>(),
                alertService = koin().get<SecurityAlertService>()
            )
            configureSiemIngestionEndpoint()
        }
    }
}
