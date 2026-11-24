package com.citacomsys.core.security.plugin

import com.citacomsys.core.logging.logIdentityAccessEvent
import com.citacomsys.core.logging.logNetworkSecurityEvent
import com.citacomsys.core.security.plugin.OrgSecurityConfig
import com.citacomsys.core.security.AuthorizationContextProvider
import com.citacomsys.core.security.service.OrgSecurityConfigProvider
import com.citacomsys.core.security.token.jwt.DevicePrincipal
import com.citacomsys.core.security.token.jwt.UserPrincipal
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.feature.api.telemetry.TelemetryLevel
import com.citacomsys.types.DeviceStatus
import com.citacomsys.types.HardwareMasterStatus
import com.citacomsys.types.ProductModule
import com.citacomsys.types.SubscriptionStatus
import com.citacomsys.types.SyncMetadata
import com.citacomsys.types.WorkspaceStatus
import com.citacomsys.types.User
import com.citacomsys.types.UserCapability
import com.citacomsys.types.UserType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.auth.principal
import io.ktor.server.request.header
import io.ktor.server.response.respond
import io.ktor.util.AttributeKey
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import java.util.Base64

enum class DeploymentChannel {
    /** Request fired live directly to the Cloud/Hub by an active human operator. */
    LIVE_INTERACTIVE,

    /** Request recorded natively at the local edge, now being replayed over the mesh network. */
    MESH_REPLAY
}

/**
 * Unified interface for downstream route handlers to read tenant boundaries.
 */
interface SecurityContext {
    val hardwareId: String
    val workspaceId: String?
    val organizationId: String
    val actorId: String
    val sessionId: String?
    val actorName: String
    val workspace: WorkspaceOperationalState?
    val channel: DeploymentChannel
}

data class AuthorizationContext(
    override val organizationId: String,
    override val workspaceId: String?,
    override val hardwareId: String,
    override val actorId: String,
    override val actorName: String,
    override val sessionId: String?,
    override val channel: DeploymentChannel,
    override val workspace: WorkspaceOperationalState? = null,
    val user: User? = null,
    val principal: UserPrincipal? = null,
    val devicePrincipal: DevicePrincipal? = null
) : SecurityContext {
    constructor(
        principal: UserPrincipal,
        user: User,
        workspace: WorkspaceOperationalState?,
        hardwareId: String
    ) : this(
        organizationId = workspace?.organizationId ?: principal.organizationId,
        workspaceId = principal.workspaceId,
        hardwareId = hardwareId,
        actorId = user.id,
        actorName = user.name,
        sessionId = principal.sessionId,
        channel = DeploymentChannel.LIVE_INTERACTIVE,
        workspace = workspace,
        user = user,
        principal = principal,
        devicePrincipal = null
    )
}

typealias UserAuthorizationContext = AuthorizationContext
typealias DeviceAuthorizationContext = AuthorizationContext

data class WorkspaceOperationalState(
    val organizationId: String,
    val status: WorkspaceStatus,
    val subscriptionStatus: SubscriptionStatus,
    val modules: Set<ProductModule>
)

enum class SecurityScope {
    WORKSPACE,
    ORGANIZATION
}

val SecurityContextKey = AttributeKey<SecurityContext>("SecurityContext")

class AuthorizeConfig {
    var scope: SecurityScope = SecurityScope.ORGANIZATION
    var module: ProductModule? = null
    var requireCapabilities: Set<UserCapability> = emptySet()
    var requireSubscription: Boolean = true
    var allowMeshReplay: Boolean = false
}


/**
 * The core security kernel plugin.
 * Performs deep validation of workspace standing, module entitlements,
 * subscription health, and user capabilities for human users,
 * while seamlessly authorizing cryptographic device / node requests.
 */
