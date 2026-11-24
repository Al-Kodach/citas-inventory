package com.citacomsys.feature.workspace.ui.branch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.citacomsys.api.workspace.branch.Branch
import com.citacomsys.api.workspace.branch.BranchRepository
import com.citacomsys.core.database.InventoryDatabaseManager
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiResult
import com.citacomsys.database.BranchEntity
import com.citacomsys.types.Address
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class BranchManagementViewModel(
    private val workspaceId: String,
    private val branchRepository: BranchRepository,
    private val dbManager: InventoryDatabaseManager,
    sessionManager: AppSessionManager
) : ViewModel() {

    val workStationInfo = sessionManager.workStationInfo

    private val branchesFlow = dbManager.database.flatMapLatest { db ->
        db.branchesQueries.selectByTenant(workspaceId).asFlow()
            .mapToList(Dispatchers.Default)
            .map { it.map { it.toDomain() } }
    }
    private val _branches = MutableStateFlow<UiResult<List<Branch>>>(UiResult.Loading)

    val branches = combine(_branches, branchesFlow) { state, branches ->
        if (state is UiResult.Success && branches.isEmpty()) {
            UiResult.Success(emptyList())
        } else {
            state
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        UiResult.Loading
    )

    private val _actionState = MutableStateFlow<UiResult<Unit>>(UiResult.Idle)
    val actionState = _actionState.asStateFlow()


    init {
        loadBranches()
    }

    fun loadBranches() {
        viewModelScope.launch {
            _branches.value = UiResult.Loading
            val result = runCatching { branchRepository.getAll(workspaceId) }
            if (result.isSuccess) {
                val branches = result.getOrThrow()
                saveLocal(branches)
            }
            _branches.value = result.asUiResult()
        }
    }

    private fun saveLocal(branches: List<Branch>) {
        viewModelScope.launch {
            dbManager.getDatabase().transaction {
                branches.forEach { branch ->
                    dbManager.getDatabase().branchesQueries.upsert(
                        id = branch.id,
                        workspace_id = branch.workspaceId,
                        name = branch.name,
                        location_code = branch.locationCode,
                        address_json = branch.address?.let { Json.encodeToString(it) },
                        contact_phone = branch.contactPhone,
                        email = branch.email,
                        website = branch.website,
                        is_active = branch.isActive,
                        created_at = branch.createdAt,
                        updated_at = branch.updatedAt
                    )
                }
            }
        }
    }

    fun createBranch(branch: Branch) {
        viewModelScope.launch {
            viewModelScope.launch {
                _actionState.value = UiResult.Loading
                val result = runCatching { branchRepository.save(branch) }
                _actionState.value = result.asUiResult()
                if (result.isSuccess) {
                    saveLocal(listOf(branch))
                }
            }
        }
    }

    fun update(updated: Branch) {
        viewModelScope.launch {
            _actionState.value = UiResult.Loading
            val result = runCatching { branchRepository.save(updated) }
            _actionState.value = result.asUiResult()
            if (result.isSuccess) {
                saveLocal(listOf(updated))
            }
        }
    }

    fun deleteBranch(id: String) {
        viewModelScope.launch {
            _actionState.value = UiResult.Loading
            val result = runCatching { branchRepository.delete(workspaceId, id) }
            _actionState.value = result.asUiResult()
            if (result.isSuccess) {
                loadBranches()
            }
        }
    }

    fun BranchEntity.toDomain() = Branch(
        id = this.id,
        workspaceId = this.workspace_id,
        name = this.name,
        locationCode = this.location_code,
        address = this.address_json?.let { Json.decodeFromString<Address>(it) },
        contactPhone = this.contact_phone,
        email = this.email,
        website = this.website,
        isActive = this.is_active,
        createdAt = this.created_at,
        updatedAt = this.updated_at
    )
}
