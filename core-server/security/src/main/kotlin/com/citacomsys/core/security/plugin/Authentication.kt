package com.citacomsys.core.security.plugin

import com.auth0.jwt.JWTVerifier
import com.citacomsys.core.security.token.jwt.*
import com.citacomsys.inventory.api.user.PlatformAdminRepository
import com.citacomsys.types.DeploymentMode
import com.citacomsys.types.PlatformAdminRole
import com.citacomsys.types.UserStatus
import io.ktor.http.HttpStatusCode
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.request.header
import io.ktor.server.response.respond
import mu.KotlinLogging
import org.koin.ktor.ext.inject
import kotlin.error

private val logger = KotlinLogging.logger { }

data class OrgSecurityConfig(
    val organizationId: String,
    val orgStatus: String,
    val activeWorkspaceIds: Set<String> = emptySet(),
    val cloudSyncedWorkspaceIds: Set<String> = emptySet()
) {
    fun isWorkspaceCloudEnabled(workspaceId: String): Boolean =
        cloudSyncedWorkspaceIds.contains(workspaceId)

    fun isWorkspaceActive(workspaceId: String): Boolean =
        activeWorkspaceIds.contains(workspaceId)
}

internal fun Application.configureAuthentication(
    accessVerifier: JWTVerifier,
    refreshVerifier: JWTVerifier,
    mapper: JwtPrincipalMapper
) {
    install(Authentication) {
        /**
         * High-Fidelity Industrial Authentication (NIST SP 800-53 IA-2 / IA-5):
         * Uses asymmetric cryptographic signatures to authorize physical hardware nodes
         * as trusted industrial workstations and hubs.
         */
        register(
            LoginSignatureAuthenticationProvider(
                BaseWorkstationAuthProvider.Config("device-auth")
            )
        )
        register(
            WorkstationSignatureAuthenticationProvider(
                BaseWorkstationAuthProvider.Config("device-access-auth")
            )
        )
        register(
            LoginSignatureAuthenticationProvider(
                BaseWorkstationAuthProvider.Config("device-login-auth")
            )
        )

        jwt("action-auth") {
            verifier(accessVerifier)

            authHeader { call ->
                val rawHeader = call.request.header("X-Action-Token") ?: return@authHeader null
                val token = rawHeader.split(';', ',').firstOrNull()?.trim()

                token?.let {
                    HttpAuthHeader.Single("Bearer", it)
                }
            }

            validate { credential ->
                val principal = mapper.from(credential.payload)

                if (principal is ActionPrincipal) {
                    principal
                } else {
                    logger.warn("Invalid Action Principal: $principal")
                    null
                }
            }

            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    "A valid action token is required for this action"
                )
            }
        }

        jwt("access-auth") {
            verifier(accessVerifier)

            validate { credential ->
                val principal = mapper.from(credential.payload)

                if (
                    principal is UserPrincipal &&
                    principal.tokenType == TokenType.ACCESS
                ) {
                    principal
                } else {
                    null
                }
            }


            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, "Your session has expired, please log in")
            }
        }

        jwt("admin-access-auth") {
            verifier(accessVerifier)

            validate { credential ->
                val principal = mapper.from(credential.payload)
                if (
                    principal is UserPrincipal &&
                    principal.tokenType == TokenType.ACCESS
                ) {
                    val normalizedRole = principal.role.removePrefix("PLATFORM_")
                    val adminRole = runCatching { PlatformAdminRole.valueOf(normalizedRole) }.getOrNull()
                    if (adminRole == null) {
                        logger.warn { "Invalid platform admin role during validation: ${principal.role}" }
                        return@validate null
                    }

                    val adminRepo: PlatformAdminRepository by application.inject()
                    val admin = adminRepo.findById(principal.userId).getOrNull()
                    if (admin == null || admin.status != UserStatus.ACTIVE) {
                        return@validate null
                    }

                    val incomingHardwareId: String? = request.header("X-Hardware-Id")
                    if (admin.activeHardwareId != null && incomingHardwareId != null && admin.activeHardwareId != incomingHardwareId) {
                        logger.warn { "Admin device mismatch: Admin ${admin.username} bound to ${admin.activeHardwareId}, incoming: $incomingHardwareId" }
                        return@validate null
                    }

                    principal
                } else {
                    null
                }
            }

            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    "Admin session invalid, suspended, or device locked. Please log in again."
                )
            }
        }

        jwt("refresh-auth") {
            verifier(refreshVerifier)

            validate { credential ->
                val principal = mapper.from(credential.payload)

                if (
                    principal is UserPrincipal &&
                    principal.tokenType == TokenType.REFRESH
                ) {
                    principal
                } else {
                    null
                }
            }
        }
    }
}

val ApplicationCall.securityContext: SecurityContext?
    get() = attributes.getOrNull(SecurityContextKey)

fun ApplicationCall.userPrincipal(): UserPrincipal {
    return principal<UserPrincipal>()
        ?: error("Missing user principal")
}

val ApplicationCall.resolveOrganizationId: String
    get() = securityContext?.organizationId
        ?: request.header("X-Organization-Id")
        ?: principal<UserPrincipal>()?.organizationId
        ?: error("No valid organization ID found")

val ApplicationCall.filterBranchId: String?
    get() = request.header("X-View-Branch-Id")

fun ApplicationCall.resolveHardwareId(): String {
    return securityContext?.hardwareId ?: request.header("X-Hardware-Id")
    ?: error("No valid hardware ID found")
}

fun ApplicationCall.actionPrincipal(): ActionPrincipal {
    return principal<ActionPrincipal>()
        ?: error("Missing action principal")
}

val ApplicationCall.resolvedWorkspaceId: String
    get() = securityContext?.workspaceId
        ?: request.header("X-Workspace-Id")
        ?: error("Workspace ID required")

val ApplicationCall.auditActorId: String
    get() = securityContext?.actorId
        ?: userPrincipal().userId
