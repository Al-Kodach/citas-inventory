package com.citacomsys.workspace.infra.gateway

import com.citacomsys.api.workspace.repository.WorkspaceRepository
import com.citacomsys.api.workspace.repository.WorkspaceResourceUsageRepository
import com.citacomsys.api.workspace.service.WorkspaceSubscriptionService
import com.citacomsys.core.security.plugin.Authorize
import com.citacomsys.core.security.plugin.resolvedWorkspaceId
import com.citacomsys.types.BrandingConfig
import com.citacomsys.types.UserCapability
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.workspaceRoute() {
    val repository: WorkspaceRepository by inject()
    val resRepository: WorkspaceResourceUsageRepository by inject()
    val subscriptionService: WorkspaceSubscriptionService by inject()

    route("api/v1/workspace") {
        authenticate("access-auth") {
            install(Authorize) {
                requireCapabilities = setOf(UserCapability.MANAGE_SETTINGS)
            }

            get("/metrics") {
                val workspaceId = call.resolvedWorkspaceId
                val usage = resRepository.getResourceUsage(workspaceId)
                call.respond(usage)
            }

            get("/pkg") {
                val workspaceId = call.resolvedWorkspaceId
                val pkg = resRepository.getWorkspaceResourceAllocation(workspaceId)
                    ?: error("Workspace not found")
                call.respond(pkg)
            }

            get("/subscription") {
                val workspaceId = call.resolvedWorkspaceId
                val subscription = repository.getSubscription(workspaceId)
                call.respond(subscription)
            }

            get("/subscription/summary") {
                val workspaceId = call.resolvedWorkspaceId
                val subscription = subscriptionService.getSubscriptionSummary(workspaceId)
                call.respond(subscription)
            }

            put("/identity") {
                val workspaceId = call.resolvedWorkspaceId
                val body = call.receive<Map<String, String?>>()

                val name = body["name"]
                val email = body["email"]
                val phone = body["phone"]

                repository.updateIdentity(workspaceId, name, email, phone)
                call.respond(mapOf("success" to true))
            }

            route("/branding") {
                delete {
                    val workspaceId = call.resolvedWorkspaceId
                    repository.deleteBranding(workspaceId)
                    call.respond(mapOf("success" to true))
                }

                post {
                    val workspaceId = call.resolvedWorkspaceId
                    val body = call.receive<BrandingConfig>()
                    repository.saveBrand(workspaceId, body)
                    call.respond(mapOf("success" to true))
                }
            }
        }
    }
}
