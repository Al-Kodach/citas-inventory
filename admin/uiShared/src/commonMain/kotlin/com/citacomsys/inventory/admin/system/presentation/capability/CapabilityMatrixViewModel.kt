package com.citacomsys.inventory.admin.system.presentation.capability

import androidx.lifecycle.viewModelScope
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.types.DefaultCapabilities
import com.citacomsys.types.PlatformAdminRole
import com.citacomsys.types.TenantUserRole
import com.citacomsys.types.UserCapability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RoleTier {
    TENANT,
    PLATFORM
}

class CapabilityMatrixViewModel(
    private val api: com.citacomsys.admin.api.system.SystemApiService,
    notification: InAppNotificationService
) : BaseViewModel(notification) {

    private val _roleTier = MutableStateFlow(RoleTier.TENANT)
    val roleTier = _roleTier.asStateFlow()

    private val _tenantMatrix = MutableStateFlow<Map<TenantUserRole, Set<UserCapability>>>(emptyMap())
    val tenantMatrix = _tenantMatrix.asStateFlow()

    private val _platformMatrix = MutableStateFlow<Map<PlatformAdminRole, Set<UserCapability>>>(emptyMap())
    val platformMatrix = _platformMatrix.asStateFlow()

    private val _inheritanceState = MutableStateFlow<List<com.citacomsys.inventory.api.user.RoleInheritanceDto>>(emptyList())
    val inheritanceState = _inheritanceState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            loadMatrix()
            loadInheritance()
        }
    }

    fun setRoleTier(tier: RoleTier) {
        _roleTier.value = tier
    }

    private suspend fun loadMatrix() {
        runCatching { api.fetchGlobalRoleCapabilities() }.onSuccess { rawMap ->
            val tenantMap = mutableMapOf<TenantUserRole, Set<UserCapability>>()
            val platformMap = mutableMapOf<PlatformAdminRole, Set<UserCapability>>()

            rawMap.forEach { (key, value) ->
                when {
                    key.startsWith("TENANT_") -> {
                        try {
                            tenantMap[TenantUserRole.valueOf(key.removePrefix("TENANT_"))] = value
                        } catch (_: Exception) {}
                    }
                    key.startsWith("PLATFORM_") -> {
                        try {
                            platformMap[PlatformAdminRole.valueOf(key.removePrefix("PLATFORM_"))] = value
                        } catch (_: Exception) {}
                    }
                }
            }
            
            // Fill defaults if missing
            TenantUserRole.entries.forEach { role ->
                if (!tenantMap.containsKey(role)) tenantMap[role] = DefaultCapabilities.forRole(role)
            }
            PlatformAdminRole.entries.forEach { role ->
                if (!platformMap.containsKey(role)) platformMap[role] = DefaultCapabilities.forRole(role)
            }

            _tenantMatrix.value = tenantMap
            _platformMatrix.value = platformMap
        }.onFailure {
            _tenantMatrix.value = TenantUserRole.entries.associateWith { DefaultCapabilities.forRole(it) }
            _platformMatrix.value = PlatformAdminRole.entries.associateWith { DefaultCapabilities.forRole(it) }
        }
    }

    private suspend fun loadInheritance() {
        runCatching { api.fetchRoleInheritance() }.onSuccess {
            _inheritanceState.value = it
        }
    }

    fun toggleTenantCapability(role: TenantUserRole, capability: UserCapability) {
        _tenantMatrix.update { current ->
            val updatedCaps = current[role]?.let { caps ->
                if (caps.contains(capability)) caps - capability else caps + capability
            } ?: setOf(capability)
            current + (role to updatedCaps)
        }
    }

    fun togglePlatformCapability(role: PlatformAdminRole, capability: UserCapability) {
        _platformMatrix.update { current ->
            val updatedCaps = current[role]?.let { caps ->
                if (caps.contains(capability)) caps - capability else caps + capability
            } ?: setOf(capability)
            current + (role to updatedCaps)
        }
    }

    fun saveChanges() {
        viewModelScope.launch {
            val jobs = mutableListOf<kotlinx.coroutines.Job>()
            
            // Save Tenant Roles
            _tenantMatrix.value.forEach { (role, caps) ->
                jobs.add(launch { api.saveGlobalRoleCapabilities("TENANT_${role.name}", caps) })
            }
            
            // Save Platform Roles
            _platformMatrix.value.forEach { (role, caps) ->
                jobs.add(launch { api.saveGlobalRoleCapabilities("PLATFORM_${role.name}", caps) })
            }
            
            jobs.forEach { it.join() }
            showSuccess("All role capabilities updated successfully")
        }
    }

    fun addInheritanceRule(role: String, parentRole: String) {
        viewModelScope.launch {
            api.saveRoleInheritance(role, parentRole).onSuccess {
                loadInheritance()
                showSuccess("Inheritance rule added")
            }.onFailure {
                showError("Failed to add rule: ${it.message}")
            }
        }
    }

    fun deleteInheritanceRule(role: String, parentRole: String) {
        viewModelScope.launch {
            api.deleteRoleInheritance(role, parentRole).onSuccess {
                loadInheritance()
                showSuccess("Inheritance rule removed")
            }.onFailure {
                showError("Failed to delete rule: ${it.message}")
            }
        }
    }
}
