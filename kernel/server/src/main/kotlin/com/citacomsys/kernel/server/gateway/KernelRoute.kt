package com.citacomsys.kernel.server.gateway

import com.citacomsys.core.persistence.plugin.transactionManager
import com.citacomsys.core.security.plugin.Authorize
import com.citacomsys.core.security.plugin.SecurityScope
import com.citacomsys.core.security.plugin.resolveHardwareId
import com.citacomsys.core.security.plugin.resolveOrganizationId
import com.citacomsys.core.security.plugin.resolvedWorkspaceId
import com.citacomsys.core.security.toAuditRequestContext
import com.citacomsys.kernel.server.PlatformRuntimeService
import com.citacomsys.types.WorkspaceClaimRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.request.receive
import mu.KotlinLogging
import org.koin.ktor.ext.inject

private val logger = KotlinLogging.logger {}

fun Route.kernelRoute() {
    val service: PlatformRuntimeService by inject()

    route("api/v1/kernel") {
        install(Authorize) {
            scope = SecurityScope.ORGANIZATION
        }

        authenticate("access-auth", "device-access-auth") {
            /**
             * Retrieves the organization-wide configuration for the authenticated user.
             * Used by the Organization Hub.
             */
            get("/organization/pkg") {
                val audit = call.toAuditRequestContext()
                val organizationId = call.resolveOrganizationId
                val clientEtag = call.request.headers["If-None-Match"]
                val currentEtag = service.getOrgETag(organizationId)

                if (!clientEtag.isNullOrBlank() && clientEtag == currentEtag) {
                    logger.debug { "Kernel audit [${audit.requestId}]: Org config 304 Not Modified for $organizationId" }
                    return@get call.respond(HttpStatusCode.NotModified)
                }

                logger.debug { "Kernel audit [${audit.requestId}]: Fetching org config for $organizationId" }
                val config = service.getOrganizationConfig(organizationId)
                call.response.headers.append("ETag", currentEtag)
                call.respond(config)
            }

            /**
             * Directly binds a device to a workspace for an authenticated user.
             * Bypasses OTP if the user has authority over the organization.
             */
            post("/workspace/claim") {
                val audit = call.toAuditRequestContext()
//                val principal = call.userPrincipal()
                val organizationId = call.resolveOrganizationId

                val request = call.receive<WorkspaceClaimRequest>()
                val deviceId = call.resolveHardwareId()

                logger.info { "Kernel audit [${audit.requestId}]: Claiming workspace ${request.workspaceId} for device $deviceId" }

                val workspaceId = request.workspaceId
                val deviceName = request.deviceName

                val response = call.transactionManager.runInTransaction {
                    service.claimWorkspace(
                        workspaceId = workspaceId,
                        deviceId = deviceId,
                        organizationId = organizationId,
                        deviceName = deviceName,
                    )
                }

                call.respond(response)
            }


            /**
             * Retrieves the active workspace configuration via device-auth (enabling background sync & runtime polling without user login).
             */
            get("/workspace/profile/refresh") {
                val audit = call.toAuditRequestContext()
                val workspaceId = call.resolvedWorkspaceId
                val deviceId = call.resolveHardwareId()
                val clientEtag = call.request.headers["If-None-Match"]
                val currentEtag = service.getWorkspaceETag(workspaceId)

                if (!clientEtag.isNullOrBlank() && clientEtag == currentEtag) {
                    logger.debug { "Kernel device sync [${audit.requestId}]: Workspace profile 304 Not Modified for $workspaceId" }
                    return@get call.respond(HttpStatusCode.NotModified)
                }

                logger.debug { "Kernel device sync [${audit.requestId}]: Fetching config for workspace $workspaceId" }

                val config = call.transactionManager.runReadOnly {
                    service.getConfig(workspaceId = workspaceId)
                }

                logger.debug { "Kernel device sync [${audit.requestId}]: Fetching runtime state for workspace $workspaceId" }
                val state = call.transactionManager.runReadOnly {
                    service.getRuntimeState(
                        workspaceId = workspaceId,
                        deviceId = deviceId
                    )
                }

                call.response.headers.append("ETag", currentEtag)
                call.respond(Pair(config, state))
            }
        }

        authenticate("device-access-auth") {
            /**
             * Retrieves the active workspace configuration via device-auth (enabling background sync & runtime polling without user login).
             */
            get("/workspace/pkg") {
                val audit = call.toAuditRequestContext()
                val workspaceId = call.resolvedWorkspaceId
                val clientEtag = call.request.headers["If-None-Match"]
                val currentEtag = service.getWorkspaceETag(workspaceId)

                if (!clientEtag.isNullOrBlank() && clientEtag == currentEtag) {
                    logger.debug { "Kernel device sync [${audit.requestId}]: Workspace pkg 304 Not Modified for $workspaceId" }
                    return@get call.respond(HttpStatusCode.NotModified)
                }

                logger.debug { "Kernel device sync [${audit.requestId}]: Fetching config for workspace $workspaceId" }

                val config = call.transactionManager.runReadOnly {
                    service.getConfig(workspaceId = workspaceId)
                }

                call.response.headers.append("ETag", currentEtag)
                call.respond(config)
            }

            /**
             * Retrieves the runtime security and enforcement state.
             */
            get("/workspace/state") {
                val audit = call.toAuditRequestContext()
                val workspaceId = call.resolvedWorkspaceId
                val deviceId = call.resolveHardwareId()

                logger.debug { "Kernel device sync [${audit.requestId}]: Fetching runtime state for workspace $workspaceId" }

                val state = call.transactionManager.runReadOnly {
                    service.getRuntimeState(
                        workspaceId = workspaceId,
                        deviceId = deviceId
                    )
                }

                call.respond(state)
            }

            /**
             * Retrieves the module enablement policy.
             */
            get("/workspace/modules/policy") {
                val audit = call.toAuditRequestContext()
                val workspaceId = call.resolvedWorkspaceId
                val version = call.queryParameters["version"]?.toLongOrNull()

                logger.debug { "Kernel device sync [${audit.requestId}]: Fetching module policy for workspace $workspaceId" }

                val policy = call.transactionManager.runReadOnly {
                    service.getModulePolicyState(workspaceId)
                }

                if (version != policy.version) {
                    call.respond(policy)
                } else {
                    call.respond(HttpStatusCode.NotModified)
                }
            }
        }
    }
}
