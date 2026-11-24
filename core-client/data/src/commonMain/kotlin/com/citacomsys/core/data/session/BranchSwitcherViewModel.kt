package com.citacomsys.core.data.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citacomsys.core.domain.security.BranchUiModel
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.sync.engine.EntitySyncEngine
import com.citacomsys.kernel.model.BranchSummary
import com.citacomsys.kernel.service.PlatformRuntimeStore
import com.citacomsys.types.EntityType
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class BranchSwitcherViewModel(
    private val sessionManager: AppSessionManager,
    store: PlatformRuntimeStore,
    private val syncEngine: EntitySyncEngine
) : ViewModel() {

    val isGlobalMode: StateFlow<Boolean> = sessionManager.activeViewBranchId.map { it == null }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            true
        )

    val currentWriteTargetBranchId: StateFlow<String?> = sessionManager.writeTargetBranchId

    /**
     * Shared reactive stream of raw branches for the active workspace.
     */
    private val rawWorkspaceBranches: Flow<List<BranchSummary>> = combine(
        sessionManager.activeWorkspaceIdFlow,
        store.organizationConfig
    ) { wsId, orgConfig ->
        if (wsId.isNullOrBlank()) emptyList()
        else orgConfig?.workspaces?.firstOrNull { it.id == wsId }?.branches ?: emptyList()
    }

    /**
     * View Branch list (for analytics & UI workspace filtering). Includes CEO Mode ("All Branches").
     */
    val viewBranches: StateFlow<List<BranchUiModel>> = combine(
        sessionManager.activeViewBranchId,
        rawWorkspaceBranches
    ) { activeViewId, branches ->
        val ceoModeTab = BranchUiModel(
            id = "CEO_MODE",
            name = "All Branches (CEO Mode)",
            isSelected = activeViewId == null
        )
        val branchModels = branches.map { BranchUiModel(it.id, it.name, it.id == activeViewId) }
        listOf(ceoModeTab) + branchModels
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    /**
     * Write Target Branch list (for POS sales & stock mutations).
     * Excludes CEO Mode as writes require a physical branch target.
     */
    val writeBranches: StateFlow<List<BranchUiModel>> = combine(
        sessionManager.writeTargetBranchId,
        rawWorkspaceBranches
    ) { activeWriteId, branches ->
        branches.map { BranchUiModel(it.id, it.name, it.id == activeWriteId) }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val isBranchLocked: StateFlow<Boolean> = sessionManager.isBranchLocked

    // Backwards compatibility alias for single-selector callers
    val branches: StateFlow<List<BranchUiModel>> = viewBranches

    private var syncRequestJob: Job? = null

    fun selectViewBranch(uiId: String) {
        if (isBranchLocked.value && uiId != "CEO_MODE") {
            val currentWriteBranch = currentWriteTargetBranchId.value
            if (!currentWriteBranch.isNullOrBlank() && uiId != currentWriteBranch) {
                Napier.w { "Branch selection is locked on this device. Switch rejected." }
                return
            }
        }
        syncRequestJob?.cancel()

        syncRequestJob = viewModelScope.launch {
            if (uiId == "CEO_MODE") {
                sessionManager.setViewBranchId(null)
            } else {
                sessionManager.setViewBranchId(uiId)
            }

            delay(1000.milliseconds)

            try {
                syncEngine.requestSync(EntityType.STOCK_BATCH)
            } catch (e: Exception) {
                Napier.e(e) { "Automated branch shift synchronization cycle failed" }
            }
        }
    }

    fun selectWriteBranch(branchId: String) {
        viewModelScope.launch {
            sessionManager.setWriteBranchId(branchId)
        }
    }

    fun toggleBranchSelection(uiId: String) {
        selectViewBranch(uiId)
    }

    fun setWriteTargetBranch(id: String) {
        viewModelScope.launch {
            sessionManager.setWriteBranchId(id)
        }
    }
}
