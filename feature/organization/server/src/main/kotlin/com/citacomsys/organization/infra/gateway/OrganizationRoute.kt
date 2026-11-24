package com.citacomsys.organization.infra.gateway

import com.citacomsys.api.organization.OrganizationRepository
import com.citacomsys.api.organization.OrganizationService
import com.citacomsys.core.security.plugin.Authorize
import com.citacomsys.core.security.plugin.resolveOrganizationId
import com.citacomsys.feature.telemetry.server.repository.WorkspaceAuditLogRepository
import com.citacomsys.organization.service.OrganizationReportService
import com.citacomsys.types.BrandingConfig
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.receive
import io.ktor.server.request.requirePathParameter
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.organizationRoute() {
    val repository: OrganizationRepository by inject()
    val organizationService: OrganizationService by inject()
    val reportService: OrganizationReportService by inject()
    val auditRepo: WorkspaceAuditLogRepository by inject()

    route("api/v1/organization") {
        authenticate("access-auth", "device-access-auth") {
            install(Authorize)

            get("/pkg") {
                val config = repository.getRuntimeConfig(call.resolveOrganizationId)
                call.respond(config)
            }

            get("/dashboard") {
                val organizationId = call.resolveOrganizationId
                val summary = reportService.getSummary(organizationId)
                call.respond(summary)
            }

            get("/reports/summary") {
                val organizationId = call.resolveOrganizationId
                val summary = reportService.getSummary(organizationId)
                call.respond(summary)
            }

            get("/security/siem-feed") {
                val organizationId = call.resolveOrganizationId
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
                val feed = auditRepo.getOrganizationAuditFeed(organizationId, limit)
                call.respond(feed)
            }

            get("/monitoring/audit-feed") {
                val organizationId = call.resolveOrganizationId
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                val feed = auditRepo.getOrganizationAuditFeed(organizationId, limit)
                call.respond(feed)
            }

            get("/workspace/{activeWorkspaceId}/details") {
                val workspaceId = call.requirePathParameter("activeWorkspaceId")
                val organizationId = call.resolveOrganizationId
                val details = repository.getWorkspaceDetails(workspaceId, organizationId)
                call.respond(details)
            }

            get("/bindings") {
                val orgId = call.resolveOrganizationId
                val bindings = repository.getWorkstationBindings(orgId)
                call.respond(bindings)
            }

            delete("/bindings/{bindingId}") {
                val bindingId = call.parameters["bindingId"] ?: error("Missing bindingId")
                repository.revokeWorkstationBinding(bindingId)
                call.respond(HttpStatusCode.OK)
            }

            get("/sessions") {
                val orgId = call.resolveOrganizationId
                val sessions = organizationService.getUserSessions(orgId).getOrThrow()
                call.respond(sessions)
            }

            delete("/sessions/{sessionId}") {
                val sessionId = call.parameters["sessionId"] ?: error("Missing sessionId")
                val reason = call.request.queryParameters["reason"]
                organizationService.revokeUserSession(sessionId, reason).getOrThrow()
                call.respond(HttpStatusCode.OK)
            }

            route("/branding") {
                post {
                    val orgId = call.resolveOrganizationId
                    val config = call.receive<BrandingConfig>()
                    organizationService.saveBranding(orgId, config)
                    call.respond(HttpStatusCode.OK)
                }

                delete {
                    val orgId = call.resolveOrganizationId
                    organizationService.deleteBrand(orgId)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}
