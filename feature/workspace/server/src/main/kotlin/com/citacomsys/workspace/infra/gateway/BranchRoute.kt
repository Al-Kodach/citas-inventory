package com.citacomsys.workspace.infra.gateway

import com.citacomsys.api.workspace.branch.Branch
import com.citacomsys.api.workspace.service.BranchService
import com.citacomsys.types.UserCapability
import com.citacomsys.core.security.plugin.Authorize
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.feature.api.telemetry.TelemetryLevel
import com.citacomsys.core.logging.logApplicationEvent
import com.citacomsys.core.security.plugin.SecurityScope
import com.citacomsys.types.EntityType
import io.ktor.server.request.requirePathParameter
import org.koin.ktor.ext.inject

fun Route.branchRoute() {
    val branchService: BranchService by inject()
    val auditor: TelemetryAuditor by inject()

    route("api/v1/workspaces/{workspaceId}/branches") {
        install(Authorize) {
            scope = SecurityScope.ORGANIZATION
            requireCapabilities =
                setOf(UserCapability.MANAGE_BRANCHES, UserCapability.MANAGE_WORKSPACES)
        }

        authenticate("access-auth") {
            get {
                val workspaceId = call.requirePathParameter("workspaceId")
                val branches = branchService.getAll(workspaceId)
                call.respond(branches)
            }

            post {
                val request = call.receive<Branch>()
                val workspaceId = call.requirePathParameter("workspaceId")

                val branch = branchService.save(
                    workspaceId = workspaceId,
                    branch = request
                )

                call.respond(branch)
            }

            route("/{branchId}") {
                get {
                    val workspaceId = call.requirePathParameter("workspaceId")
                    val id = call.requirePathParameter("branchId")
                    val branch = branchService.get(workspaceId, id)
                    call.respond(branch)
                }

                delete {
                    val workspaceId = call.requirePathParameter("workspaceId")
                    val id = call.requirePathParameter("branchId")

                    branchService.delete(workspaceId, id)
                    auditor.logApplicationEvent(
                        action = "BRANCH_DELETED",
                        entityType = EntityType.TENANT.name,
                        entityId = id,
                        summary = "Branch location removed from workspace",
                        level = TelemetryLevel.WARNING
                    )
                    call.respond(io.ktor.http.HttpStatusCode.NoContent)
                }
            }
        }
    }
}

