package com.citacomsys.core.security

import com.citacomsys.api.auth.UserSessionRepository
import com.citacomsys.api.organization.OrganizationRepository
import com.citacomsys.api.workspace.repository.WorkspaceRepository
import com.citacomsys.api.workspace.repository.WorkspaceResourceUsageRepository
import com.citacomsys.core.domain.result.SystemActors
import com.citacomsys.core.persistence.table.tenant.HardwareRegistry
import com.citacomsys.core.persistence.table.tenant.WorkstationBindings
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.core.security.plugin.UserAuthorizationContext
import com.citacomsys.core.security.plugin.WorkspaceOperationalState
import com.citacomsys.core.security.token.jwt.UserPrincipal
import com.citacomsys.inventory.api.user.PlatformAdminRepository
import com.citacomsys.inventory.api.user.UserRepository
import com.citacomsys.shared.event.DeviceRevokedEvent
import com.citacomsys.shared.event.DeviceSecurityEvent
import com.citacomsys.shared.event.DeviceUpdatedEvent
import com.citacomsys.shared.event.EventBus
import com.citacomsys.shared.event.SubscriptionEvent
import com.citacomsys.shared.event.UserWorkspaceBindingEvent
import com.citacomsys.shared.event.WorkstationBindingEvent
import com.citacomsys.shared.event.WorkstationSecurityEvent
import com.citacomsys.types.*
import com.github.benmanes.caffeine.cache.AsyncLoadingCache
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.util.Base64
import java.util.concurrent.TimeUnit

