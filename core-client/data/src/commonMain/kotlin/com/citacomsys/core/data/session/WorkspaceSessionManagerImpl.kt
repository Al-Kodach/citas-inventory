package com.citacomsys.core.data.session

import com.citacomsys.core.domain.service.WorkspaceSessionConfig
import com.citacomsys.core.domain.service.WorkspaceSessionManager
import com.citacomsys.kernel.model.LocalWorkstationProfile
import com.citacomsys.kernel.service.PlatformRuntimeStore
import com.citacomsys.types.DeploymentMode
import com.citacomsys.types.ProductType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

class WorkspaceSessionManagerImpl(
    private val store: PlatformRuntimeStore,
) : WorkspaceSessionManager {
    private val defaultConfig = MutableStateFlow(
        WorkspaceSessionConfig(
            workspaceId = "",
            writeBranchId = "",
            deploymentMode = DeploymentMode.OFFLINE_LOCAL,
            viewBranchId = null,
            productType = ProductType.RETAIL,
            isBranchLocked = false
        )
    )

    private val _dpMode = MutableStateFlow(DeploymentMode.OFFLINE_LOCAL)
    override val deploymentMode: DeploymentMode get() = _dpMode.value
    private val _activeProductType = MutableStateFlow(ProductType.RETAIL)
    override val activeProductType: ProductType get() = _activeProductType.value

    private val workspaceId = MutableStateFlow<String?>(null)
    override val activeWorkspaceId: String? get() = workspaceId.value
    override val activeWorkspaceIdFlow: StateFlow<String?> = workspaceId.asStateFlow()

    private val writeBranchId = MutableStateFlow<String?>(null)
    override val writeTargetBranchId: StateFlow<String?> = writeBranchId.asStateFlow()

    private val viewBranchId = MutableStateFlow<String?>(null)
    override val activeViewBranchId: StateFlow<String?> = viewBranchId.asStateFlow()

    private val _isBranchLocked = MutableStateFlow(false)
    override val isBranchLocked = _isBranchLocked.asStateFlow()


    override suspend fun configureWorkspace(
        workspaceId: String,
        defaultBranchId: String,
        type: ProductType,
        isBranchLocked: Boolean,
    ) {
        this.workspaceId.value = workspaceId
        this.writeBranchId.value = defaultBranchId
        this.viewBranchId.value = defaultBranchId
        this._isBranchLocked.value = isBranchLocked

        val profile = store.getProfile(workspaceId) ?: error("Workspace Profile not found")
        val deploymentMode = profile.config.resource.deploymentMode
        _dpMode.value = deploymentMode
        _activeProductType.value = type

        defaultConfig.value = WorkspaceSessionConfig(
            workspaceId = workspaceId,
            writeBranchId = defaultBranchId,
            deploymentMode = deploymentMode,
            viewBranchId = defaultBranchId,
            productType = type,
            isBranchLocked = isBranchLocked
        )
    }

    override fun setViewBranchId(branchId: String?) {
        if (_isBranchLocked.value && defaultConfig.value.writeBranchId.isNotBlank() && branchId != null && branchId != "CEO_MODE" && branchId != defaultConfig.value.writeBranchId) {
            return
        }
        viewBranchId.value = branchId
    }

    override fun setWriteBranchId(branchId: String) {
        if (_isBranchLocked.value && defaultConfig.value.writeBranchId.isNotBlank() && branchId != defaultConfig.value.writeBranchId) {
            return
        }
        writeBranchId.value = branchId
    }

    override fun setBranchLocked(locked: Boolean) {
        _isBranchLocked.value = locked
    }

    override fun resetWorkspaceSession() {
        workspaceId.value = defaultConfig.value.workspaceId
        writeBranchId.value = defaultConfig.value.writeBranchId
        viewBranchId.value = defaultConfig.value.viewBranchId
        _isBranchLocked.value = defaultConfig.value.isBranchLocked
        _dpMode.value = defaultConfig.value.deploymentMode
        _activeProductType.value = defaultConfig.value.productType
    }

    override fun clearWorkspaceSession() {
        workspaceId.value = null
        writeBranchId.value = null
        viewBranchId.value = null
        _isBranchLocked.value = false
        _dpMode.value = DeploymentMode.OFFLINE_LOCAL
        _activeProductType.value = ProductType.RETAIL
        defaultConfig.value = WorkspaceSessionConfig(
            workspaceId = "",
            writeBranchId = "",
            deploymentMode = DeploymentMode.OFFLINE_LOCAL,
            viewBranchId = null,
            productType = ProductType.RETAIL,
            isBranchLocked = false
        )
    }
}
