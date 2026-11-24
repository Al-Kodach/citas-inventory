package com.citacomsys.app.shared.util

import com.citacomsys.core.domain.security.UserCapabilityManager
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.spi.AppManifest
import com.citacomsys.core.spi.AppOperatingMode
import com.citacomsys.types.DefaultCapabilities
import com.citacomsys.types.User
import com.citacomsys.types.UserCapability
import com.citacomsys.types.UserType
import com.citacomsys.types.UserWorkspaceBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

internal class TenantUserCapabilityManager(
    sessionManager: AppSessionManager,
    appManifest: AppManifest
) : UserCapabilityManager {

    override val capabilities: StateFlow<Set<UserCapability>> =
        combine(
            sessionManager.appUser,
            sessionManager.userWorkspaceBindings,
            sessionManager.activeWorkspaceIdFlow,
            appManifest.operatingMode
        ) { user: User?, bindings: List<UserWorkspaceBinding>, activeWsId: String?, mode: AppOperatingMode ->
            if (user == null) return@combine emptySet()

            // System and Platform Super Admins have all capabilities
            if (user.userType == UserType.SYSTEM || user.userType == UserType.PLATFORM) {
                return@combine UserCapability.entries.toSet()
            }

            if (user.role == "OWNER" || user.role == "TENANT_OWNER") {
                return@combine DefaultCapabilities.forRole(user.role)
            }

            // Resolve User Account Capabilities
            val userCaps = user.capabilities.ifEmpty {
                DefaultCapabilities.forRole(user.role)
            }

            when (mode) {
                AppOperatingMode.ORG -> userCaps

                AppOperatingMode.WS -> {
                    // Look for workspace-specific binding override
                    val binding = activeWsId?.let { wsId ->
                        bindings.firstOrNull { it.workspaceId == wsId }
                    }

                    if (binding != null) {
                        if (binding.capabilities.isNotEmpty()) {
                            binding.capabilities
                        } else {
                            // Fallback: Default for binding role or User capabilities
                            val bindingRoleCaps = DefaultCapabilities.forRole(binding.role)
                            if (bindingRoleCaps.isNotEmpty()) bindingRoleCaps else userCaps
                        }
                    } else {
                        // No explicit workspace binding (e.g. Org Owner or Org Admin) -> Fallback to User Capabilities
                        userCaps
                    }
                }
            }
        }.stateIn(
            scope = CoroutineScope(Dispatchers.Default),
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    override fun has(capability: UserCapability): Boolean {
        return capabilities.value.contains(capability)
    }

    override fun hasAll(caps: Set<UserCapability>): Boolean {
        if (caps.isEmpty()) return true
        return capabilities.value.containsAll(caps)
    }
}
