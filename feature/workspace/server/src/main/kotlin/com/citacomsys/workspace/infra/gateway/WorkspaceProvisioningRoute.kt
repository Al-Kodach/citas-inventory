package com.citacomsys.workspace.infra.gateway

import com.citacomsys.api.platform.model.PkgProvisionRequest
import com.citacomsys.api.workspace.model.WorkspaceRegistrationRequest
import com.citacomsys.api.workspace.service.WorkspaceProvisioningService
import com.citacomsys.core.persistence.plugin.transactionManager
import com.citacomsys.core.security.plugin.Authorize
import com.citacomsys.core.security.plugin.SecurityContextKey
import com.citacomsys.core.security.plugin.SecurityScope
import com.citacomsys.types.UserCapability
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.workspaceProvisioningRoute() {
    val service: WorkspaceProvisioningService by inject()

    route("api/v1/workspace/provisioning") {
        install(Authorize) {
            scope = SecurityScope.ORGANIZATION
            requireCapabilities = setOf(
                UserCapability.MANAGE_ORGANIZATIONS,
                UserCapability.MANAGE_WORKSPACES
            )
        }

        authenticate("access-auth") {
            post("/save") {
                val secCtx = call.attributes[SecurityContextKey]
                val request = call.receive<WorkspaceRegistrationRequest>()

                println("Security context-orgId: $secCtx")

                val workspaceId = call.transactionManager.runInTransaction {
                    service.provisionWorkspace(
                        secCtx.organizationId,
                        request = request,
                        actorId = secCtx.actorId
                    )
                }
                call.respond(workspaceId)
            }

            post("/invoice-preview") {
                val request = call.receive<PkgProvisionRequest>()
                val preview = service.previewInvoice(request)
                call.respond(preview)
            }
        }
    }
}