class AuthorizationContextProvider(
    private val organizationRepository: OrganizationRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val resourceUsageRepository: WorkspaceResourceUsageRepository,
    private val userRepository: UserRepository,
    private val adminUserRepository: PlatformAdminRepository,
    private val userSessionRepository: UserSessionRepository,
    private val cacheScope: CoroutineScope
) {
    private val logger = KotlinLogging.logger("com.citacomsys.AuthorizationContextProvider")

    init {
        cacheScope.launch {
            EventBus.subscribe<SubscriptionEvent>().collect {
                logger.info { "Organization subscription state changed event received, invalidating validation context cache for organization: ${it.organizationId}" }
                invalidateOrganization(it.organizationId)
                invalidateWorkspace(it.wsId)
            }
        }
        cacheScope.launch {
            EventBus.subscribe<WorkstationSecurityEvent>().collect { event ->
                logger.info { "Workspace security invalidation event received for Org: ${event.organizationId}, Workspace: ${event.workspaceId}. Reason: ${event.reason}" }
                if (event.workspaceId != null) {
                    invalidateWorkspace(event.workspaceId!!)
                } else {
                    invalidateOrganization(event.organizationId)
                }
            }
        }
        cacheScope.launch {
            EventBus.subscribe<UserWorkspaceBindingEvent>().collect { event ->
                logger.info { "UserWorkspaceBindingEvent received for user ${event.userId}, invalidating org security context." }
                if (event.workspaceId.isNullOrBlank().not()) {
                    invalidateWorkspace(event.workspaceId!!)
                }
            }
        }
        cacheScope.launch {
            EventBus.subscribe<WorkstationBindingEvent>().collect { event ->
                logger.info { "WorkstationBindingEvent received for HW ${event.hardwareId}, invalidating device cache." }
                invalidateDevice(event.hardwareId)
                if (!event.workspaceId.isNullOrBlank()) {
                    invalidateWorkspace(event.workspaceId!!)
                }
            }
        }
        cacheScope.launch {
            EventBus.subscribe<DeviceSecurityEvent>().collect { event ->
                logger.info { "DeviceSecurityEvent received for HW ${event.hardwareId}, invalidating device cache." }
                invalidateDevice(event.hardwareId)
                invalidateOrganization(event.organizationId)
            }
        }
        cacheScope.launch {
            EventBus.subscribe<DeviceRevokedEvent>().collect { event ->
                logger.info { "DeviceRevokedEvent received for HW ${event.hardwareId}, invalidating device cache." }
                invalidateDevice(event.hardwareId)
                invalidateOrganization(event.organizationId)
            }
        }
        cacheScope.launch {
            EventBus.subscribe<DeviceUpdatedEvent>().collect { event ->
                logger.info { "DeviceUpdatedEvent received for HW ${event.hardwareId}, invalidating device cache." }
                invalidateDevice(event.hardwareId)
                if (!event.workspaceId.isNullOrBlank()) {
                    invalidateWorkspace(event.workspaceId!!)
                }
            }
        }
    }

    private val wsContextCache: AsyncLoadingCache<String, WorkspaceOperationalState> =
        Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .buildAsync { workspaceId, _ ->
                cacheScope.future { getWorkspaceOperationalState(workspaceId) }
            }

    private val deviceSecurityCache: AsyncLoadingCache<String, WorkstationSecurityContext?> =
        Caffeine.newBuilder()
            .maximumSize(1_000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .buildAsync { hardwareId, _ ->
                cacheScope.future { fetchDeviceSecurityContext(hardwareId) }
            }

    suspend fun getWorkspaceOperationalState(workspaceId: String): WorkspaceOperationalState =
        dbQuery {
            logger.debug { "Fetching security profile for workspace: $workspaceId" }

            if (workspaceId == SystemActors.SYSTEM_ID) {
                return@dbQuery WorkspaceOperationalState(
                    organizationId = SystemActors.SYSTEM_ID,
                    status = WorkspaceStatus.ACTIVE,
                    subscriptionStatus = SubscriptionStatus.ACTIVE,
                    modules = ProductModule.entries.toSet()
                )
            }

            val workspace = workspaceRepository.get(workspaceId).getOrNull()
                ?: error("Workspace $workspaceId not found")

            val pkg = resourceUsageRepository.getWorkspaceResourceAllocation(workspaceId)
                ?: error("Workspace $workspaceId has no allocation")

            val subscription = workspaceRepository.getSubscription(workspaceId)

            WorkspaceOperationalState(
                organizationId = workspace.organizationId,
                status = workspace.status,
                subscriptionStatus = subscription.status,
                modules = pkg.modules
            )
        }

    private suspend fun fetchDeviceSecurityContext(hardwareId: String): WorkstationSecurityContext? =
        dbQuery {
            val hardwareRow = HardwareRegistry.selectAll()
                .where { HardwareRegistry.hardwareId eq hardwareId }
                .singleOrNull() ?: return@dbQuery null

            val bindings = WorkstationBindings.selectAll()
                .where { WorkstationBindings.hardwareId eq hardwareId }
                .toList()
                .associate { it[WorkstationBindings.workspaceId].toString() to it[WorkstationBindings.status] }

            WorkstationSecurityContext(
                hardwareId = hardwareRow[HardwareRegistry.hardwareId],
                fingerprint = hardwareRow[HardwareRegistry.fingerprint],
                masterStatus = hardwareRow[HardwareRegistry.masterStatus],
                bindingStatus = bindings
            )
        }

    suspend fun getUserContext(
        principal: UserPrincipal,
        preResolvedWorkspaceState: WorkspaceOperationalState? = null
    ): UserAuthorizationContext? {
        // Session Revocation Enforcement Check
        if (principal.sessionId.isNotBlank()) {
            val session = userSessionRepository.find(principal.sessionId)
            if (session == null || session.revoked) {
                logger.warn { "[SECURITY_REJECT] Session ${principal.sessionId} for user ${principal.userId} has been revoked." }
                return null
            }
        }

        // Hardware Master Status Check
        if (principal.deviceId.isNotBlank()) {
            val hardwareRow = dbQuery {
                HardwareRegistry.selectAll()
                    .where { HardwareRegistry.hardwareId eq principal.deviceId }
                    .singleOrNull()
            }
            if (hardwareRow != null && hardwareRow[HardwareRegistry.masterStatus] == HardwareMasterStatus.REVOKED) {
                logger.warn { "[SECURITY_REJECT] Hardware ${principal.deviceId} is globally REVOKED in HardwareRegistry." }
                return null
            }
        }

        val user = userRepository.findById(principal.userId).getOrNull() ?: run {
            adminUserRepository.findById(principal.userId).getOrNull()?.toUser()
        } ?: run {
            logger.warn { "User not found for principal: $principal" }
            return null
        }

        if (principal.workspaceId == SystemActors.SYSTEM_ID) {
            return UserAuthorizationContext(
                user = user,
                principal = principal,
                workspace = null,
                hardwareId = principal.deviceId
            )
        }

        val workspaceId = principal.workspaceId ?: return getOrgContext(principal)

        val securityProfile = preResolvedWorkspaceState ?: try {
            wsContextCache.get(workspaceId).await()
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving security profile for workspace: $workspaceId" }
            null
        }

        if (securityProfile == null) return null

        if (securityProfile.organizationId != principal.organizationId) {
            logger.warn { "Organization mismatch for workspace $workspaceId. WorkspaceOrg=${securityProfile.organizationId}, PrincipalOrg=${principal.organizationId}" }
            return null
        }

        return UserAuthorizationContext(
            user = user,
            principal = principal,
            workspace = securityProfile,
            hardwareId = principal.deviceId
        )
    }

    suspend fun getOrgContext(principal: UserPrincipal): UserAuthorizationContext? {
        val user = userRepository.findById(principal.userId).getOrNull() ?: run {
            adminUserRepository.findById(principal.userId).getOrNull()?.toUser()
        } ?: run {
            logger.warn { "User not found for principal: $principal" }
            return null
        }

        val org = organizationRepository.get(principal.organizationId).getOrNull() ?: run {
            logger.warn { "Organization not found for principal: $principal" }
            return null
        }

        if (user.organizationId != principal.organizationId) {
            logger.warn { "Organization mismatch for user ${user.id}. UserOrg=${user.organizationId}, PrincipalOrg=${principal.organizationId}" }
            return null
        }

        logger.info { "Hub Access Authorized: User ${user.username} on hardware ${principal.deviceId} for Org ${org.name}" }

        return UserAuthorizationContext(
            user = user,
            principal = principal,
            hardwareId = principal.deviceId,
            workspace = null
        )
    }

    @Suppress("UNUSED_PARAMETER")
    suspend fun getSecurityContext(
        workspaceId: String,
        hardwareId: String
    ): WorkstationSecurityContext? {
        return try {
            deviceSecurityCache.get(hardwareId).await()
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving security context for hardware $hardwareId" }
            null
        }
    }

    @Suppress("UNUSED_PARAMETER")
    suspend fun getHubSecurityContext(
        organizationId: String,
        hardwareId: String
    ): WorkstationSecurityContext? {
        return try {
            deviceSecurityCache.get(hardwareId).await()
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving hub security context for hardware $hardwareId" }
            null
        }
    }

    suspend fun verifyFleetToken(organizationId: String, token: String): Boolean {
        val org = organizationRepository.get(organizationId).getOrNull() ?: return false
        val fleetDna = org.fleetDna

        return try {
            val decrypted = xorDecrypt(token, fleetDna.encodeToByteArray())
            decrypted == organizationId
        } catch (e: Exception) {
            false
        }
    }

    private fun xorDecrypt(base64Data: String, secret: ByteArray): String {
        val data = Base64.getDecoder().decode(base64Data)
        val result = ByteArray(data.size)
        for (i in data.indices) {
            result[i] = (data[i].toInt() xor secret[i % secret.size].toInt()).toByte()
        }
        return String(result)
    }

    fun invalidate(id: String) {
        wsContextCache.synchronous().invalidate(id)
    }

    fun invalidateWorkspace(workspaceId: String) {
        wsContextCache.synchronous().invalidate(workspaceId)
    }

    fun invalidateOrganization(organizationId: String) {
        wsContextCache.synchronous().asMap().forEach { (wsId, profile) ->
            if (profile?.organizationId == organizationId) {
                wsContextCache.synchronous().invalidate(wsId)
            }
        }
    }

    fun invalidateDevice(hardwareId: String) {
        deviceSecurityCache.synchronous().invalidate(hardwareId)
    }
}