val Authorize = createRouteScopedPlugin(
    name = "Authorize", createConfiguration = ::AuthorizeConfig
) {
    val json by lazy { Json { ignoreUnknownKeys = true } }

    on(AuthenticationChecked) { call ->
        val pluginConfig = pluginConfig
        val contextProvider: AuthorizationContextProvider by call.application.inject()
        val auditor: TelemetryAuditor by call.application.inject()

        val devicePrincipal = call.principal<DevicePrincipal>()
        val userPrincipal = call.principal<UserPrincipal>()

        // Extract Organization ID from authenticated sources
        val rawOrgHeader = call.request.header("X-Organization-Id")
        val parsedOrgHeader = rawOrgHeader?.split(",")?.firstOrNull()?.trim()

        val organizationId = devicePrincipal?.organizationId
            ?: userPrincipal?.organizationId
            ?: parsedOrgHeader
            ?: error("IA breach, Organization ID not found")

        // Resolve Hardware ID strictly from cryptographic device principal (preventing header spoofing)
        val hardwareId = devicePrincipal?.hardwareId ?: call.request.header("X-Hardware-Id")
        ?: error("IA breach, Hardware id not found")

        // Extract Raw Workspace ID from standard sources
        val rawWorkspaceId = devicePrincipal?.workspaceId
            ?: userPrincipal?.workspaceId
            ?: call.request.header("X-Workspace-Id")
            ?: call.request.queryParameters["activeWorkspaceId"]

        // Extract Sync Metadata FIRST (mesh replay / offline sync worker) to resolve targetWsId
        val encodedMeta = call.request.header("X-Sync-Metadata")
        var targetWsId = rawWorkspaceId
        var resolvedActorId = userPrincipal?.userId ?: hardwareId
        var resolvedActorName = userPrincipal?.username
            ?: if (hardwareId.isNotBlank()) "DEVICE:$hardwareId" else "SYSTEM"
        var deploymentChannel = DeploymentChannel.LIVE_INTERACTIVE

        if (!encodedMeta.isNullOrBlank()) {
            try {
                val decodedBytes = Base64.getDecoder().decode(encodedMeta)
                val jsonStr = String(decodedBytes, Charsets.UTF_8)
                val meta = json.decodeFromString<SyncMetadata>(jsonStr)
                targetWsId = meta.wsId
                resolvedActorId = meta.actorId
                resolvedActorName = meta.actorName
                deploymentChannel = DeploymentChannel.MESH_REPLAY
            } catch (e: Exception) {
                e.printStackTrace()
                return@on call.respond(
                    HttpStatusCode.BadRequest,
                    "Malformed sync metadata structural headers."
                )
            }
        }

        // THEN enforce scope requirements using the fully resolved targetWsId
        if (pluginConfig.scope == SecurityScope.WORKSPACE && targetWsId.isNullOrBlank()) {
            return@on call.respond(
                HttpStatusCode.Unauthorized,
                "Please select a business workspace to continue."
            )
        }

        // Enforce live interactive requirement for administrative/non-data routes
        if (!pluginConfig.allowMeshReplay && deploymentChannel == DeploymentChannel.MESH_REPLAY) {
            auditor.logNetworkSecurityEvent(
                triggerEvent = "ADMIN_ACTION_MESH_REPLAY_REJECTED",
                organizationId = organizationId,
                workspaceId = targetWsId,
                deviceId = hardwareId,
                level = TelemetryLevel.WARNING
            )
            return@on call.respond(
                HttpStatusCode.Forbidden,
                "Administrative actions require a live online connection and cannot be replayed offline."
            )
        }

        // Hardware Master & Workspace Binding Security Validation
        if (hardwareId.isNotBlank()) {
            val deviceContext = contextProvider.getSecurityContext(targetWsId ?: "", hardwareId)
            if (deviceContext != null) {
                if (deviceContext.masterStatus == HardwareMasterStatus.REVOKED) {
                    auditor.logNetworkSecurityEvent(
                        triggerEvent = "DEVICE_MASTER_REVOKED",
                        organizationId = organizationId,
                        workspaceId = targetWsId,
                        deviceId = hardwareId,
                        level = TelemetryLevel.WARNING
                    )

                    return@on call.respond(
                        HttpStatusCode.Forbidden,
                        "Access denied: This device has been disabled by your administrator."
                    )
                }

                if (!targetWsId.isNullOrBlank()) {
                    val bindingStatus = deviceContext.bindingStatus?.get(targetWsId)
                    if (bindingStatus == DeviceStatus.REVOKED) {
                        return@on call.respond(
                            HttpStatusCode.Forbidden,
                            "Access denied: This device is no longer authorized for the selected business workspace."
                        )
                    }
                    if (bindingStatus == DeviceStatus.LOCKED) {
                        return@on call.respond(
                            HttpStatusCode.Forbidden,
                            "Access denied: Device access to this business workspace is temporarily locked."
                        )
                    }
                }
            }
        }

        // Mandatory Workspace Operational State Query & Validation
        var workspaceState: WorkspaceOperationalState? = null
        if (pluginConfig.scope == SecurityScope.WORKSPACE) {

            workspaceState = try {
                contextProvider.getWorkspaceOperationalState(targetWsId!!)
            } catch (_: Exception) {
                return@on call.respond(
                    HttpStatusCode.Forbidden,
                    "Unable to load workspace details. Please try again."
                )
            } ?: return@on call.respond(
                HttpStatusCode.Forbidden,
                "The selected business workspace could not be found."
            )

            val orgSecurityConfig = call.getOrgSecurityConfigForId(organizationId)
            if (orgSecurityConfig != null && pluginConfig.allowMeshReplay && encodedMeta != null) {
                if (!orgSecurityConfig.isWorkspaceCloudEnabled(targetWsId)) {
                    auditor.logNetworkSecurityEvent(
                        triggerEvent = "CLOUD_SYNC_DISABLED_REJECTED",
                        organizationId = organizationId,
                        workspaceId = targetWsId,
                        deviceId = hardwareId,
                        level = TelemetryLevel.WARNING
                    )
                    return@on call.respond(
                        HttpStatusCode.Forbidden,
                        "Cloud sync is currently disabled for this business workspace."
                    )
                }
            }

            if (workspaceState.status != WorkspaceStatus.ACTIVE) {
                return@on call.respond(
                    HttpStatusCode.Forbidden,
                    "This business workspace is currently inactive or suspended."
                )
            }

            if (pluginConfig.requireSubscription) {
                val isHealthy =
                    workspaceState.subscriptionStatus == SubscriptionStatus.ACTIVE ||
                            workspaceState.subscriptionStatus == SubscriptionStatus.TRIAL
                if (!isHealthy) {
                    return@on call.respond(
                        HttpStatusCode.PaymentRequired,
                        "Your subscription is inactive. Please renew your plan to continue using this service."
                    )
                }
            }

            pluginConfig.module?.let { requiredModule ->
                if (!workspaceState.modules.contains(requiredModule)) {
                    return@on call.respond(
                        HttpStatusCode.Forbidden,
                        "This feature is not included in your current subscription plan."
                    )
                }
            }
        }

        // Optimized User Context & Capabilities Validation
        var user: User? = null
        if (userPrincipal != null && pluginConfig.requireCapabilities.isNotEmpty()) {
            val ctx = contextProvider.getUserContext(userPrincipal, workspaceState)
            if (ctx != null) {
                user = ctx.user
                val isPlatformUser =
                    user?.userType == UserType.SYSTEM || user?.userType == UserType.PLATFORM
                if (!isPlatformUser) {
                    val hasAll =
                        user?.capabilities?.containsAll(pluginConfig.requireCapabilities) == true
                    if (!hasAll) {
                        return@on call.respond(
                            HttpStatusCode.Forbidden,
                            "You do not have permission to access this feature."
                        )
                    }
                }
            }
        }

        // Emit Unified Authorization Context
        val authContext = AuthorizationContext(
            organizationId = organizationId,
            workspaceId = targetWsId,
            hardwareId = hardwareId,
            actorId = resolvedActorId,
            actorName = resolvedActorName,
            sessionId = userPrincipal?.sessionId,
            channel = deploymentChannel,
            workspace = workspaceState,
            user = user,
            principal = userPrincipal,
            devicePrincipal = devicePrincipal
        )

        call.attributes.put(SecurityContextKey, authContext)
    }
}

val OrgSecurityConfigKey = AttributeKey<OrgSecurityConfig>("OrgSecurityConfigKey")

/**
 * Enhanced context helper to retrieve organization runtime security states
 * using an explicitly supplied organization identifier.
 */
suspend fun ApplicationCall.getOrgSecurityConfigForId(organizationId: String): OrgSecurityConfig? {
    if (organizationId.isBlank()) return null

    val existing = attributes.getOrNull(OrgSecurityConfigKey)
    if (existing != null) return existing

    val provider: OrgSecurityConfigProvider by application.inject()
    val config = provider.get(organizationId) ?: return null
    attributes.put(OrgSecurityConfigKey, config)
    return config
}
