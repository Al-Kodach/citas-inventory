package com.citacomsys.kernel.client

import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.domain.service.NetworkConnectivityService
import com.citacomsys.core.remote.ApiNetworkException
import com.citacomsys.device.service.DeviceService
import com.citacomsys.kernel.model.*
import com.citacomsys.kernel.service.KernelApiService
import com.citacomsys.kernel.service.PlatformRuntimeStore
import com.citacomsys.shared.event.AppLifecycleEvent
import com.citacomsys.shared.event.EventBus
import com.citacomsys.types.DeviceStatus
import com.citacomsys.types.UserType
import com.citacomsys.types.WorkspaceClaimRequest
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

/**
 * Single source of truth for workspace configuration, workstation claims/check-ins, and step progress state.
 * Uses cached workstation profiles instantly when available for zero-delay activation, and updates session parameters ONLY upon success.
 */
class WorkspaceConfigurationManager(
    private val kernelApiService: KernelApiService,
    private val store: PlatformRuntimeStore,
    private val sessionManager: AppSessionManager,
    private val deviceService: DeviceService,
    private val connectivityService: NetworkConnectivityService,
    private val scope: CoroutineScope,
) {
    private val TAG = "WORKSPACE_CONFIG_MANAGER"
    private val configMutex = Mutex()

    private val _wsState = MutableStateFlow<WsConfigurationState>(WsConfigurationState.Loading)
    val wsConfigurationState: StateFlow<WsConfigurationState> = _wsState.asStateFlow()

    private fun reset() {
        _wsState.value = WsConfigurationState.Loading
    }

    fun configureWs(
        event: AppLifecycleEvent.WorkspaceSelected,
        onSuccess: suspend () -> Unit
    ) {
        scope.launch {
            configMutex.withLock {
                _wsState.value = WsConfigurationState.Configuring(
                    totalSteps = 3,
                    currentStep = 1,
                    currentStepMessage = "Verifying business profile & security attestation...",
                )

                val targetWsId = event.workspaceId

                // User & Workspace Authorization Validation
                val authError = sessionManager.isAuthorized(targetWsId, event.branchId)
                if (authError != null) {
                    Napier.w(tag = TAG) { "[SECURITY_REJECT] User authorization failed for workspace $targetWsId: $authError" }
                    _wsState.value = WsConfigurationState.Error("Access Denied: $authError")
                    return@launch
                }

                val cachedProfile = store.workstationRegistry.value[targetWsId]

                val hardwareId = deviceService.deviceInfo.value.hardwareId

                // Device Binding & Attestation Status Validation

                val currentDeviceStatus = cachedProfile?.deviceStatus ?: DeviceStatus.ACTIVE

                if (currentDeviceStatus.revoked() || currentDeviceStatus.locked()) {
                    Napier.w(tag = TAG) { "[SECURITY_REJECT] Terminal $hardwareId device status for workspace $targetWsId is $currentDeviceStatus" }
                    _wsState.value =
                        WsConfigurationState.Error("Access Denied: This terminal's access to this store has been ${currentDeviceStatus.name} by an administrator.")
                    return@launch
                }


                if (cachedProfile != null) {
                    _wsState.value = WsConfigurationState.Configuring(
                        totalSteps = 3,
                        currentStep = 2,
                        currentStepMessage = "Restoring workstation environment & security context...",
                    )

                    configureWorkspace(event)

                    delay(300.milliseconds)

                    _wsState.value = WsConfigurationState.Configuring(
                        totalSteps = 3,
                        currentStep = 3,
                        currentStepMessage = "Finalizing workspace runtime states...",
                    )

                    delay(500.milliseconds)
//                    _wsState.value = WsConfigurationState.Completed

                    onSuccess()
                    reset()
                    return@launch
                }

                Napier.i(tag = TAG) { "Workstation profile not in cache for $targetWsId. Claiming workspace channel..." }
                _wsState.value = WsConfigurationState.Configuring(
                    totalSteps = 3,
                    currentStep = 1,
                    currentStepMessage = "Claiming business channel...",
                )

                val claimResult = claimWorkspaceInternal(
                    workspaceId = targetWsId,
                    deviceName = sessionManager.workStationInfo.value.deviceName,
                )

                if (claimResult.isFailure) {
                    val err = claimResult.exceptionOrNull()
                    Napier.e(
                        tag = TAG,
                        throwable = err
                    ) { "Failed to claim workspace for $targetWsId" }

                    val errorMessage = when (err) {
                        is ApiNetworkException -> "Failed to connect to Citas server. Please check your internet connection."
                        else -> err?.message ?: "Failed to launch business workstation"
                    }
                    _wsState.value = WsConfigurationState.Error(errorMessage)
                    return@launch
                }

                // SYNC PATH: Fetch workspace config and state
                _wsState.value = WsConfigurationState.Configuring(
                    totalSteps = 3,
                    currentStep = 2,
                    currentStepMessage = "Connecting & syncing business environment...",
                )

                runCatching {
                    coroutineScope {
                        val state =  kernelApiService.getRuntimeState(targetWsId)
                        store.updateWorkspaceState(state)
                    }
                }.onSuccess {
                    _wsState.value = WsConfigurationState.Configuring(
                        totalSteps = 3,
                        currentStep = 3,
                        currentStepMessage = "Finalizing business runtime states...",
                    )

                    configureWorkspace(event)
                    delay(300.milliseconds)

                    onSuccess()
                    reset()
                }.onFailure { e ->
                    Napier.e(
                        tag = TAG,
                        throwable = e
                    ) { "Failed to configureWorkspace workspace environment for $targetWsId" }
                    val errorMessage = when (e) {
                        is ApiNetworkException -> "Failed to connect to Citas server. Please check your internet connection."
                        else -> e.message ?: "Failed to fetch business runtime state"
                    }
                    _wsState.value = WsConfigurationState.Error(errorMessage)
                }
            }
        }
    }

    private suspend fun configureWorkspace(
        event: AppLifecycleEvent.WorkspaceSelected,
    ) {
        sessionManager.setWorkspaceLocked(event.isWsLocked, event.workspaceId)

        sessionManager.configureWorkspace(
            workspaceId = event.workspaceId,
            defaultBranchId = event.branchId,
            isBranchLocked = event.isBranchLocked,
            type = event.productType
        )

        sessionManager.recordAccessedBranch(event.workspaceId, event.branchId)

        if (event.isWsLocked) {
            applyWorkspaceIsolation(event.workspaceId)
        }
    }

    private suspend fun applyWorkspaceIsolation(workspaceId: String) {
        val currentOrg = store.getOrgConfig()
        currentOrg?.workspaces?.forEach { ws ->
            if (ws.id != workspaceId) {
                store.removeWorkstationProfile(ws.id)
            }
        }
    }

    private suspend fun claimWorkspaceInternal(
        workspaceId: String,
        deviceName: String
    ): Result<Unit> = runCatching {
        val techInfo = deviceService.getHardwareInfo()
        val response = kernelApiService.claimWorkspace(
            WorkspaceClaimRequest(
                workspaceId = workspaceId,
                deviceName = deviceName,
                techInfo = techInfo,
            )
        )
        injectWorkspace(response)
    }

    private suspend fun injectWorkspace(response: BootstrapWorkspaceResponse) {
        val config = response.config
        val state = response.state

        val orgConfig = response.organizationConfig
        if (orgConfig != null) {
            store.saveOrganizationConfig(orgConfig)
        }

        val profile = LocalWorkstationProfile(
            workspaceId = config.workspaceId,
            organizationId = sessionManager.organizationId.orEmpty(),
            branchId = config.workspaceId,
            branchName = "Main",
            appType = config.type,
            deviceStatus = DeviceStatus.ACTIVE,
            config = config,
            state = state,
            lastActivatedAt = Clock.System.now(),
        )

        store.saveWorkstationProfile(profile)
        store.updateWorkspaceConfig(config)
        store.updateWorkspaceState(state)
    }
}
