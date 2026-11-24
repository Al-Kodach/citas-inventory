package com.citacomsys.kernel.server

import com.citacomsys.api.organization.OrganizationRepository
import com.citacomsys.api.workspace.repository.WorkspaceRepository
import com.citacomsys.api.workspace.service.ResourceCheckResult
import com.citacomsys.api.workspace.service.WorkspaceResourceManager
import com.citacomsys.core.domain.result.toUuid
import com.citacomsys.core.persistence.table.tenant.HardwareRegistry
import com.citacomsys.core.persistence.table.tenant.WorkstationBindings
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.core.notification.service.NotificationService
import com.citacomsys.core.security.hashing.HashUtils
import com.citacomsys.inventory.api.user.UserRepository
import com.citacomsys.kernel.model.*
import com.citacomsys.types.*
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert
import com.citacomsys.shared.event.EventBus
import com.citacomsys.shared.event.OrgRuntimeConfig
import com.citacomsys.shared.event.SubscriptionEvent
import com.citacomsys.shared.event.WorkspaceEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

internal class PlatformRuntimeService(
    private val workspaceRepository: WorkspaceRepository,
    private val organizationRepository: OrganizationRepository,
    private val resManager: WorkspaceResourceManager,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService
) {
    private val workspaceETags = ConcurrentHashMap<String, String>()
    private val orgETags = ConcurrentHashMap<String, String>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        scope.launch {
            EventBus.subscribe<WorkspaceEvent>().collect { event ->
                event.workspaceId?.let { invalidateWorkspaceETag(it) }
                if (event.organizationId.isNotBlank()) invalidateOrgETag(event.organizationId)
            }
        }
        scope.launch {
            EventBus.subscribe<OrgRuntimeConfig>().collect { event ->
                if (event.organizationId.isNotBlank()) invalidateOrgETag(event.organizationId)
            }
        }
        scope.launch {
            EventBus.subscribe<SubscriptionEvent>().collect { event ->
                invalidateWorkspaceETag(event.wsId)
                if (event.organizationId.isNotBlank()) invalidateOrgETag(event.organizationId)
            }
        }
    }

    fun getWorkspaceETag(workspaceId: String): String {
        return workspaceETags.getOrPut(workspaceId) {
            val now = Clock.System.now().toEpochMilliseconds()
            HashUtils.sha256("ws:$workspaceId:$now")
        }
    }

    fun invalidateWorkspaceETag(workspaceId: String) {
        val newTag = HashUtils.sha256("ws:$workspaceId:${Clock.System.now().toEpochMilliseconds()}")
        workspaceETags[workspaceId] = newTag
    }

    fun getOrgETag(organizationId: String): String {
        return orgETags.getOrPut(organizationId) {
            val now = Clock.System.now().toEpochMilliseconds()
            HashUtils.sha256("org:$organizationId:$now")
        }
    }

    fun invalidateOrgETag(organizationId: String) {
        val newTag =
            HashUtils.sha256("org:$organizationId:${Clock.System.now().toEpochMilliseconds()}")
        orgETags[organizationId] = newTag
    }

    suspend fun getOrganizationConfig(organizationId: String): OrganizationRuntimeConfig {
        return organizationRepository.getRuntimeConfig(organizationId)
    }

    suspend fun getConfig(workspaceId: String): WorkspaceRuntimeConfig = coroutineScope {
        val workspace = workspaceRepository.get(workspaceId).getOrThrow()

        val subscriptionDeferred = async { workspaceRepository.getSubscription(workspace.id) }
        val resource = async { resManager.get(workspace.id) }
        val usageDeferred = async { resManager.getUsage(workspace.id) }
//        val accountDeferred = async { workspaceRepository.getAccount(workspaceId) }
        val brand = async {
            workspaceRepository.findBrand(workspaceId)
        }

        val subscription = subscriptionDeferred.await()
//        val account = accountDeferred.await()

        WorkspaceRuntimeConfig(
            workspaceId = workspace.id,
            name = workspace.name,
            type = workspace.businessType,
            status = workspace.status,
            resource = resource.await(),
            usage = usageDeferred.await(),
            subscriptionStatus = subscription.status,
            branding = brand.await()
        )
    }

    suspend fun getModulePolicyState(workspaceId: String): ServerProductPolicy {
        return ServerProductPolicy(
            workspaceId = workspaceId,
            version = 1,
            overallStatus = ModuleStatus.ACTIVE,
            modules = resManager.get(workspaceId)
                .modules.map { module ->
                    EffectiveModulePolicy(module)
                },
            evaluatedAt = Clock.System.now(),
        )
    }

    suspend fun getRuntimeState(
        workspaceId: String,
        deviceId: String
    ): WorkspaceRuntimeState {
        val workspace = workspaceRepository.get(workspaceId).getOrNull()
            ?: return createTerminatedState(workspaceId)

        val account = workspaceRepository.getAccount(workspaceId)
        val subscription = workspaceRepository.getSubscription(workspaceId)
        val now = Clock.System.now()

        val deviceStatus = determineDeviceStatus(deviceId, workspaceId)

        val workspaceStatus = when {
            workspace.status == WorkspaceStatus.SUSPENDED ||
                    subscription.status == SubscriptionStatus.SUSPEND -> WorkspaceStatus.SUSPENDED

            workspace.status == WorkspaceStatus.CANCELED ||
                    subscription.status == SubscriptionStatus.CANCELED -> WorkspaceStatus.TERMINATED

            workspace.status == WorkspaceStatus.EXPIRED ||
                    subscription.status == SubscriptionStatus.EXPIRED -> WorkspaceStatus.READ_ONLY

            subscription.status == SubscriptionStatus.TRIAL ||
                    subscription.status == SubscriptionStatus.ACTIVE -> WorkspaceStatus.ACTIVE

            else -> WorkspaceStatus.ACTIVE
        }

        val absoluteLimit = subscription.endedAt

        val calculatedLease = when (account.deploymentMode) {
            DeploymentMode.CLOUD_SYNCED -> now.plus(7.days)
            DeploymentMode.OFFLINE_LOCAL -> now.plus(30.days)
        }

        val leaseExpiresAt = if (calculatedLease > absoluteLimit) absoluteLimit else calculatedLease

        val requireOnlineValidation = subscription.status == SubscriptionStatus.GRACE_PERIOD ||
                subscription.status == SubscriptionStatus.EXPIRED ||
                now > leaseExpiresAt.minus(3.days)

        val enforcement = WorkspaceEnforcementPolicy(
            allowLogin = WorkspaceStatus.canAccess(workspaceStatus) && deviceStatus != DeviceStatus.LOCKED,
            allowOfflineAuth = WorkspaceStatus.canAccess(workspaceStatus) && deviceStatus == DeviceStatus.ACTIVE,
            allowSync = account.deploymentMode == DeploymentMode.CLOUD_SYNCED &&
                    WorkspaceStatus.canSync(workspaceStatus),
            allowWrites = WorkspaceStatus.canWrite(workspaceStatus) && deviceStatus == DeviceStatus.ACTIVE,
            requireOnlineValidation = requireOnlineValidation,
            forceLogout = workspaceStatus == WorkspaceStatus.SUSPENDED || deviceStatus == DeviceStatus.REVOKED || deviceStatus == DeviceStatus.LOCKED,
            clearOfflineProfiles = workspaceStatus == WorkspaceStatus.TERMINATED || deviceStatus == DeviceStatus.REVOKED,
            requireRecovery = deviceStatus == DeviceStatus.ACTIVE || deviceStatus == DeviceStatus.REVOKED,
            forceCloudOnly = deviceStatus == DeviceStatus.ACTIVE || workspaceStatus == WorkspaceStatus.SUSPENDED,
            offlineRevokedUserIds = userRepository.findSuspendedUsers(workspaceId).map { it.id }
                .toSet(),
            leaseExpiresAt = leaseExpiresAt
        )

        val runtimeFlags = RuntimeFlags(
            hasPendingInvoice = false,
            showSubscriptionWarning = subscription.status == SubscriptionStatus.GRACE_PERIOD || now > leaseExpiresAt.minus(
                2.days
            ),
            showPaymentBanner = subscription.endedAt.let { (it - now).inWholeDays <= 3 },
            maintenanceMode = false,
        )

        return WorkspaceRuntimeState(
            workspaceId = workspaceId,
            workspaceStatus = workspaceStatus,
            deviceStatus = deviceStatus,
            enforcement = enforcement,
            runtimeFlags = runtimeFlags,
            configVersion = 1,
            serverTime = now
        )
    }

    private suspend fun determineDeviceStatus(
        hardwareId: String,
        workspaceId: String
    ): DeviceStatus = dbQuery {
        // Hardware-Level Registry Check (Global Lock)
        val hardware = HardwareRegistry.selectAll()
            .where { HardwareRegistry.hardwareId eq hardwareId }
            .singleOrNull() ?: return@dbQuery DeviceStatus.REVOKED

        if (hardware[HardwareRegistry.masterStatus] != HardwareMasterStatus.ACTIVE) {
            return@dbQuery DeviceStatus.LOCKED
        }

        // Workspace-Specific Binding Check
        val binding = WorkstationBindings.selectAll()
            .where {
                (WorkstationBindings.hardwareId eq hardwareId) and
                        (WorkstationBindings.workspaceId eq workspaceId.toUuid())
            }
            .singleOrNull()

        binding?.get(WorkstationBindings.status) ?: DeviceStatus.REVOKED
    }

    private fun createTerminatedState(tenantId: String) = WorkspaceRuntimeState(
        workspaceId = tenantId,
        workspaceStatus = WorkspaceStatus.TERMINATED,
        deviceStatus = DeviceStatus.ACTIVE,
        enforcement = WorkspaceEnforcementPolicy(
            allowLogin = false,
            allowWrites = false,
            allowSync = false,
            allowOfflineAuth = false,
            requireOnlineValidation = true,
            forceLogout = true,
            clearOfflineProfiles = true,
            requireRecovery = true,
            forceCloudOnly = true,
            offlineRevokedUserIds = emptySet(),
            leaseExpiresAt = Clock.System.now()
        ),
        runtimeFlags = RuntimeFlags(maintenanceMode = false),
        configVersion = 0,
        serverTime = Clock.System.now()
    )

    suspend fun claimWorkspace(
        workspaceId: String,
        deviceId: String,
        organizationId: String,
        deviceName: String,
    ): BootstrapWorkspaceResponse = dbQuery {
        val workspace = workspaceRepository.get(workspaceId).getOrThrow()
        require(workspace.organizationId == organizationId) { "Workspace does not belong to this organization" }

        val existingBinding = WorkstationBindings.selectAll()
            .where {
                (WorkstationBindings.hardwareId eq deviceId) and
                        (WorkstationBindings.workspaceId eq workspaceId.toUuid())
            }
            .singleOrNull()

        if (existingBinding == null) {
            // New Binding - Perform Capacity Check
            val check = resManager.checkAvailability(workspaceId, ResourceType.DEVICE)
            if (check !is ResourceCheckResult.Available) {
                throw IllegalStateException("Device capacity limit reached. Please remove an existing workstation or upgrade.")
            }

            val now = Clock.System.now()

            // Security Gate: Ensure Hardware is ALREADY registered in HardwareRegistry for this Organization
            val registeredHardware = HardwareRegistry.selectAll()
                .where {
                    (HardwareRegistry.hardwareId eq deviceId) and
                            (HardwareRegistry.organizationId eq organizationId.toUuid())
                }
                .singleOrNull()
                ?: throw IllegalStateException("Device $deviceId is not registered in organization hardware registry. Device attestation required.")

            if (registeredHardware[HardwareRegistry.masterStatus] != HardwareMasterStatus.ACTIVE) {
                throw IllegalStateException("Device $deviceId master status is ${registeredHardware[HardwareRegistry.masterStatus]}. Access forbidden.")
            }

            // Create Workstation Binding (User workstation view info)
            WorkstationBindings.upsert {
                it[id] = UUID.randomUUID()
                it[hardwareId] = deviceId
                it[this.workspaceId] = workspaceId.toUuid()
                it[workspaceName] = workspace.name
                it[displayName] = deviceName
                it[orgId] = organizationId.toUuid()
                it[status] = DeviceStatus.ACTIVE
                it[lastUsedAt] = now
                it[createdAt] = now
            }

            resManager.consume(workspaceId, ResourceType.DEVICE)

            // Notify Owner of new workstation entry
            userRepository.findOwner(organizationId)?.let {
                notificationService.sendSecurityAlert(
                    email = it.email,
                    phone = it.phone,
                    organizationId = organizationId,
                    workspaceId = workspaceId,
                    action = "NEW_WORKSTATION_BINDING",
                    summary = "A new workstation '$deviceName' (ID: $deviceId) has been authorized for ${workspace.name}."
                )
            }
        } else if (existingBinding[WorkstationBindings.status] == DeviceStatus.REVOKED) {
            throw IllegalStateException("This workstation has been revoked. Please contact your organization administrator.")
        }

        invalidateWorkspaceETag(workspaceId)
        invalidateOrgETag(organizationId)

        createBootstrapResponse(workspaceId, organizationId, deviceId)
    }

    private suspend fun createBootstrapResponse(
        workspaceId: String,
        organizationId: String,
        deviceId: String
    ): BootstrapWorkspaceResponse = coroutineScope {
        val configDeferred = async { getConfig(workspaceId) }
        val stateDeferred = async { getRuntimeState(workspaceId, deviceId) }
        val orgConfigDeferred = async { getOrganizationConfig(organizationId) }

        BootstrapWorkspaceResponse(
            config = configDeferred.await(),
            state = stateDeferred.await(),
            organizationConfig = orgConfigDeferred.await()
        )
    }
}
